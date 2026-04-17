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
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.Block
import java.util.concurrent.ThreadLocalRandom

object AcidRainEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("酸雨侵蚀").color(NamedTextColor.DARK_GREEN)

    override val phase: GamePhase
        get() = GamePhase.LATE

    override val weight: Int = 2

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val world = context.world
        val random = ThreadLocalRandom.current()

        // 切换为雨天
        scope.callSync {
            world.setStorm(true)
            world.weatherDuration = 20 * 20
        }

        Component.empty()
            .append(Component.text("酸雨侵蚀").color(NamedTextColor.DARK_GREEN))
            .append(Component.newline())
            .append(Component.text("   整天浇地那老头终于放假啦...").color(NamedTextColor.GRAY))
            .broadcast()

        Sound.ENTITY_LIGHTNING_BOLT_THUNDER.broadcast(SoundCategory.MASTER, 1.0f, 0.5f)

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
            val remainingSeconds = ((endTime - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            if (remainingSeconds != lastDisplayedSeconds) {
                bossBar.updateProgress(remainingSeconds)
                lastDisplayedSeconds = remainingSeconds
            }

            // 腐蚀玩家附近的方块
            scope.callSync {
                val active = context.playerSessions.values.filter { it.isActive }
                for (session in active) {
                    val player = session.playerOrNull ?: continue
                    val loc = player.location

                    // 在玩家脚下产生酸雨粒子效果
                    player.world.spawnParticle(
                        Particle.FALLING_DRIPSTONE_WATER,
                        loc.clone().add(0.0, 1.0, 0.0),
                        20,
                        2.0,
                        0.0,
                        2.0
                    )

                    // 检查玩家头顶是否有方块遮蔽
                    val highestY = world.getHighestBlockYAt(loc.blockX, loc.blockZ)
                    val hasCover = highestY > loc.blockY

                    // 如果没有遮蔽 造成一颗心伤害
                    if (!hasCover) {
                        player.damage(2.0)
                    }

                    // 随机腐蚀附近地表方块
                    val erosionRadius = 3
                    for (dx in -erosionRadius..erosionRadius) {
                        for (dz in -erosionRadius..erosionRadius) {
                            if (random.nextDouble() > 0.3) continue // 30% 概率腐蚀

                            val blockX = loc.blockX + dx
                            val blockZ = loc.blockZ + dz
                            val blockY = world.getHighestBlockYAt(blockX, blockZ)

                            val block = world.getBlockAt(blockX, blockY, blockZ)
                            // 只腐蚀非 AIR, 非 BEDROCK, 非 CHEST 等重要方块
                            if (shouldErode(block)) {
                                block.type = Material.AIR
                            }
                        }
                    }
                }
            }

            delay(1000L)
        }

        withContext(NonCancellable + ServerThreadDispatcher) {
            bossBar.hide()
            world.setStorm(false)
        }

        Sound.ENTITY_LIGHTNING_BOLT_THUNDER.broadcast(SoundCategory.MASTER, 1.0f, 1.5f)
    }

    private fun shouldErode(block: Block): Boolean {
        val type = block.type
        return type != Material.AIR &&
               type != Material.BEDROCK &&
               type != Material.CHEST &&
               type != Material.TRAPPED_CHEST &&
               type != Material.BARRIER &&
               type != Material.WATER &&
               type != Material.LAVA &&
               !type.name.contains("SPAWN") // 不腐蚀刷怪笼等
    }

}