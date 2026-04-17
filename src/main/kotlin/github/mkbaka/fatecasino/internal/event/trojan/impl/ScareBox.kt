package github.mkbaka.fatecasino.internal.event.trojan.impl

import github.mkbaka.fatecasino.internal.event.trojan.TrojanEvent
import github.mkbaka.fatecasino.internal.util.buildItem
import github.mkbaka.fatecasino.internal.util.sendMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.block.data.type.TNT
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.inventory.ItemStack

object ScareBox : TrojanEvent {

    override fun buildIcon(): ItemStack {
        return buildItem(
            material = Material.TNT,
            meta = {
                customName(
                    Component.text("惊吓盲盒")
                        .color(NamedTextColor.GRAY)
                )
            }
        )
    }

    override fun onAccept(sender: Player, target: Player) {
        // 召唤一个三秒后爆炸的 tnt
        target.world.spawn(
            target.location.clone().add(0.0, 1.0, 0.0),
            TNTPrimed::class.java
        ) { tnt ->
            tnt.fuseTicks = 60
            tnt.source = sender
        }

        Component.text("轰隆隆隆隆...").color(NamedTextColor.GRAY).sendMessage(target)
        Component.empty()
            .append(Component.text(target.name).color(NamedTextColor.YELLOW))
            .append(Component.text(" 拆开了你的惊吓盲盒").color(NamedTextColor.GRAY))
            .sendMessage(sender)
    }

}
