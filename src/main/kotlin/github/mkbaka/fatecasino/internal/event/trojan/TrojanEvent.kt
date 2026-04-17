package github.mkbaka.fatecasino.internal.event.trojan

import github.mkbaka.fatecasino.internal.game.CallbackManager
import github.mkbaka.fatecasino.internal.util.onClick
import github.mkbaka.fatecasino.internal.util.sendMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

interface TrojanEvent {

    fun buildIcon(): ItemStack

    fun onAccept(sender: Player, target: Player)

    fun onReject(sender: Player, target: Player) {
        Component.text("你拒绝了对方的礼物").sendMessage(target)
        Component.empty()
            .append(Component.text(target.name))
            .append(Component.text(" 拒绝了你的礼物"))
            .sendMessage(sender)
    }

    fun sendGiftMessage(sender: Player, target: Player) {
        val group = CallbackManager.createGroup()
        Component.empty()
            .append(Component.text(sender.name))
            .append(Component.text(" 给你送了一份礼物 "))
            .append(
                Component.text("[接受]")
                    .color(NamedTextColor.GREEN)
                    .onClick(group = group) { onAccept(sender, target) }
            )
            .append(Component.text(" 或是 "))
            .append(
                Component.text("[拒绝]")
                    .color(NamedTextColor.RED)
                    .onClick(group = group) { onReject(sender, target) }
            )
            .sendMessage(target)
    }

}
