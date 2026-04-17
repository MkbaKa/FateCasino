package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.SoundCategory

object PositionSwapEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("空间置换").color(NamedTextColor.LIGHT_PURPLE)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int = 2

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val active = context.playerSessions.values.filter { it.isActive }
        if (active.size < 2) return

        Component.empty()
            .append(Component.text("空间置换!").color(NamedTextColor.LIGHT_PURPLE))
            .append(Component.newline())
            .append(Component.text("   你感到一阵眩晕...").color(NamedTextColor.GRAY))
            .broadcast()

        Sound.ENTITY_ENDERMAN_TELEPORT.broadcast(SoundCategory.MASTER, 1.0f, 1.5f)

        scope.callSync {
            // 收集所有存活玩家的位置
            val players = active.mapNotNull { it.playerOrNull }
            if (players.size < 2) return@callSync

            val positions = players.map { it.location.clone() }
            val shuffledIndices = (0 until players.size).shuffled()

            for (i in players.indices) {
                players[i].teleport(positions[shuffledIndices[i]])
            }

            Component.empty()
                .append(Component.text("位置交换完成!").color(NamedTextColor.LIGHT_PURPLE))
                .append(Component.newline())
                .append(Component.text("   所有玩家的位置已被随机交换!").color(NamedTextColor.GRAY))
                .broadcast()
        }
    }

}