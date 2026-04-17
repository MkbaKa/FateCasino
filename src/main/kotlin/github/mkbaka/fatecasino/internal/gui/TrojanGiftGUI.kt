package github.mkbaka.fatecasino.internal.gui

import github.mkbaka.fatecasino.internal.event.trojan.TrojanEvent
import github.mkbaka.fatecasino.internal.event.trojan.impl.CurseTransfer
import github.mkbaka.fatecasino.internal.event.trojan.impl.PainMask
import github.mkbaka.fatecasino.internal.event.trojan.impl.RealMoney
import github.mkbaka.fatecasino.internal.event.trojan.impl.ScareBox
import github.mkbaka.fatecasino.internal.game.GameManager
import github.mkbaka.fatecasino.internal.menu.Menu
import github.mkbaka.fatecasino.internal.menu.menu
import github.mkbaka.fatecasino.internal.util.buildItem
import github.mkbaka.fatecasino.internal.util.playSound
import github.mkbaka.fatecasino.internal.util.playerOrNull
import github.mkbaka.fatecasino.internal.util.sendMessage
import github.mkbaka.fatecasino.internal.util.session
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory

object TrojanGiftGUI {

    private val events: List<TrojanEvent> = listOf(
        RealMoney,
        PainMask,
        ScareBox,
        CurseTransfer
    )

    val menu: Menu = menu {
        pattern(
            "#########",
            "#1#2#3#4#",
            "B########"
        )

        slot(
            '#', buildItem(
            material = Material.GRAY_STAINED_GLASS_PANE,
            meta = { customName(Component.empty()) }
        ))

        slot(
            'B',
            buildItem(
                material = Material.ARROW,
                meta = {
                    customName(Component.text("返回上一页"))
                }
            )
        ) { open(CasinoGUI.menu) }

        events.withIndex().forEach { (index, event) ->
            slot(
                '1' + index,
                event.buildIcon()
            ) {
                val currentSession = player.session
                if (currentSession == null) {
                    Component.text("你不在这场游戏中.")
                        .sendMessage(player)
                    return@slot
                }

                open(SelectTargetGUI.createMenu(player.uniqueId) { targetId ->
                    val targetSession = targetId.playerOrNull?.session
                    if (targetSession == null) {
                        Component.text("对方不在这场游戏中.")
                            .sendMessage(player)
                        return@createMenu
                    }

                    val context = GameManager.currentPhase!!.context
                    currentSession.consumeThen(
                        (context.priceOverride ?: 1) * context.priceMultiplier,
                        then = {
                            event.sendGiftMessage(player, targetSession.player)
                        },
                        deny = {
                            Sound.ENTITY_VILLAGER_TRADE.playSound(player, SoundCategory.MASTER, 1.0f, 1.0f)
                            Component.text("命运券不足")
                                .sendMessage(player)
                        }
                    )
                })
            }
        }
    }

}
