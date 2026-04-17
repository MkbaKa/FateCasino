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
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.Block
import java.util.concurrent.ThreadLocalRandom

object AbsoluteZeroEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("绝对零度").color(NamedTextColor.AQUA)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int = 2

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val isWaterVariant = ThreadLocalRandom.current().nextBoolean()
        val variantName = if (isWaterVariant) "绝对零度" else "熔岩地狱"
        val variantColor = if (isWaterVariant) NamedTextColor.AQUA else NamedTextColor.RED

        Component.empty()
            .append(Component.text(variantName).color(variantColor))
            .append(Component.newline())
            .append(Component.text("   脚下的世界正在改变...").color(NamedTextColor.GRAY))
            .broadcast()

        Sound.BLOCK_FIRE_EXTINGUISH.broadcast(SoundCategory.MASTER, 1.0f, if (isWaterVariant) 2.0f else 0.5f)

        val collectDurationSeconds = 5 // 踩踏收集阶段时长
        val collectEndTime = System.currentTimeMillis() + collectDurationSeconds * 1000L

        // BossBar 提示 - 显示踩踏收集阶段倒计时
        val bossBar = countdownBossBar(
            context = context,
            title = LegacyComponentSerializer.legacySection().serialize(displayName),
            durationSeconds = collectDurationSeconds
        )
        bossBar.show()

        // 记录每个方块的开始时间和类型
        val blockStates = mutableMapOf<Block, Pair<Long, Boolean>>() // block -> (startTime, isWaterVariant)

        var lastDisplayedSeconds = collectDurationSeconds

        // 阶段1: 踩踏收集阶段
        while (scope.isActive && System.currentTimeMillis() < collectEndTime) {
            val now = System.currentTimeMillis()

            // 当时间变化时更新 BossBar 进度
            val remainingSeconds = ((collectEndTime - now) / 1000).toInt().coerceAtLeast(0)
            if (remainingSeconds != lastDisplayedSeconds) {
                bossBar.updateProgress(remainingSeconds)
                lastDisplayedSeconds = remainingSeconds
            }

            scope.callSync {
                // 处理每个玩家脚下的方块
                for (session in context.playerSessions.values.filter { it.isActive }) {
                    val player = session.playerOrNull ?: continue
                    val loc = player.location.clone()
                    val blockBelow = loc.subtract(0.0, 1.0, 0.0).block

                    // 新方块：记录并设置为第一颜色
                    if (!blockStates.containsKey(blockBelow)) {
                        blockStates[blockBelow] = Pair(now, isWaterVariant)
                        val firstColor = if (isWaterVariant) Material.WHITE_WOOL else Material.YELLOW_WOOL
                        blockBelow.type = firstColor
                    }
                }

                // 更新所有方块状态
                for ((block, state) in blockStates) {
                    val (startTime, isWater) = state
                    val elapsed = now - startTime

                    if (elapsed in 2000..<3000) {
                        val secondColor = if (isWater) Material.BLUE_WOOL else Material.RED_WOOL
                        if (block.type != secondColor && block.type != Material.WATER && block.type != Material.LAVA) {
                            block.type = secondColor
                        }
                    } else if (elapsed >= 3000) {
                        val liquid = if (isWater) Material.WATER else Material.LAVA
                        if (block.type != liquid) {
                            block.type = liquid
                        }
                    }
                }
            }

            delay(50L)
        }

        // 隐藏 BossBar
        bossBar.hide()

        // 阶段2: 继续更新方块直到全部变成液体
        val transformDurationMs = 3000L // 方块变成液体需要3秒
        val latestBlockStartTime = blockStates.values.maxOfOrNull { it.first } ?: 0L
        val transformEndTime = latestBlockStartTime + transformDurationMs

        while (scope.isActive && System.currentTimeMillis() < transformEndTime) {
            val now = System.currentTimeMillis()

            scope.callSync {
                for ((block, state) in blockStates) {
                    val (startTime, isWater) = state
                    val elapsed = now - startTime

                    if (elapsed in 2000..<3000) {
                        val secondColor = if (isWater) Material.BLUE_WOOL else Material.RED_WOOL
                        if (block.type != secondColor && block.type != Material.WATER && block.type != Material.LAVA) {
                            block.type = secondColor
                        }
                    } else if (elapsed >= 3000) {
                        val liquid = if (isWater) Material.WATER else Material.LAVA
                        if (block.type != liquid) {
                            block.type = liquid
                        }
                    }
                }
            }

            delay(50L)
        }

        // 确保即使 scope 取消也能隐藏 BossBar
        withContext(NonCancellable + ServerThreadDispatcher) {
            bossBar.hide()
        }
    }

}