package github.mkbaka.fatecasino.internal.menu

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

class MenuContext(
    val player: Player,
    val event: InventoryClickEvent,
    val menu: Menu,
) {

    fun close() = player.closeInventory()

    fun open(menu: Menu) = menu.open(player)

}
