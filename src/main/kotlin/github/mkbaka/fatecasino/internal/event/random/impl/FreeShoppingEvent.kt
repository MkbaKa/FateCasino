package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.countdownBossBar
import github.mkbaka.fatecasino.internal.util.ServerThreadDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Sound
import org.bukkit.SoundCategory

object FreeShoppingEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("零元购").color(NamedTextColor.GREEN)

    override val phase: GamePhase
        get() = GamePhase.EARLY

    override val weight: Int = 1

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        context.priceOverride = 1

        Component.empty()
            .append(Component.text("零元购").color(NamedTextColor.GREEN))
            .append(Component.newline())
            .append(Component.text("   所有赌场技能消耗降为 ").color(NamedTextColor.GRAY))
            .append(Component.text(1).color(NamedTextColor.YELLOW))
            .append(Component.text(" 券, 持续 ").color(NamedTextColor.GRAY))
            .append(Component.text(15).color(NamedTextColor.YELLOW))
            .append(Component.text(" 秒!").color(NamedTextColor.GRAY))
            .broadcast()

        Sound.ENTITY_VILLAGER_YES.broadcast(SoundCategory.MASTER, 1.0f, 1.0f)

        val bossBar = countdownBossBar(
            context = context,
            title = LegacyComponentSerializer.legacySection().serialize(displayName),
            durationSeconds = 15
        )
        bossBar.run(scope)

        withContext(NonCancellable + ServerThreadDispatcher) {
            context.priceOverride = null
        }

        Component.empty()
            .append(Component.text("零元购").color(NamedTextColor.GREEN))
            .append(Component.newline())
            .append(Component.text("   价格恢复正常.").color(NamedTextColor.GRAY))
            .broadcast()
    }

}