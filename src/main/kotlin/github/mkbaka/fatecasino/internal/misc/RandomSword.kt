package github.mkbaka.fatecasino.internal.misc

import github.mkbaka.fatecasino.FateCasino
import github.mkbaka.fatecasino.internal.util.buildItem
import github.mkbaka.fatecasino.internal.util.on
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.ThreadLocalRandom

object RandomSword {

    private val NAMESPACE = NamespacedKey(FateCasino.INSTANCE, "fate_sword")

    val name: Component
        get() = Component.text("命运之刃").color(TextColor.color(255, 180, 50))

    private val descriptions: List<Component>
        get() = listOf(
            Component.empty(),

            Component.empty()
                .append(Component.text(" ▸ ").color(NamedTextColor.GREEN))
                .append(Component.text("50%").color(NamedTextColor.YELLOW))
                .append(Component.text(" 概率秒杀敌人").color(NamedTextColor.GREEN)),

            Component.empty()
                .append(Component.text(" ▸ ").color(NamedTextColor.RED))
                .append(Component.text("50%").color(NamedTextColor.YELLOW))
                .append(Component.text(" 概率秒杀自己").color(NamedTextColor.RED)),

            Component.empty(),

            Component.text(" ※ 每一击都是命运的赌局").color(TextColor.color(100, 80, 140)),
            Component.text("    你敢挥下这一剑吗?").color(TextColor.color(100, 80, 140))
        )

    val getter: ItemStack
        get() = buildItem(
            material = Material.GOLDEN_SWORD,
            meta = {
                this as Damageable
                damage = Material.GOLDEN_SWORD.maxDurability - 1

                customName(name)
                lore(descriptions)

                addEnchant(Enchantment.INFINITY, 1, true)
                addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)

                persistentDataContainer[NAMESPACE, PersistentDataType.BOOLEAN] = true
            }
        )

    fun isFateSword(item: ItemStack): Boolean =
        item.itemMeta?.persistentDataContainer
            ?.get(NAMESPACE, PersistentDataType.BOOLEAN) == true

    fun subscribe(listener: Listener) {
        on<EntityDamageByEntityEvent>(listener = listener) {
            val attacker = damager as? Player ?: return@on

            if (!isFateSword(attacker.inventory.itemInMainHand)) return@on

            val target = entity as? Player ?: return@on

            if (ThreadLocalRandom.current().nextBoolean()) {
                // 秒杀敌人
                target.health = 0.0
                attacker.world.playSound(
                    attacker.location,
                    Sound.ENTITY_PLAYER_ATTACK_CRIT,
                    SoundCategory.PLAYERS,
                    1.0f,
                    1.0f
                )
                attacker.world.playSound(
                    target.location,
                    Sound.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.PLAYERS,
                    0.5f,
                    1.0f
                )
            } else {
                // 秒杀自己
                attacker.health = 0.0
                attacker.world.playSound(attacker.location, Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 0.5f)
                attacker.world.playSound(
                    attacker.location,
                    Sound.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.PLAYERS,
                    0.5f,
                    1.0f
                )
            }
        }
    }

}