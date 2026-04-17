package github.mkbaka.fatecasino.internal.phase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable.isCancelled
import kotlinx.coroutines.launch

class PhaseRunner(
    private val scope: CoroutineScope
) {

    /** 当前阶段 */
    var currentPhase: AbstractGamePhase? = null
        private set

    /**
     * @param [phase] 起始阶段
     * @param [onComplete] 完成回调
     */
    fun start(phase: AbstractGamePhase, onComplete: () -> Unit) {
        scope.launch {
            runChain(phase)
            onComplete()
        }
    }

    /**
     * 执行阶段
     * @param [startPhase] 起始阶段
     */
    private suspend fun runChain(startPhase: AbstractGamePhase) {
        var phase: AbstractGamePhase? = startPhase
        while (phase != null) {
            currentPhase = phase
            // 返回 null 代表游戏结束
            phase = phase.execute()
        }
        currentPhase = null
    }

}