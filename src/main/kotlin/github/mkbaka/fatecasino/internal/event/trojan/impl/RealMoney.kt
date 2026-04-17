package github.mkbaka.fatecasino.internal.event.trojan.impl

import github.mkbaka.fatecasino.internal.event.trojan.TrojanEvent
import github.mkbaka.fatecasino.internal.util.buildItem
import github.mkbaka.fatecasino.internal.util.sendMessage
import github.mkbaka.fatecasino.internal.util.session
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object RealMoney : TrojanEvent {

    override fun buildIcon(): ItemStack {
        return buildItem(
            material = Material.EMERALD,
            meta = {
                customName(
                    Component.text("真金白银")
                        .color(NamedTextColor.GREEN)
                )
            }
        )
    }

    override fun onAccept(sender: Player, target: Player) {
        val targetSession = target.session
        if (targetSession == null) {
            Component.text("你不在这场游戏中, 无法领取对方的礼物").color(NamedTextColor.GRAY)
                .sendMessage(target)

            Component.text("对方不在这场游戏中, 无法领取你的礼物").color(NamedTextColor.GRAY)
                .sendMessage(sender)
            return
        }

        targetSession.giveTicket(1)
        Component.text("你接受了对方的礼物").color(NamedTextColor.GRAY).sendMessage(target)
        Component.empty()
            .append(Component.text(target.name).color(NamedTextColor.YELLOW))
            .append(Component.text(" 接受了你的礼物").color(NamedTextColor.GRAY))
            .sendMessage(sender)
    }

}
