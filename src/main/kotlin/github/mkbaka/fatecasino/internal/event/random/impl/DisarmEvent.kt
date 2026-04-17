package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import github.mkbaka.fatecasino.internal.util.countdownBossBar
import github.mkbaka.fatecasino.internal.util.playSound
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory

object DisarmEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("缴械打击").color(NamedTextColor.DARK_RED)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int = 2

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val active = context.playerSessions.values.filter { it.isActive }

        Component.empty()
            .append(Component.text("缴械打击").color(NamedTextColor.DARK_RED))
            .append(Component.newline())
            .append(Component.text("   你的武器不断脱落...").color(NamedTextColor.GRAY))
            .broadcast()

        Sound.ENTITY_ITEM_BREAK.broadcast(SoundCategory.MASTER, 1.0f, 1.0f)

        val bossBar = countdownBossBar(
            context = context,
            title = LegacyComponentSerializer.legacySection().serialize(displayName),
            durationSeconds = 10,
            onTick = {
                scope.callSync {
                    for (session in active) {
                        val player = session.playerOrNull ?: continue
                        if (!session.isActive) continue

                        val mainHand = player.inventory.itemInMainHand
                        if (mainHand.type != Material.AIR) {
                            player.dropItem(mainHand.clone())
                            player.inventory.setItemInMainHand(null)

                            Sound.ENTITY_ITEM_BREAK.playSound(player, SoundCategory.MASTER, 0.5f, 1.5f)
                        }
                    }
                }
            }
        )
        bossBar.run(scope)
    }

}