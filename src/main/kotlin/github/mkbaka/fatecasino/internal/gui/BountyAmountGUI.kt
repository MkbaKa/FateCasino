package github.mkbaka.fatecasino.internal.gui

import github.mkbaka.fatecasino.internal.menu.Menu
import github.mkbaka.fatecasino.internal.menu.menu
import github.mkbaka.fatecasino.internal.util.buildItem
import kotlinx.coroutines.CompletableDeferred
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.inventory.meta.KnowledgeBookMeta

object BountyAmountGUI {

    fun createMenu(targetName: String, onSelect: (Int) -> Unit): Menu = menu {
        pattern(
            "####5####",
            "#########",
            "#1#2#3#4#",
            "#########",
            "P###B####",
        )

        slot('#', buildItem(Material.GRAY_STAINED_GLASS_PANE))

        slot(
            'B',
            buildItem(
                material = Material.ARROW,
                meta = { customName(Component.text("返回上一页")) }
            )
        ) { close() }

        slot(
            'P',
            buildItem(
                material = Material.BOOK,
                meta = {
                    customName(
                        Component.empty()
                            .append(Component.text("追杀 ").color(NamedTextColor.RED))
                            .append(Component.text(targetName).color(NamedTextColor.YELLOW))
                    )

                    val desc = listOf(
                        Component.empty(),
                        Component.text("选择一个悬赏档位").color(NamedTextColor.GOLD),
                        Component.text("悬赏目标后会根据档位给予 发光效果 以及 坐标暴露").color(NamedTextColor.YELLOW),
                    )

                    lore(desc)
                }
            )
        )

        val amounts = listOf(1, 2, 3, 4, 5)
        val glowDurations = listOf(20, 30, 40, 50, 60) // amount * 10 + 10
        val bountyDurations = listOf(30, 45, 60, 75, 90) // 30 + (amount - 1) * 15

        amounts.forEachIndexed { index, amount ->
            val slotChar = ('1'.code + index).toChar()
            val glowSec = glowDurations[index]
            val bountySec = bountyDurations[index]

            // 位置播报描述
            val locationDesc = when (amount) {
                1 -> "无坐标暴露"
                2 -> "创建时播报坐标"
                3 -> "创建时 + 30秒播报坐标"
                4 -> "创建时 + 30秒 + 60秒播报坐标"
                5 -> "创建时 + 30秒 + 60秒 + 70秒 + 80秒播报坐标"
                else -> error("只支持 1 - 5 的悬赏金额")
            }

            slot(
                slotChar,
                buildItem(
                    material = Material.SUNFLOWER,
                    meta = {
                        customName(
                            Component.empty()
                                .append(Component.text("悬赏 ").color(NamedTextColor.GOLD))
                                .append(Component.text(amount).color(NamedTextColor.YELLOW))
                                .append(Component.text(" 券").color(NamedTextColor.GOLD))
                        )

                        lore(
                            listOf(
                                Component.empty(),
                                Component.empty()
                                    .append(Component.text("目标发光: ").color(NamedTextColor.GRAY))
                                    .append(Component.text(glowSec).color(NamedTextColor.YELLOW))
                                    .append(Component.text(" 秒").color(NamedTextColor.GRAY)),
                                Component.empty()
                                    .append(Component.text("悬赏时效: ").color(NamedTextColor.GRAY))
                                    .append(Component.text(bountySec).color(NamedTextColor.YELLOW))
                                    .append(Component.text(" 秒").color(NamedTextColor.GRAY)),
                                Component.empty()
                                    .append(Component.text("坐标暴露: ").color(NamedTextColor.GRAY))
                                    .append(Component.text(locationDesc).color(NamedTextColor.YELLOW)),
                                Component.empty(),
                                Component.text("击杀者获得全部悬赏金额").color(NamedTextColor.GRAY)
                            )
                        )
                    }
                )
            ) {
                onSelect(amount)
                close()
            }
        }
    }

}