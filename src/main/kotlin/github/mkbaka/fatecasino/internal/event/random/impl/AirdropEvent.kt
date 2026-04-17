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
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.Chest
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ThreadLocalRandom

object AirdropEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("天降横财").color(NamedTextColor.GOLD)

    override val phase: GamePhase
        get() = GamePhase.EARLY

    override val weight: Int = 3

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val random = ThreadLocalRandom.current()
        val world = context.world

        // 随机空投坐标
        val spawnRadius = context.config.lobby.platformRadius
        val x = random.nextInt(-spawnRadius, spawnRadius)
        val z = random.nextInt(-spawnRadius, spawnRadius)
        val y = world.getHighestBlockYAt(x, z)

        scope.callSync {
            val loc = Location(world, x.toDouble(), y.toDouble(), z.toDouble())

            // 刷一个箱子
            val chestBlock = loc.block
            chestBlock.type = Material.CHEST

            Component.empty()
                .append(Component.text("[天降横财]").color(NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("   空投宝箱降临在 [").color(NamedTextColor.GRAY))
                .append(Component.text(x).color(NamedTextColor.YELLOW))
                .append(Component.text(", ").color(NamedTextColor.GRAY))
                .append(Component.text(y).color(NamedTextColor.YELLOW))
                .append(Component.text(", ").color(NamedTextColor.GRAY))
                .append(Component.text(z).color(NamedTextColor.YELLOW))
                .append(Component.text("]! 第一个碰到的人直接获得 ").color(NamedTextColor.GRAY))
                .append(Component.text(3).color(NamedTextColor.YELLOW))
                .append(Component.text(" 券!").color(NamedTextColor.GRAY))
                .broadcast()

            Sound.ENTITY_FIREWORK_ROCKET_BLAST.broadcast(SoundCategory.MASTER, 1.0f, 1.0f)
        }

        // BossBar 提示
        val durationSeconds = 60
        val bossBar = countdownBossBar(
            context = context,
            title = LegacyComponentSerializer.legacySection().serialize(displayName),
            durationSeconds = durationSeconds
        )
        bossBar.show()

        // 等玩家去捡
        val tempListener = object : Listener {}
        var claimed = false

        on<PlayerInteractEvent>(listener = tempListener) {
            if (claimed) return@on
            if (hand != EquipmentSlot.HAND) return@on
            if (action != Action.RIGHT_CLICK_BLOCK) return@on

            val clickedBlock = clickedBlock
            if (clickedBlock == null || clickedBlock.type != Material.CHEST) return@on

            val session = context.playerSessions[player.uniqueId]
            if (session == null || !session.isActive) return@on

            // 排除其他箱子 只匹配空投坐标
            val loc = clickedBlock.location
            if (loc.blockX != x || loc.blockZ != z) return@on

            claimed = true
            session.giveTicket(3)

            Component.empty()
                .append(Component.text(player.name).color(NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("   拾取了空投宝箱, 获得 ").color(NamedTextColor.GRAY))
                .append(Component.text(3).color(NamedTextColor.YELLOW))
                .append(Component.text(" 张命运券!").color(NamedTextColor.GRAY))
                .broadcast()

            Sound.ENTITY_PLAYER_LEVELUP.broadcast(SoundCategory.MASTER, 1.0f, 1.0f)

            // 移除箱子
            clickedBlock.type = Material.AIR
        }

        // 最多等待 60 秒, 没人捡就消失
        val startTime = System.currentTimeMillis()
        val endTime = startTime + durationSeconds * 1000L

        while (scope.isActive && System.currentTimeMillis() < endTime && !claimed) {
            val remainingSeconds = ((endTime - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            bossBar.updateProgress(remainingSeconds)
            delay(1000L)
        }

        // 清理资源
        withContext(NonCancellable + ServerThreadDispatcher) {
            bossBar.hide()
            unregisterListener(tempListener)
        }

        if (!claimed) {
            withContext(NonCancellable + ServerThreadDispatcher) {
                val loc = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                if (loc.block.type == Material.CHEST) {
                    loc.block.type = Material.AIR
                }
            }
            Component.text("空投宝箱无人拾取, 已消失.").color(NamedTextColor.GRAY).broadcast()
        }
    }

}