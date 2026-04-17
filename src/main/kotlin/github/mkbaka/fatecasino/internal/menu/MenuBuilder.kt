package github.mkbaka.fatecasino.internal.menu

import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack

fun menu(builder: MenuBuilder.() -> Unit): Menu = MenuBuilder().apply(builder).build()

class MenuBuilder {

    private var rows: Int = 1
    private var patterns: List<String>? = null
    private val charSlots = mutableMapOf<Char, MenuIcon>()
    private val indexSlots = mutableMapOf<Int, MenuIcon>()

    private var onCloseCallback: (() -> Unit)? = null

    var title: Component = Component.empty()

    fun pattern(vararg lines: String): MenuBuilder {
        require(lines.isNotEmpty() && lines.size <= 6) { "行数需要在 1 到 6 之间" }
        require(lines.all { it.length == 9 }) { "每行必须为 9 个字符" }
        rows = lines.size
        patterns = lines.toList()
        return this
    }

    fun slot(char: Char, item: ItemStack, onClick: (MenuContext.() -> Unit)? = null): MenuBuilder {
        charSlots[char] = MenuIcon(item, onClick)
        return this
    }

    fun slot(index: Int, item: ItemStack, onClick: (MenuContext.() -> Unit)? = null): MenuBuilder {
        indexSlots[index] = MenuIcon(item, onClick)
        return this
    }

    fun onClose(onCloseCallback: () -> Unit): MenuBuilder {
        this.onCloseCallback = onCloseCallback
        return this
    }

    fun build(): Menu {
        if (patterns == null) error("未指定模板")

        val slots = hashMapOf<Int, MenuIcon>()

        for ((row, line) in patterns!!.withIndex()) {
            for ((column, char) in line.withIndex()) {
                val slot = charSlots[char] ?: continue
                val index = row * 9 + column
                slots[index] = slot
            }
        }

        for ((index, slot) in indexSlots) {
            slots[index] = slot
        }

        return Menu(title, rows * 9, slots, onCloseCallback)
    }

}
