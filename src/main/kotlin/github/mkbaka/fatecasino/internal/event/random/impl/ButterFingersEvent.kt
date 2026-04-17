package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.countdownBossBar
import github.mkbaka.fatecasino.internal.util.on
import github.mkbaka.fatecasino.internal.util.unregisterListener
import github.mkbaka.fatecasino.internal.util.ServerThreadDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import java.util.concurrent.ThreadLocalRandom

object ButterFingersEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("黄油手").color(NamedTextColor.YELLOW)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int = 3

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        Component.empty()
            .append(Component.text("黄油手").color(NamedTextColor.YELLOW))
            .append(Component.newline())
            .append(Component.text("   你的手滑了, 每次操作都可能把东西丢出去...").color(NamedTextColor.GRAY))
            .broadcast()

        Sound.ITEM_BUNDLE_DROP_CONTENTS.broadcast(SoundCategory.MASTER, 1.0f, 1.5f)

        val random = ThreadLocalRandom.current()
        val tempListener = object : Listener {}

        // 交互事件: 20% 概率丢弃主手物品
        on<PlayerInteractEvent>(listener = tempListener) {
            val player = player
            val session = context.playerSessions[player.uniqueId]
            if (session == null || !session.isActive) return@on

            if (random.nextInt(100) < 20) {
                val item = player.inventory.itemInMainHand
                if (item.type.isAir) return@on

                // 丢弃主手物品
                player.inventory.setItemInMainHand(null)
                player.world.dropItemNaturally(player.location, item)
            }
        }

        // 切换快捷栏事件: 20% 概率丢弃新持有的物品
        on<PlayerItemHeldEvent>(listener = tempListener) {
            val player = player
            val session = context.playerSessions[player.uniqueId]
            if (session == null || !session.isActive) return@on

            if (random.nextInt(100) < 20) {
                val item = player.inventory.getItem(newSlot)
                if (item == null || item.type.isAir) return@on

                player.inventory.setItem(newSlot, null)
                player.world.dropItemNaturally(player.location, item)
            }
        }

        val bossBar = countdownBossBar(
            context = context,
            title = LegacyComponentSerializer.legacySection().serialize(displayName),
            durationSeconds = 20
        )
        bossBar.run(scope)

        withContext(NonCancellable + ServerThreadDispatcher) {
            unregisterListener(tempListener)
        }

        Component.empty()
            .append(Component.text("黄油手").color(NamedTextColor.YELLOW))
            .append(Component.newline())
            .append(Component.text("   你的手终于稳了.").color(NamedTextColor.GRAY))
            .broadcast()
    }

}