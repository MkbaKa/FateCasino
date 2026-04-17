package github.mkbaka.fatecasino.internal.util

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.checkerframework.checker.units.qual.m

fun buildItem(
    material: Material,
    stack: (ItemStack.() -> Unit)? = null,
    meta: (ItemMeta.() -> Unit)? = null
): ItemStack {
    val itemStack = ItemStack(material)
    stack?.invoke(itemStack)

    val itemMeta = itemStack.itemMeta
    if (itemMeta != null && meta != null) {
        itemStack.itemMeta = itemMeta.apply(meta)
    }
    return itemStack
}