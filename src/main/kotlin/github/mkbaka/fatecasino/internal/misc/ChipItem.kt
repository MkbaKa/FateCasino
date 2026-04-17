package github.mkbaka.fatecasino.internal.misc

import github.mkbaka.fatecasino.FateCasino
import github.mkbaka.fatecasino.internal.gui.CasinoGUI
import github.mkbaka.fatecasino.internal.util.buildItem
import github.mkbaka.fatecasino.internal.util.on
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object ChipItem {

    private val NAMESPACE = NamespacedKey(FateCasino.INSTANCE, "is_chip")

    private val name: Component
        get() = Component.empty()
            .append(
                Component.text(" xxx ")
                    .color(NamedTextColor.GRAY)
                    .decorate(TextDecoration.OBFUSCATED)
            )
            .append(Component.text("命运筹码").color(NamedTextColor.YELLOW))
            .append(
                Component.text(" xxx ")
                    .color(NamedTextColor.GRAY)
                    .decorate(TextDecoration.OBFUSCATED)
            )
            .append(Component.text())

    private val descriptions: List<Component>
        get() = listOf(
            Component.empty(),

            Component.text("  它在你的掌心微微发烫, 仿佛在渴求什么.")
                .color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false),

            Component.empty(),

            Component.text(" ▸ 右键开启命运赌局")
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false),

            Component.text(" ※ 愿你的贪婪, 配得上你的运气.")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false),

            Component.text(" 梭哈, 或者死亡.")
                .color(NamedTextColor.DARK_RED)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false)
        )

    val getter: ItemStack
        get() = buildItem(
            material = Material.MUSIC_DISC_PIGSTEP,
            meta = {
                customName(name)
                lore(descriptions)
                persistentDataContainer[NAMESPACE, PersistentDataType.BOOLEAN] = true
            }
        )

    fun isChipItem(item: ItemStack): Boolean =
        item.itemMeta?.persistentDataContainer
            ?.get(NAMESPACE, PersistentDataType.BOOLEAN) == true

    fun subscribe(listener: Listener) {
        on<PlayerDropItemEvent>(listener = listener) {
            if (!isChipItem(itemDrop.itemStack)) return@on

            isCancelled = true
        }
        on<PlayerInteractEvent>(listener = listener) {
            if (hand != EquipmentSlot.HAND) return@on

            if (!isChipItem(player.inventory.itemInMainHand)) return@on

            if (!action.isRightClick) return@on

            isCancelled = true

            CasinoGUI.menu.open(player)
        }
    }

}