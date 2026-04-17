package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import github.mkbaka.fatecasino.internal.util.sendMessage
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Sound
import org.bukkit.SoundCategory

object RobinHoodEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("罗宾汉降临").color(NamedTextColor.GREEN)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int = 2

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val active = context.playerSessions.values.filter { it.isActive }

        if (active.isEmpty()) return

        // 收集所有存活玩家的票数总和
        val totalTickets = active.sumOf { it.getTicket() }

        if (totalTickets == 0) {
            // 大家都没票，不触发
            Component.empty()
                .append(Component.text("罗宾汉降临").color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("   但大家都没券...").color(NamedTextColor.GRAY))
                .broadcast()
            return
        }

        // 平分给所有人
        val perPlayer = totalTickets / active.size
        val remainder = totalTickets % active.size
        val shuffled = active.shuffled()

        for ((i, session) in shuffled.withIndex()) {
            val amount = perPlayer + if (i < remainder) 1 else 0
            session.setTicket(amount)
            val player = session.playerOrNull ?: continue
            Component.empty()
                .append(Component.text("罗宾汉将你的券分配为 ").color(NamedTextColor.GRAY))
                .append(Component.text(amount).color(NamedTextColor.YELLOW))
                .append(Component.text(" 券").color(NamedTextColor.GRAY))
                .sendMessage(player)
        }

        Component.empty()
            .append(Component.text("罗宾汉降临").color(NamedTextColor.GREEN))
            .append(Component.newline())
            .append(Component.text("   均贫富, 天道轮回...").color(NamedTextColor.GRAY))
            .broadcast()

        Sound.ENTITY_VILLAGER_CELEBRATE.broadcast(SoundCategory.MASTER, 1.0f, 1.0f)
    }

}