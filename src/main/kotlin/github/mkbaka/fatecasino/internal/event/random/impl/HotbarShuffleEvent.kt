package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import github.mkbaka.fatecasino.internal.util.countdownBossBar
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Sound
import org.bukkit.SoundCategory

object HotbarShuffleEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("乾坤大挪移").color(NamedTextColor.DARK_PURPLE)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int = 3

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val active = context.playerSessions.values.filter { it.isActive }

        Component.empty()
            .append(Component.text("乾坤大挪移!").color(NamedTextColor.DARK_PURPLE))
            .append(Component.newline())
            .append(Component.text("   你感到手忙脚乱...").color(NamedTextColor.GRAY))
            .broadcast()

        Sound.ENTITY_ENDERMAN_TELEPORT.broadcast(SoundCategory.MASTER, 1.0f, 0.3f)

        val bossBar = countdownBossBar(
            context = context,
            title = LegacyComponentSerializer.legacySection().serialize(displayName),
            durationSeconds = 10,
            onTick = {
                scope.callSync {
                    for (session in active) {
                        val player = session.playerOrNull ?: continue
                        if (!session.isActive) continue

                        val inv = player.inventory

                        // 收集快捷栏物品
                        val hotbar = (0..8).map { inv.getItem(it) }
                        val shuffled = hotbar.toMutableList()
                        shuffled.shuffle()

                        // 放回
                        for (i in 0..8) {
                            inv.setItem(i, shuffled[i])
                        }
                    }
                }
            }
        )
        bossBar.run(scope)
    }

}