package github.mkbaka.fatecasino.internal.menu

import github.mkbaka.fatecasino.internal.util.on
import kotlinx.coroutines.NonCancellable.isCancelled
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class Menu(
    title: Component,
    size: Int,
    private val slots: Map<Int, MenuIcon>,
    private val onClose: (() -> Unit)?
) : InventoryHolder {

    private val inventory = Bukkit.createInventory(this, size, title)

    init {
        for ((index, slot) in slots) {
            inventory.setItem(index, slot.item)
        }
    }

    fun handleClick(slot: Int, player: Player, event: InventoryClickEvent) {
        slots[slot]?.onClick?.invoke(MenuContext(player, event, this))
    }

    fun open(player: Player) = player.openInventory(inventory)

    override fun getInventory() = inventory

    companion object {

        fun subscribe(listener: Listener) {
            on<InventoryClickEvent>(listener = listener) {
                val menu = inventory.holder as? Menu ?: return@on
                val player = whoClicked as? Player ?: return@on
                isCancelled = true
                menu.handleClick(slot, player, this)
            }
            on<InventoryDragEvent>(listener = listener) {
                if (inventory.holder !is Menu) return@on
                isCancelled = true
            }
            on<InventoryCloseEvent>(listener = listener) {
                val holder = inventory.holder as? Menu ?: return@on
                holder.onClose?.invoke()
            }
        }

    }

}
