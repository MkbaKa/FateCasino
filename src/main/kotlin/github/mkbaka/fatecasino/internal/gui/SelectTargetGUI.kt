package github.mkbaka.fatecasino.internal.gui

import github.mkbaka.fatecasino.internal.menu.Menu
import github.mkbaka.fatecasino.internal.menu.menu
import github.mkbaka.fatecasino.internal.util.buildItem
import github.mkbaka.fatecasino.internal.util.currentSessions
import github.mkbaka.fatecasino.internal.util.session
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.meta.SkullMeta
import java.util.UUID

object SelectTargetGUI {

    fun createMenu(owner: UUID, page: Int = 0, onSelect: (UUID) -> Unit): Menu {
        val sessions = currentSessions.filter { it.owner != owner }
        val pageSize = 7
        val pageItems = sessions.drop(page * pageSize).take(pageSize)

        return menu {
            pattern(
                "#########",
                "#       #",
                "#       #",
                "#       #",
                "B########"
            )

            slot('#', buildItem(Material.GRAY_STAINED_GLASS_PANE))

            slot(
                'B',
                buildItem(
                    material = Material.ARROW,
                    meta = {
                        customName(Component.text("返回上一页"))
                    }
                )
            ) { open(TrojanGiftGUI.menu) }

            pageItems.withIndex().forEach { (index, session) ->
                slot(10 + index, buildHead(session)) {
                    onSelect(session.owner)
                    close()
                }
            }

            if ((page + 1) * pageSize < sessions.size) {
                slot(
                    49, buildItem(
                        material = Material.ARROW,
                        meta = {
                            customName(Component.text("下一页"))
                        }
                    )
                ) {
                    open(createMenu(owner, page + 1, onSelect))
                }
            }
        }
    }

    private fun buildHead(session: github.mkbaka.fatecasino.internal.phase.data.PlayerSession) =
        buildItem(
            material = Material.PLAYER_HEAD,
            meta = {
                this as SkullMeta
                playerProfile = session.player.playerProfile
            }
        )

}
