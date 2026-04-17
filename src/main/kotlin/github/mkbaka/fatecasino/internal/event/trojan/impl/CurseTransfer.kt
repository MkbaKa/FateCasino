package github.mkbaka.fatecasino.internal.event.trojan.impl

import github.mkbaka.fatecasino.internal.event.trojan.TrojanEvent
import github.mkbaka.fatecasino.internal.util.buildItem
import github.mkbaka.fatecasino.internal.util.sendMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType

object CurseTransfer : TrojanEvent {

    // 可以转移的负面效果
    private val TRANSFERABLE_EFFECTS = setOf(
        // 中毒
        PotionEffectType.POISON,
        // 凋灵
        PotionEffectType.WITHER,
        // 缓慢
        PotionEffectType.SLOWNESS,
        // 挖掘疲劳
        PotionEffectType.MINING_FATIGUE,
        // 虚弱
        PotionEffectType.WEAKNESS,
        // 失明
        PotionEffectType.BLINDNESS,
        // 反胃
        PotionEffectType.NAUSEA,
        // 饥饿
        PotionEffectType.HUNGER
    )

    override fun buildIcon(): ItemStack {
        return buildItem(
            material = Material.POTION,
            meta = {
                this as PotionMeta
                customName(
                    Component.text("祸水东引")
                        .color(NamedTextColor.GREEN)
                )
                basePotionType = PotionType.POISON
            }
        )
    }

    override fun onAccept(sender: Player, target: Player) {
        val transferred = mutableListOf<String>()

        // 转移痛苦面具
        if (PainMask.isCursed(sender)) {
            PainMask.transfer(sender, target)
            transferred.add("痛苦面具")
        }

        // 转移负面药水效果
        val negativeEffects = sender.activePotionEffects.filter {
            it.type in TRANSFERABLE_EFFECTS
        }

        if (negativeEffects.isNotEmpty()) {
            // withXXX 会搓一个仅 指定参数 不同 其他参数完全相同的实例出来
            // 那么只要指定参数与来源相同 这就等于是一个 copy 函数
            val toTransfer = negativeEffects.map { it.withIcon(it.hasIcon()) }
            for (effect in toTransfer) {
                sender.removePotionEffect(effect.type)
                target.addPotionEffect(effect)
            }
            transferred.add("${toTransfer.size} 个负面效果")
        }

        if (transferred.isEmpty()) {
            Component.text("对方没有任何负面效果可转移...").color(NamedTextColor.GRAY).sendMessage(target)
            Component.empty()
                .append(Component.text(target.name).color(NamedTextColor.YELLOW))
                .append(Component.text(" 拆开了你的祸水东引, 但你没有负面效果可转移").color(NamedTextColor.GRAY))
                .sendMessage(sender)
            return
        }

        val summary = transferred.joinToString(" 和 ")
        Component.empty()
            .append(Component.text("你被转移了 ").color(NamedTextColor.GRAY))
            .append(Component.text(summary).color(NamedTextColor.YELLOW))
            .append(Component.text("!").color(NamedTextColor.GRAY))
            .sendMessage(target)
        Component.empty()
            .append(Component.text("成功将 ").color(NamedTextColor.GRAY))
            .append(Component.text(summary).color(NamedTextColor.YELLOW))
            .append(Component.text(" 转移给了 ").color(NamedTextColor.GRAY))
            .append(Component.text(target.name).color(NamedTextColor.YELLOW))
            .sendMessage(sender)
    }

}
