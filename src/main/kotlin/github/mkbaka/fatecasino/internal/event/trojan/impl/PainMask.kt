package github.mkbaka.fatecasino.internal.event.trojan.impl

import github.mkbaka.fatecasino.FateCasino
import github.mkbaka.fatecasino.internal.event.trojan.TrojanEvent
import github.mkbaka.fatecasino.internal.util.buildItem
import github.mkbaka.fatecasino.internal.util.on
import github.mkbaka.fatecasino.internal.util.sendMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object PainMask : TrojanEvent {

    private val NAMESPACE = NamespacedKey(FateCasino.INSTANCE, "pain_mask")

    private val getter: ItemStack
        get() = buildItem(
            material = Material.CARVED_PUMPKIN,
            meta = {
                val description = Component.empty()
                    .append(Component.text("x").decorate(TextDecoration.OBFUSCATED))
                    .append(Component.text("只有死亡能将你解脱...").color(NamedTextColor.GRAY))
                    .append(Component.text("x").decorate(TextDecoration.OBFUSCATED))

                customName(Component.text("痛苦面具").color(NamedTextColor.RED))
                lore(listOf(description))
                persistentDataContainer[NAMESPACE, PersistentDataType.BOOLEAN] = true
            }
        )

    private val cursedPlayers = mutableSetOf<UUID>()

    override fun buildIcon(): ItemStack {
        return buildItem(
            material = Material.CARVED_PUMPKIN,
            meta = {
                customName(
                    Component.text("痛苦面具")
                        .color(NamedTextColor.RED)
                )
            }
        )
    }

    override fun onAccept(sender: Player, target: Player) {
        val helmet = target.inventory.helmet
        if (helmet != null && helmet.type != Material.AIR) {
            // 掉落原有头盔
            target.world.dropItemNaturally(target.location, helmet)
            target.inventory.helmet = null
        }

        target.inventory.helmet = getter
        cursedPlayers.add(target.uniqueId)

        Component.text("你戴上了痛苦面具...").color(NamedTextColor.GRAY).sendMessage(target)
        Component.empty()
            .append(Component.text(target.name).color(NamedTextColor.YELLOW))
            .append(Component.text(" 戴上了你的痛苦面具").color(NamedTextColor.GRAY))
            .sendMessage(sender)
    }

    fun isCursed(player: Player): Boolean = player.uniqueId in cursedPlayers

    fun transfer(from: Player, to: Player) {
        if (!isCursed(from)) return

        from.inventory.helmet = null
        cursedPlayers.remove(from.uniqueId)

        val helmet = to.inventory.helmet
        if (helmet != null && helmet.type != Material.AIR) {
            to.world.dropItemNaturally(to.location, helmet)
        }

        to.inventory.helmet = getter
        cursedPlayers.add(to.uniqueId)
    }

    fun isMaskItem(itemStack: ItemStack): Boolean =
        itemStack.itemMeta?.persistentDataContainer
            ?.get(NAMESPACE, PersistentDataType.BOOLEAN) == true

    fun subscribe(listener: Listener) {
        // 防止南瓜头被摘下来
        // 绑定诅咒应该也行 但是那行附魔名字太丑了
        on<InventoryClickEvent>(listener = listener) {
            val player = whoClicked as? Player ?: return@on
            if (!isCursed(player)) return@on

            val item = currentItem ?: return@on
            if (isMaskItem(item)) {
                isCancelled = true
            }
        }
        on<PlayerDeathEvent>(listener = listener) {
            cursedPlayers.remove(entity.uniqueId)
        }
    }

}
