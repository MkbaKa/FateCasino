package github.mkbaka.fatecasino.internal.phase.manager

import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.ServerThreadDispatcher
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.on
import github.mkbaka.fatecasino.internal.util.sendMessage
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class Bounty(
    val issuerId: UUID,
    val issuerName: String,
    val targetId: UUID,
    val targetName: String,
    val amount: Int,
    val createdAt: Long = System.currentTimeMillis(),
    var claimed: Boolean = false,
    var expired: Boolean = false
) {

    val isActive: Boolean get() = !claimed && !expired

    /**
     * 发光效果时长(秒): amount * 10 + 10
     * 1券=20s, 2券=30s, 3券=40s, 4券=50s, 5券=60s
     */
    val glowDurationSeconds: Int get() = amount * 10 + 10

    /**
     * 悬赏有效期(秒): 30 + (amount - 1) * 15
     * 1券=30s, 2券=45s, 3券=60s, 4券=75s, 5券=90s
     */
    val bountyDurationSeconds: Int get() = 30 + (amount - 1) * 15

}

class BountyManager(
    private val context: GameContext,
    private val scope: CoroutineScope
) {

    private val activeBounties: MutableMap<UUID, MutableList<Bounty>> = ConcurrentHashMap()


    fun start(listener: Listener) {
        registerDeathListener(listener)
    }

    fun registerBounty(bounty: Bounty) {
        // 按目标分组存储
        activeBounties.getOrPut(bounty.targetId) { mutableListOf() }.add(bounty)

        // 给目标添加发光效果
        val target = Bukkit.getPlayer(bounty.targetId)
        target?.addPotionEffect(
            PotionEffect(PotionEffectType.GLOWING, bounty.glowDurationSeconds * 20, 0)
        )

        // 播报悬赏信息
        broadcastBountyCreated(bounty, target)

        // 启动倒计时协程
        scope.launchBountyTimer(bounty)
    }

    private fun registerDeathListener(listener: Listener) {
        on<PlayerDeathEvent>(listener = listener) {
            val bountiesOnTarget = activeBounties[player.uniqueId]
            if (bountiesOnTarget.isNullOrEmpty()) return@on

            val killer = player.killer ?: return@on

            val killerSession = context.playerSessions[killer.uniqueId]
            if (killerSession == null || !killerSession.isActive) return@on

            // 处理所有未领取的悬赏 - 不设上限, 击杀者获得全部悬赏金额
            val activeBountiesList = bountiesOnTarget.filter { it.isActive }
            var totalReward = 0

            for (bounty in activeBountiesList) {
                bounty.claimed = true
                totalReward += bounty.amount
            }

            if (totalReward > 0) {
                killerSession.giveTicket(totalReward)

                Component.empty()
                    .append(Component.text("悬赏成功").color(NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("   你击杀了悬赏目标, 获得 ").color(NamedTextColor.GRAY))
                    .append(Component.text(totalReward).color(NamedTextColor.YELLOW))
                    .append(Component.text(" 张券!").color(NamedTextColor.GRAY))
                    .sendMessage(killer)
            }

            // 清理已处理的悬赏
            cleanupBounties(player.uniqueId)
        }
    }

    private fun broadcastBountyCreated(bounty: Bounty, target: Player?) {
        Component.empty()
            .append(Component.text("悬赏发布").color(NamedTextColor.GOLD))
            .append(Component.newline())
            .append(Component.text("   ").color(NamedTextColor.GRAY))
            .append(Component.text(bounty.issuerName).color(NamedTextColor.YELLOW))
            .append(Component.text(" 悬赏 ").color(NamedTextColor.GRAY))
            .append(Component.text(bounty.targetName).color(NamedTextColor.RED))
            .append(Component.text(" - 击杀可获得 ").color(NamedTextColor.GRAY))
            .append(Component.text(bounty.amount).color(NamedTextColor.YELLOW))
            .append(Component.text(" 张券!").color(NamedTextColor.GRAY))
            .broadcast()

        // 位置播报规则: amount >= 2 时在创建时播报目标坐标
        if (bounty.amount >= 2 && target != null) {
            broadcastTargetLocation(bounty.targetName, target.location)
            Sound.ENTITY_WITHER_SPAWN.broadcast(SoundCategory.MASTER, 1.0f, 0.5f)
        }
    }

    private fun broadcastTargetLocation(targetName: String, location: Location) {
        Component.empty()
            .append(Component.text("   目标 ").color(NamedTextColor.GRAY))
            .append(Component.text(targetName).color(NamedTextColor.RED))
            .append(Component.text(" 位于 [").color(NamedTextColor.GRAY))
            .append(Component.text(location.blockX).color(NamedTextColor.YELLOW))
            .append(Component.text(", ").color(NamedTextColor.GRAY))
            .append(Component.text(location.blockY).color(NamedTextColor.YELLOW))
            .append(Component.text(", ").color(NamedTextColor.GRAY))
            .append(Component.text(location.blockZ).color(NamedTextColor.YELLOW))
            .append(Component.text("]").color(NamedTextColor.GRAY))
            .broadcast()
    }

    private fun CoroutineScope.launchBountyTimer(bounty: Bounty) {
        launch {
            val durationSeconds = bounty.bountyDurationSeconds
            val endTime = bounty.createdAt + durationSeconds * 1000L
            val createdAt = bounty.createdAt

            // 位置播报调度
            // amount >= 2: 创建时播报
            // amount >= 3: 30秒时再次播报
            // amount >= 4: 60秒时再次播报
            // amount >= 5: 70秒、80秒时再次播报
            val broadcastAtCreate = bounty.amount >= 2
            val broadcastAt30s = bounty.amount >= 3
            val broadcastAt60s = bounty.amount >= 4
            val broadcastAt70s = bounty.amount >= 5
            val broadcastAt80s = bounty.amount >= 5

            while (isActive && System.currentTimeMillis() < endTime && bounty.isActive) {
                val elapsed = System.currentTimeMillis() - createdAt

                // 30秒位置播报
                if (broadcastAt30s && elapsed >= 30_000L && elapsed < 31_000L) {
                    withContext(NonCancellable + ServerThreadDispatcher) {
                        val target = Bukkit.getPlayer(bounty.targetId)
                        if (target != null && bounty.isActive) {
                            broadcastTargetLocation(bounty.targetName, target.location)
                        }
                    }
                    // 等待一秒避免重复播报
                    delay(1000L)
                    continue
                }

                // 60秒位置播报
                if (broadcastAt60s && elapsed >= 60_000L && elapsed < 61_000L) {
                    withContext(NonCancellable + ServerThreadDispatcher) {
                        val target = Bukkit.getPlayer(bounty.targetId)
                        if (target != null && bounty.isActive) {
                            broadcastTargetLocation(bounty.targetName, target.location)
                        }
                    }
                    // 等待一秒避免重复播报
                    delay(1000L)
                    continue
                }

                // 70秒位置播报
                if (broadcastAt70s && elapsed >= 70_000L && elapsed < 71_000L) {
                    withContext(NonCancellable + ServerThreadDispatcher) {
                        val target = Bukkit.getPlayer(bounty.targetId)
                        if (target != null && bounty.isActive) {
                            broadcastTargetLocation(bounty.targetName, target.location)
                        }
                    }
                    // 等待一秒避免重复播报
                    delay(1000L)
                    continue
                }

                // 80秒位置播报
                if (broadcastAt80s && elapsed >= 80_000L && elapsed < 81_000L) {
                    withContext(NonCancellable + ServerThreadDispatcher) {
                        val target = Bukkit.getPlayer(bounty.targetId)
                        if (target != null && bounty.isActive) {
                            broadcastTargetLocation(bounty.targetName, target.location)
                        }
                    }
                    // 等待一秒避免重复播报
                    delay(1000L)
                    continue
                }

                delay(1000L)
            }

            // 超时处理
            if (!bounty.claimed) {
                bounty.expired = true

                withContext(NonCancellable + ServerThreadDispatcher) {
                    Component.text("${bounty.targetName} 逃脱了 ${bounty.issuerName} 的悬赏!")
                        .color(NamedTextColor.GRAY)
                        .broadcast()

                    cleanupBounties(bounty.targetId)
                }
            }
        }
    }

    private fun cleanupBounties(targetId: UUID) {
        val bounties = activeBounties[targetId] ?: return

        // 移除已完成的悬赏
        bounties.removeAll { !it.isActive }

        // 如果全部完成, 移除整个条目
        if (bounties.isEmpty()) {
            activeBounties.remove(targetId)
        }
    }

    fun getActiveBountiesForTarget(targetId: UUID): List<Bounty> {
        return activeBounties[targetId]?.filter { it.isActive } ?: emptyList()
    }

    fun getTotalBountyOnTarget(targetId: UUID): Int {
        return activeBounties[targetId]?.filter { it.isActive }?.sumOf { it.amount } ?: 0
    }

    fun clearAll() {
        activeBounties.clear()
    }

}