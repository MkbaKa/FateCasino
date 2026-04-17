package github.mkbaka.fatecasino.internal.event.slotmachine

import github.mkbaka.fatecasino.internal.util.buildItem
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType

interface SlotMachinePhase {

    val borderMaterial: Material

    val scrollItems: List<ScrollItem>

    val defaultPunishments: List<RewardEvent>

    object BAIT : SlotMachinePhase {

        override val borderMaterial: Material = Material.GRAY_STAINED_GLASS_PANE

        override val defaultPunishments: List<RewardEvent> = emptyList()

        override val scrollItems: List<ScrollItem> = listOf(
            // 小赚
            scroll(Material.COOKED_BEEF, RewardEvent.Item(Material.COOKED_BEEF, 16)),
            scroll(Material.IRON_INGOT, RewardEvent.Item(Material.IRON_INGOT, 10)),
            scroll(Material.IRON_CHESTPLATE, RewardEvent.Item(Material.IRON_CHESTPLATE)),
            scroll(Material.GOLDEN_CARROT, RewardEvent.Item(Material.GOLDEN_CARROT, 8)),
            scroll(Material.ENCHANTED_BOOK, RewardEvent.Item(Material.ENCHANTED_BOOK)),
            scroll(Material.GOLDEN_APPLE, RewardEvent.Heal(2.0)),
            // 大赚
            scroll(Material.DIAMOND, RewardEvent.Ticket(1)),
            scroll(Material.DIAMOND_SWORD, RewardEvent.Item(
                Material.DIAMOND_SWORD,
                enchantments = mapOf(Enchantment.SHARPNESS to 3)
            )),
            scroll(Material.EMERALD, RewardEvent.Item(Material.EMERALD, 5)),
        )

    }

    object GREED : SlotMachinePhase {

        override val borderMaterial: Material = Material.ORANGE_STAINED_GLASS_PANE

        override val defaultPunishments: List<RewardEvent> = listOf(
            RewardEvent.Damage(6.0),
            RewardEvent.Effect(PotionEffectType.SLOWNESS, 10, 1),
            RewardEvent.Effect(PotionEffectType.WEAKNESS, 8, 1)
        )

        override val scrollItems: List<ScrollItem> = listOf(
            // 极品
            scroll(Material.DIAMOND_BLOCK, RewardEvent.Special(RewardEvent.Special.Action.CURSED_ARMOR)),
            scroll(Material.NETHERITE_SWORD, RewardEvent.Item(Material.NETHERITE_SWORD)),
            scroll(Material.ENCHANTED_GOLDEN_APPLE, RewardEvent.Item(Material.ENCHANTED_GOLDEN_APPLE)),
            scroll(Material.GLISTERING_MELON_SLICE, RewardEvent.Heal(5.0)),
            // 惩罚
            scroll(Material.TNT, RewardEvent.Damage(10.0)),
            scroll(Material.WITHER_ROSE, RewardEvent.Effect(PotionEffectType.SLOWNESS, 10, 2)),
            scroll(Material.POISONOUS_POTATO, RewardEvent.Effect(PotionEffectType.BLINDNESS, 10, 1)),
            scroll(Material.SPONGE, RewardEvent.Effect(PotionEffectType.WEAKNESS, 15, 1)),
            scroll(Material.DEAD_BUSH, RewardEvent.Damage(6.0)),
        )

    }

    object ABYSS : SlotMachinePhase {

        override val borderMaterial: Material = Material.RED_STAINED_GLASS_PANE

        override val defaultPunishments: List<RewardEvent> = listOf(
            RewardEvent.Damage(10.0),
            RewardEvent.Effect(PotionEffectType.WITHER, 5, 1),
            RewardEvent.Effect(PotionEffectType.POISON, 5, 1)
        )

        override val scrollItems: List<ScrollItem> = listOf(
            // 给点希望
            scroll(Material.TOTEM_OF_UNDYING, RewardEvent.Special(RewardEvent.Special.Action.THUNDER_ALL)),
            // 给俩附魔金  一个用来装糖 一个用来搞反转
            scroll(Material.ENCHANTED_GOLDEN_APPLE, RewardEvent.Item(Material.ENCHANTED_GOLDEN_APPLE, count = 2)),
            // 大的来咯
            scroll(Material.TNT, RewardEvent.Damage(19.5, clearArmor = true)),
            scroll(Material.CREEPER_HEAD, RewardEvent.Special(RewardEvent.Special.Action.SPAWN_CREEPER)),
            scroll(Material.WITHER_SKELETON_SKULL, RewardEvent.Effect(PotionEffectType.WITHER, 10, 2)),
            scroll(Material.POISONOUS_POTATO, RewardEvent.Effect(PotionEffectType.POISON, 8, 2)),
            scroll(Material.DEAD_BUSH, RewardEvent.Damage(18.0, clearArmor = true)),
            scroll(Material.COARSE_DIRT, RewardEvent.Effect(PotionEffectType.SLOWNESS, 20, 3)),
            scroll(Material.SPIDER_EYE, RewardEvent.Effect(PotionEffectType.BLINDNESS, 15, 2)),
        )

    }

    fun scroll(material: Material, event: RewardEvent): ScrollItem {
        return ScrollItem(
            displayItem = buildItem(
                material = material,
                meta = { customName(event.name) }
            ),
            event = event
        )
    }

    data class ScrollItem(
        val displayItem: ItemStack,
        val event: RewardEvent
    )

}