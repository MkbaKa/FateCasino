package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory

object BigMouthEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("真心话大喇叭").color(NamedTextColor.YELLOW)

    override val phase: GamePhase
        get() = GamePhase.LATE

    override val weight: Int = 2

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val active = context.playerSessions.values.filter { it.isActive }
        if (active.isEmpty()) return

        val target = active.random()
        val ticketCount = target.getTicket()

        scope.callSync {
            val player = target.playerOrNull ?: return@callSync
            val inv = player.inventory

            val valuable = inv.contents
                .filterNotNull()
                .filter { it.type != Material.AIR }
                .sortedByDescending { estimateValue(it.type) }
                .take(3)

            val itemsComponent = if (valuable.isEmpty()) {
                Component.text("空空如也").color(NamedTextColor.GRAY)
            } else {
                valuable.map { item ->
                    Component.translatable(item.type.translationKey()).color(NamedTextColor.YELLOW)
                }.reduce { acc, comp ->
                    acc.append(Component.text(", ").color(NamedTextColor.WHITE)).append(comp)
                }
            }

            Component.empty()
                .append(Component.text("真心话大喇叭").color(NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("   ").color(NamedTextColor.GRAY))
                .append(Component.text(player.name).color(NamedTextColor.YELLOW))
                .append(Component.text(" 包里最值钱的东西: ").color(NamedTextColor.GRAY))
                .append(itemsComponent)
                .append(Component.text(" | 券: ").color(NamedTextColor.GRAY))
                .append(Component.text(ticketCount).color(NamedTextColor.YELLOW))
                .broadcast()

            Sound.BLOCK_BELL_USE.broadcast(SoundCategory.MASTER, 1.0f, 2.0f)
        }
    }

    private fun estimateValue(material: Material): Int {
        return when (material) {
            Material.NETHERITE_INGOT, Material.NETHERITE_BLOCK, Material.ENCHANTED_GOLDEN_APPLE -> 10
            Material.DIAMOND, Material.DIAMOND_BLOCK -> 9
            Material.GOLDEN_APPLE -> 8
            Material.EMERALD, Material.EMERALD_BLOCK -> 7
            Material.GOLD_INGOT, Material.GOLD_BLOCK -> 6
            Material.IRON_INGOT, Material.IRON_BLOCK -> 5
            Material.TNT -> 4
            else -> 1
        }
    }

}