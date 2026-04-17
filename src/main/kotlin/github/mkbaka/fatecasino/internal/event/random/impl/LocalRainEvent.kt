package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.ServerThreadDispatcher
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import github.mkbaka.fatecasino.internal.util.countdownBossBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.EntityType
import org.bukkit.entity.FallingBlock
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.concurrent.ThreadLocalRandom

object LocalRainEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("局部降水").color(NamedTextColor.DARK_GRAY)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int = 2

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val active = context.playerSessions.values.filter { it.isActive }
        if (active.isEmpty()) return

        val target = active.random()
        val random = ThreadLocalRandom.current()

        scope.callSync {
            val player = target.playerOrNull ?: return@callSync
            // 给目标轻微移速加成 让追杀更有趣
            player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 20, 0))
        }

        Component.empty()
            .append(Component.text("局部降水降临!").color(NamedTextColor.DARK_GRAY))
            .append(Component.newline())
            .append(Component.text("   别再牵挂啦~ 家里下大啦~").color(NamedTextColor.GRAY))
            .broadcast()

        val durationSeconds = 20
        val endTime = System.currentTimeMillis() + durationSeconds * 1000L

        // BossBar 提示
        val bossBar = countdownBossBar(
            context = context,
            title = LegacyComponentSerializer.legacySection().serialize(displayName),
            durationSeconds = durationSeconds
        )
        bossBar.show()

        var lastDisplayedSeconds = durationSeconds

        while (scope.isActive && System.currentTimeMillis() < endTime) {
            // 当时间变化时更新 BossBar 进度
            val remainingSeconds = ((endTime - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            if (remainingSeconds != lastDisplayedSeconds) {
                bossBar.updateProgress(remainingSeconds)
                lastDisplayedSeconds = remainingSeconds
            }

            scope.callSync {
                val player = target.playerOrNull
                if (player != null && target.isActive) {
                    val loc = player.location.clone()

                    // 在头顶显示 angry_villager 粒子
                    repeat(3) {
                        player.world.spawnParticle(
                            Particle.ANGRY_VILLAGER,
                            loc.add(0.0, 2.0, 0.0),
                            3
                        )
                        delay(10L)
                    }

                    loc.subtract(0.0, 2.0, 0.0) // 恢复位置

                    // 50% 概率劈雷 / 掉铁砧
                    if (random.nextBoolean()) {
                        loc.world.strikeLightningEffect(loc)
                        //  一颗半心伤害
                        player.damage(3.0)
                    } else {
                        // 在半径 5 格范围内随机生成 8-15 个铁砧
                        val anvilCount = random.nextInt(8, 16)
                        repeat(anvilCount) {
                            val dx = random.nextDouble(-5.0, 5.0)
                            val dz = random.nextDouble(-5.0, 5.0)
                            val anvilLoc = loc.clone().add(dx, 5.0, dz)
                            loc.world.spawn(
                                anvilLoc,
                                FallingBlock::class.java
                            ) { entity ->
                                entity.blockData = Material.ANVIL.createBlockData()
                                entity.setHurtEntities(true)
                                entity.damagePerBlock = 1.0f
                            }
                        }
                    }
                }
            }

            delay(2000L)
        }

        withContext(NonCancellable + ServerThreadDispatcher) {
            bossBar.hide()
        }
    }

}