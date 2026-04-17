package github.mkbaka.fatecasino.internal.phase.manager

import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.phase.data.GameConfig.ShrinkPhase
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Sound
import org.bukkit.SoundCategory

/**
 * 边界缩圈管理器
 * 基于 [ShrinkPhase] 列表控制缩圈时机和速度
 */
class BorderManager(
    private val context: GameContext,
    private val scope: CoroutineScope,
) {

    private var shrinkJob: Job? = null

    // 当前边界大小
    private var currentSize: Double = context.config.lobby.borderSize

    // 已触发的缩圈阶段索引
    private var triggeredPhaseIndex: Int = -1

    // 和平时间结束的时间点
    private var peaceEndTimeMS: Long = 0

    fun start() {
        val shrinkPhases = context.config.playing.shrinkPhases
        if (shrinkPhases.isEmpty()) return

        val now = System.currentTimeMillis()
        peaceEndTimeMS = now + context.config.playing.peaceTimeMS

        shrinkJob = scope.launch {
            // 等待和平时间结束
            delay(context.config.playing.peaceTimeMS)

            while (isActive) {
                delay(1000L)

                // 计算从和平时间结束后的 elapsed 时间
                val elapsed = System.currentTimeMillis() - peaceEndTimeMS

                // 检查是否有未触发的阶段需要触发
                val nextPhaseIndex = triggeredPhaseIndex + 1
                if (nextPhaseIndex < shrinkPhases.size) {
                    val nextPhase = shrinkPhases[nextPhaseIndex]
                    if (elapsed >= nextPhase.triggerTimeMS) {
                        triggeredPhaseIndex = nextPhaseIndex
                        shrinkBorder(nextPhase)
                    }
                }

                // 所有阶段已触发且已达最小边界，停止循环
                if (triggeredPhaseIndex >= shrinkPhases.size - 1 && currentSize <= context.config.playing.borderMinSize) {
                    break
                }
            }
        }
    }

    fun stop() {
        shrinkJob?.cancel()
    }

    private suspend fun shrinkBorder(phase: ShrinkPhase) {
        val config = context.config.playing

        val targetSize = (currentSize * config.borderShrinkRatio)
            .coerceAtLeast(config.borderMinSize)

        val distance = currentSize - targetSize
        currentSize = targetSize

        scope.callSync {
            context.world.worldBorder.changeSize(targetSize, phase.shrinkDurationSeconds * 20L)
        }

        broadcastShrink(distance, phase.shrinkDurationSeconds)
    }

    private fun broadcastShrink(distance: Double, durationSeconds: Long) {
        Component.empty()
            .append(Component.text("边界将在 ").color(NamedTextColor.WHITE))
            .append(Component.text(durationSeconds).color(NamedTextColor.GREEN))
            .append(Component.text(" 秒内收缩 ").color(NamedTextColor.WHITE))
            .append(Component.text("%.0f".format(distance)).color(NamedTextColor.GREEN))
            .append(Component.text(" 格").color(NamedTextColor.WHITE))
            .broadcast()

        Sound.BLOCK_BELL_USE.broadcast(SoundCategory.MASTER, 1.0f, 0.8f)
    }

}