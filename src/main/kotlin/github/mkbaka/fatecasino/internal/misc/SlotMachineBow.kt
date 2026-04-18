package github.mkbaka.fatecasino.internal.misc

import github.mkbaka.fatecasino.FateCasino
import github.mkbaka.fatecasino.internal.util.buildItem
import github.mkbaka.fatecasino.internal.util.on
import github.mkbaka.fatecasino.internal.util.sendMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Arrow
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.entity.IronGolem
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.FireworkMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

object SlotMachineBow {

    private val NAMESPACE = NamespacedKey(FateCasino.INSTANCE, "slot_machine_bow")
    private val ARROW_NAMESPACE = NamespacedKey(FateCasino.INSTANCE, "slot_machine_arrow")

    val name: Component
        get() = Component.text("老虎机之弓").color(TextColor.color(255, 200, 100))

    private val descriptions: List<Component>
        get() = listOf(
            Component.empty(),

            Component.empty()
                .append(Component.text(" ▸ ").color(NamedTextColor.GREEN))
                .append(Component.text("25%").color(NamedTextColor.YELLOW))
                .append(Component.text(" 烟花庆祝").color(TextColor.color(255, 180, 100))),

            Component.empty()
                .append(Component.text(" ▸ ").color(NamedTextColor.RED))
                .append(Component.text("25%").color(NamedTextColor.YELLOW))
                .append(Component.text(" 愤怒铁傀儡").color(TextColor.color(220, 80, 80))),

            Component.empty()
                .append(Component.text(" ▸ ").color(NamedTextColor.RED))
                .append(Component.text("25%").color(NamedTextColor.YELLOW))
                .append(Component.text(" TNT爆炸").color(TextColor.color(255, 120, 60))),

            Component.empty()
                .append(Component.text(" ▸ ").color(NamedTextColor.AQUA))
                .append(Component.text("25%").color(NamedTextColor.YELLOW))
                .append(Component.text(" 范围治疗").color(TextColor.color(100, 220, 150))),

            Component.empty(),

            Component.text(" ※ 每一箭都是一场未知的豪赌").color(TextColor.color(100, 80, 140)),
            Component.text("    你敢射出这一箭吗?").color(TextColor.color(100, 80, 140))
        )

    val getter: ItemStack
        get() = buildItem(
            material = Material.BOW,
            meta = {
                customName(name)
                lore(descriptions)

                addEnchant(Enchantment.INFINITY, 1, true)
                addItemFlags(ItemFlag.HIDE_ENCHANTS)

                persistentDataContainer[NAMESPACE, PersistentDataType.BOOLEAN] = true
            }
        )

    fun isSlotMachineBow(item: ItemStack): Boolean =
        item.itemMeta?.persistentDataContainer?.get(NAMESPACE, PersistentDataType.BOOLEAN) == true

    fun subscribe(listener: Listener) {
        on<ProjectileLaunchEvent>(listener = listener) {
            val arrow = entity as? Arrow ?: return@on
            val shooter = arrow.shooter as? Player ?: return@on

            if (!isSlotMachineBow(shooter.inventory.itemInMainHand)) return@on

            // 标记箭矢为老虎机箭矢 并记录来源
            arrow.persistentDataContainer[ARROW_NAMESPACE, PersistentDataType.BOOLEAN] = true
        }

        // 箭矢命中
        on<ProjectileHitEvent>(listener = listener) {
            val arrow = entity as? Arrow ?: return@on

            // 是否为老虎机箭矢
            if (arrow.persistentDataContainer.get(ARROW_NAMESPACE, PersistentDataType.BOOLEAN) != true) return@on

            val shooter = arrow.shooter as? Player ?: return@on
            val location = arrow.location.clone()

            // 移除箭矢
            arrow.remove()

            // 执行抽奖
            val random = ThreadLocalRandom.current()
            val result = random.nextInt(4)

            when (result) {
                0 -> spawnFirework(location, shooter)
                1 -> spawnAngryIronGolem(location, shooter)
                2 -> spawnPrimedTNT(location, shooter)
                3 -> spawnHealingArea(location, shooter)
            }
        }
    }

    private fun spawnFirework(location: Location, shooter: Player) {
        val colors = listOf(
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
            Color.PURPLE, Color.AQUA, Color.FUCHSIA, Color.LIME
        )
        val random = ThreadLocalRandom.current()

        // 发射多束烟花
        repeat(3) {
            val offsetLocation = location.clone().add(
                random.nextDouble(-1.0, 1.0),
                0.5,
                random.nextDouble(-1.0, 1.0)
            )

            val firework = location.world.spawn(offsetLocation, Firework::class.java)
            val meta = firework.fireworkMeta

            meta.addEffect(
                org.bukkit.FireworkEffect.builder()
                    .with(org.bukkit.FireworkEffect.Type.entries.random())
                    .withColor(colors.random())
                    .withFade(colors.random())
                    .trail(true)
                    .flicker(random.nextBoolean())
                    .build()
            )
            meta.power = random.nextInt(1, 3)

            firework.fireworkMeta = meta
        }

        location.world.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.0f, 1.0f)
    }

    private fun spawnAngryIronGolem(location: Location, shooter: Player) {
        val golem = location.world.spawn(location, IronGolem::class.java) { entity ->
            entity.customName(
                Component.empty()
                    .append(Component.text("xxx").color(NamedTextColor.GRAY).decorate(TextDecoration.OBFUSCATED))
                    .append(Component.text("愤怒的铁傀儡").color(NamedTextColor.RED))
                    .append(Component.text("xxx").color(NamedTextColor.GRAY).decorate(TextDecoration.OBFUSCATED))
            )
            entity.isCustomNameVisible = true
        }

        // 让铁傀儡去打最近的实体
        golem.target = location.getNearbyLivingEntities(5.0).first()

        location.world.playSound(location, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, 1.0f, 0.5f)
        location.world.spawnParticle(Particle.ANGRY_VILLAGER, location, 10, 1.0, 1.0, 1.0)
    }

    private fun spawnPrimedTNT(location: Location, shooter: Player) {
        location.world.spawn(location.clone().add(0.0, 0.5, 0.0), TNTPrimed::class.java) { entity ->
            entity.fuseTicks = 40 // 2秒后爆炸
            entity.source = shooter
        }

        location.world.playSound(location, Sound.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 1.0f, 1.0f)
    }

    private fun spawnHealingArea(location: Location, shooter: Player) {
        // 生成治疗粒子效果
        location.world.spawnParticle(
            Particle.HEART,
            location.clone().add(0.0, 1.0, 0.0),
            20,
            2.0, 1.0, 2.0
        )

        // 治疗范围内的所有玩家
        val healRange = 5.0
        val healAmount = 10.0 // 5颗心

        location.world.getNearbyEntities(location, healRange, healRange, healRange)
            .filterIsInstance<Player>()
            .forEach { player ->
                val maxHealth = player.maxHealth
                val newHealth = (player.health + healAmount).coerceAtMost(maxHealth)
                player.health = newHealth

                // 给予生命恢复效果
                player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 1))
            }

        location.world.playSound(location, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 1.5f)
    }
}