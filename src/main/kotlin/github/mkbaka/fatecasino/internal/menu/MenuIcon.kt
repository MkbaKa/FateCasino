package github.mkbaka.fatecasino.internal.menu

import org.bukkit.inventory.ItemStack

data class MenuIcon(
    val item: ItemStack,
    val onClick: (MenuContext.() -> Unit)? = null,
)
