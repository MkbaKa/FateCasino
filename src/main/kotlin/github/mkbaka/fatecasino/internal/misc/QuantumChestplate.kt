package github.mkbaka.fatecasino.internal.misc

import github.mkbaka.fatecasino.FateCasino
import github.mkbaka.fatecasino.internal.util.buildItem
import github.mkbaka.fatecasino.internal.util.on
import github.mkbaka.fatecasino.internal.util.sendMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import java.util.concurrent.ThreadLocalRandom

object QuantumChestplate {

    private val NAMESPACE = NamespacedKey(FateCasino.INSTANCE, "quantum_chestplate_v2")

    val name: Component
        get() = Component.text("量子胸甲").color(TextColor.color(180, 220, 255))

    private val descriptions: List<Component>
        get() = listOf(
            Component.empty(),

            Component.empty()
                .append(Component.text(" ▸ ").color(NamedTextColor.GREEN))
                .append(Component.text("80%").color(NamedTextColor.YELLOW))
                .append(Component.text(" 免疫致命伤害并传送").color(NamedTextColor.GREEN)),

            Component.empty()
                .append(Component.text(" ▸ ").color(NamedTextColor.RED))
                .append(Component.text("5%").color(NamedTextColor.YELLOW))
                .append(Component.text(" 轻微伤害时碎裂炸飞").color(NamedTextColor.RED)),

            Component.empty(),

            Component.text(" ※ 生与死的界限，由概率决定").color(TextColor.color(100, 80, 140)),
            Component.text("    你敢穿上它吗?").color(TextColor.color(100, 80, 140))
        )

    val getter: ItemStack
        get() = buildItem(
            material = Material.LEATHER_CHESTPLATE,
            meta = {
                customName(name)
                lore(descriptions)

                addEnchant(Enchantment.INFINITY, 1, true)
                addItemFlags(ItemFlag.HIDE_ENCHANTS)

                persistentDataContainer[NAMESPACE, PersistentDataType.BOOLEAN] = true
            }
        )

    fun isQuantumChestplate(item: ItemStack?): Boolean {
        if (item == null || item.type == Material.AIR) return false
        return item.itemMeta?.persistentDataContainer
            ?.get(NAMESPACE, PersistentDataType.BOOLEAN) == true
    }

    fun subscribe(listener: Listener) {
        on<EntityDamageEvent>(listener = listener) {
            val player = entity as? Player ?: return@on

            val chestplate = player.inventory.chestplate
            if (!isQuantumChestplate(chestplate)) return@on

            val currentHealth = player.health
            val damage = finalDamage
            val random = ThreadLocalRandom.current()

            val isFatalDamage = currentHealth - damage <= 0

            if (isFatalDamage) {
                if (random.nextDouble() < 0.8) {
                    // 80%: 忽略死亡并随机传送
                    isCancelled = true
                    player.health = 1.0

                    randomTeleport(player)

                    player.world.playSound(
                        player.location,
                        Sound.ENTITY_ENDERMAN_TELEPORT,
                        SoundCategory.PLAYERS,
                        1.0f,
                        1.0f
                    )

                    Component.empty()
                        .append(Component.text("量子跃迁! ").color(NamedTextColor.GOLD))
                        .append(Component.text("你逃离了死亡...").color(NamedTextColor.GREEN))
                        .sendMessage(player)
                } else {
                    // 20%: 装备碎裂并炸飞
                    player.inventory.chestplate = null
                    blastOff(player, power = 5.0)

                    // 创建爆炸效果但不造成伤害
                    player.world.createExplosion(player.location, 0f, false, false)

                    Component.empty()
                        .append(Component.text("量子崩塌! ").color(NamedTextColor.RED))
                        .append(Component.text("胸甲碎裂并将你炸上了天!").color(NamedTextColor.YELLOW))
                        .sendMessage(player)
                }
            } else {
                // 轻微伤害: 5% 概率碎裂
                if (random.nextDouble() < 0.05) {
                    player.inventory.chestplate = null
                    blastOff(player, power = 3.0)

                    Component.empty()
                        .append(Component.text("量子不稳定! ").color(NamedTextColor.RED))
                        .append(Component.text("胸甲意外碎裂并炸飞了你!").color(NamedTextColor.YELLOW))
                        .sendMessage(player)
                }
            }
        }
    }

    private fun randomTeleport(player: Player) {
        val world = player.world
        val random = ThreadLocalRandom.current()

        val randomX = random.nextInt(-500, 500)
        val randomZ = random.nextInt(-500, 500)

        val y = world.getHighestBlockYAt(randomX, randomZ) + 1

        val targetLocation = Location(world, randomX.toDouble(), y.toDouble(), randomZ.toDouble())

        player.teleport(targetLocation)
        player.world.playSound(
            targetLocation,
            Sound.ENTITY_ENDERMAN_TELEPORT,
            SoundCategory.PLAYERS,
            1.0f,
            1.0f
        )
    }

    private fun blastOff(player: Player, power: Double = 3.0) {
        player.world.playSound(
            player.location,
            Sound.ENTITY_GENERIC_EXPLODE,
            SoundCategory.PLAYERS,
            1.0f,
            1.0f
        )

        player.velocity = Vector(0.0, power, 0.0)
    }
}