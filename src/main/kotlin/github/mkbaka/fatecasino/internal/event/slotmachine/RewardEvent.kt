package github.mkbaka.fatecasino.internal.event.slotmachine

import github.mkbaka.fatecasino.internal.util.buildItem
import github.mkbaka.fatecasino.internal.util.giveItem
import github.mkbaka.fatecasino.internal.util.session
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Creeper
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

sealed class RewardEvent {

    abstract val name: Component
    abstract fun execute(player: Player, level: RewardLevel, phase: SlotMachinePhase)

    /**
     * 命运券奖励
     */
    data class Ticket(val amount: Int) : RewardEvent() {
        override val name: Component = Component.empty()
            .append(Component.text("命运券 ").color(NamedTextColor.GOLD))
            .append(Component.text("x"))
            .append(Component.text(amount))

        override fun execute(player: Player, level: RewardLevel, phase: SlotMachinePhase) {
            val multiplier = when (level) {
                RewardLevel.JACKPOT -> 3
                RewardLevel.WIN -> 1
                RewardLevel.LOSE -> 0
            }

            val ticket = amount * multiplier
            if (ticket == 0) return

            player.session!!.giveTicket(ticket)
        }
    }

    /**
     * 物品奖励
     */
    data class SimpleItem(
        val material: Material,
        val count: Int = 1,
        val enchantments: Map<Enchantment, Int> = emptyMap()
    ) : RewardEvent() {
        override val name: Component = Component.empty()
            .append(Component.translatable(material.translationKey()).color(NamedTextColor.YELLOW))
            .append(Component.text(" x"))
            .append(Component.text(count))

        override fun execute(player: Player, level: RewardLevel, phase: SlotMachinePhase) {
            val multiplier = when (level) {
                RewardLevel.JACKPOT -> 2
                RewardLevel.WIN -> 1
                RewardLevel.LOSE -> 1
            }
            val stack = buildItem(
                material = material,
                stack = { amount = count * multiplier },
                meta = {
                    enchantments.forEach { (ench, level) ->
                        addEnchant(ench, level * multiplier, true)
                    }
                }
            )
            player.giveItem(stack)
        }
    }

    /**
     * 高级物品
     */
    data class PowerfulItem(
        val nameSupplier: () -> Component,
        val itemSupplier: () -> ItemStack
    ) : RewardEvent() {
        override val name: Component
            get() = nameSupplier()
        override fun execute(
            player: Player,
            level: RewardLevel,
            phase: SlotMachinePhase
        ) {
            val item = itemSupplier()
            player.giveItem(item)
        }

    }

    /**
     * 治疗
     */
    data class Heal(val hearts: Double) : RewardEvent() {
        override val name: Component = Component.empty()
            .append(Component.text("治疗 ").color(NamedTextColor.RED))
            .append(Component.text(hearts))
            .append(Component.text("❤"))

        override fun execute(player: Player, level: RewardLevel, phase: SlotMachinePhase) {
            val multiplier = when (level) {
                RewardLevel.JACKPOT -> 2.0
                RewardLevel.WIN -> 1.5
                RewardLevel.LOSE -> 1.0
            }
            val maxHealth = player.maxHealth
            val newHealth = (player.health + hearts * multiplier).coerceAtMost(maxHealth)
            player.health = newHealth
        }
    }

    /**
     * 药水效果
     */
    data class Effect(
        val type: PotionEffectType,
        val durationSeconds: Int,
        val amplifier: Int
    ) : RewardEvent() {
        override val name: Component = Component.empty()
            .append(Component.translatable(type.translationKey()).color(NamedTextColor.AQUA))
            .append(Component.text(" "))
            .append(Component.text(durationSeconds))
            .append(Component.text("s"))

        override fun execute(player: Player, level: RewardLevel, phase: SlotMachinePhase) {
            val multiplier = when (level) {
                RewardLevel.JACKPOT -> 2
                RewardLevel.WIN -> 1
                RewardLevel.LOSE -> 1
            }
            player.addPotionEffect(
                PotionEffect(type, durationSeconds * 20 * multiplier, amplifier)
            )
        }
    }

    /**
     * 伤害惩罚
     */
    data class Damage(
        val hearts: Double,
        val clearArmor: Boolean = false
    ) : RewardEvent() {
        override val name: Component = Component.empty()
            .append(Component.text("-").color(NamedTextColor.DARK_RED))
            .append(Component.text(hearts))
            .append(Component.text("❤"))

        override fun execute(player: Player, level: RewardLevel, phase: SlotMachinePhase) {
            val multiplier = when (level) {
                // 大奖时惩罚减半
                RewardLevel.JACKPOT -> 0.5
                RewardLevel.WIN -> 1.0
                // 未中奖时惩罚加重
                RewardLevel.LOSE -> 1.5
            }
            val damage = hearts * multiplier
            player.damage(damage, DamageSource.builder(DamageType.GENERIC).build())

            if (clearArmor) {
                EquipmentSlot.entries
                    .filter { it != EquipmentSlot.HAND && it != EquipmentSlot.OFF_HAND }
                    .forEach { slot ->
                        player.inventory.setItem(slot, null)
                    }
            }
        }
    }

    /**
     * 特殊事件
     */
    data class Special(val action: Action) : RewardEvent() {
        enum class Action {
            // 全图雷击
            THUNDER_ALL,

            // 刷苦力怕
            SPAWN_CREEPER,

            // 诅咒神装
            CURSED_ARMOR
        }

        override val name: Component = when (action) {
            Action.THUNDER_ALL -> Component.empty()
                .append(Component.text("✦ ").color(NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("全图雷击").color(NamedTextColor.GOLD))
                .append(Component.text(" ✦").color(NamedTextColor.LIGHT_PURPLE))

            Action.SPAWN_CREEPER -> Component.text("你的贪婪...").color(NamedTextColor.DARK_GRAY)
            Action.CURSED_ARMOR -> Component.text("天选之子").color(NamedTextColor.DARK_PURPLE)
        }

        override fun execute(player: Player, level: RewardLevel, phase: SlotMachinePhase) {
            when (action) {
                Action.THUNDER_ALL -> {
                    // 给玩家不死图腾
                    player.giveItem(buildItem(Material.TOTEM_OF_UNDYING))
                    // 全图雷击
                    Bukkit.getOnlinePlayers()
                        .filter { it != player }
                        .forEach { target ->
                            target.world.strikeLightning(target.location)
                        }
                }

                Action.SPAWN_CREEPER -> {
                    player.world.spawn(
                        player.location,
                        Creeper::class.java
                    ) { creeper ->
                        creeper.customName(Component.text("你的贪婪").color(NamedTextColor.RED))
                        // 掐个瞬爆
                        creeper.isPowered = true
                    }
                }

                Action.CURSED_ARMOR -> {
                    // 直接穿在身上
                    val armorMap = mapOf(
                        Material.DIAMOND_HELMET to EquipmentSlot.HEAD,
                        Material.DIAMOND_CHESTPLATE to EquipmentSlot.CHEST,
                        Material.DIAMOND_LEGGINGS to EquipmentSlot.LEGS,
                        Material.DIAMOND_BOOTS to EquipmentSlot.FEET
                    )
                    for ((material, slot) in armorMap) {
                        val item = buildItem(
                            material = material,
                            meta = {
                                // 只能用一次
                                this as Damageable
                                damage = material.maxDurability - 1

                                // 随机附魔
                                RegistryAccess.registryAccess()
                                    .getRegistry(RegistryKey.ENCHANTMENT)
                                    .asSequence()
                                    .shuffled()
                                    .take(3)
                                    .forEach { ench ->
                                        addEnchant(ench, (1..4).random(), true)
                                    }
                            }
                        )
                        player.equipment.setItem(slot, item)
                    }
                }
            }
        }
    }
}