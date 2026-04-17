package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.ServerThreadDispatcher
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import github.mkbaka.fatecasino.internal.util.countdownBossBar
import github.mkbaka.fatecasino.internal.util.on
import github.mkbaka.fatecasino.internal.util.sendMessage
import github.mkbaka.fatecasino.internal.util.unregisterListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object BountyEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("悬赏通缉令").color(NamedTextColor.GOLD)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int = 2

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val active = context.playerSessions.values.filter { it.isActive }

        // 找到命运券最多的玩家
        val target = active.maxByOrNull { it.getTicket() }
        if (target == null) return

        val targetId = target.owner
        val bountyAmount = target.getTicket() + 2

        scope.callSync {
            val player = target.playerOrNull ?: return@callSync
            player.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, 30 * 20, 0))

            // 播报坐标
            val loc = player.location
            Component.empty()
                .append(Component.text("悬赏通缉").color(NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("   ").color(NamedTextColor.GRAY))
                .append(Component.text(player.name).color(NamedTextColor.YELLOW))
                .append(Component.text(" 位于 [").color(NamedTextColor.GRAY))
                .append(Component.text(loc.blockX).color(NamedTextColor.YELLOW))
                .append(Component.text(", ").color(NamedTextColor.GRAY))
                .append(Component.text(loc.blockY).color(NamedTextColor.YELLOW))
                .append(Component.text(", ").color(NamedTextColor.GRAY))
                .append(Component.text(loc.blockZ).color(NamedTextColor.YELLOW))
                .append(Component.text("] - 击杀TA可掠夺 ").color(NamedTextColor.GRAY))
                .append(Component.text(bountyAmount).color(NamedTextColor.YELLOW))
                .append(Component.text(" 张券!").color(NamedTextColor.GRAY))
                .broadcast()

            Sound.ENTITY_WITHER_SPAWN.broadcast(SoundCategory.MASTER, 1.0f, 0.5f)
        }

        val tempListener = object : Listener {}
        var claimed = false

        on<PlayerDeathEvent>(listener = tempListener) {
            if (player.uniqueId != targetId) return@on
            if (claimed) return@on

            val killer = player.killer ?: return@on

            val killerSession = context.playerSessions[killer.uniqueId]
            if (killerSession == null || !killerSession.isActive) return@on

            claimed = true

            // 奖励击杀者
            val maxTicket = context.config.playing.maxTicket
            val reward = minOf(bountyAmount, maxTicket - killerSession.getTicket())
            if (reward > 0) {
                killerSession.giveTicket(reward)

                Component.empty()
                    .append(Component.text("悬赏成功").color(NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("   你获得了 ").color(NamedTextColor.GRAY))
                    .append(Component.text(reward).color(NamedTextColor.YELLOW))
                    .append(Component.text(" 张券!").color(NamedTextColor.GRAY))
                    .sendMessage(killer)
            }
        }

        // 创建 BossBar 倒计时
        val durationSeconds = 30
        val bossBar = countdownBossBar(
            context = context,
            title = LegacyComponentSerializer.legacySection().serialize(displayName),
            durationSeconds = durationSeconds
        )
        bossBar.show()

        // 等待击杀或超时
        val startTime = System.currentTimeMillis()
        val endTime = startTime + durationSeconds * 1000L

        while (scope.isActive && System.currentTimeMillis() < endTime && !claimed) {
            val remainingSeconds = ((endTime - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            bossBar.updateProgress(remainingSeconds)
            delay(1000L)
        }

        // 确保即使 scope 取消也能注销监听器
        withContext(NonCancellable + ServerThreadDispatcher) {
            bossBar.hide()
            unregisterListener(tempListener)
        }

        if (!claimed) {
            Component.text("目标逃脱追捕, 悬赏令已失效.").color(NamedTextColor.GRAY).broadcast()
        }
    }

}