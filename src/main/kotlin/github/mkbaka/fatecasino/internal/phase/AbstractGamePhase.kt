package github.mkbaka.fatecasino.internal.phase

import github.mkbaka.fatecasino.internal.phase.data.GameContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

abstract class AbstractGamePhase(
    val context: GameContext,
    val scope: CoroutineScope
) {

    abstract val name: String

    /**
     * 执行阶段
     */
    suspend fun execute(): AbstractGamePhase? {
        onStart()
        try {
            return runPhase()
        } finally {
            onEnd()
        }
    }

    /**
     * 阶段开始
     */
    protected open suspend fun onStart() {}

    /**
     * 阶段主逻辑
     * @return 下一个阶段 若为 null 代表游戏结束
     */
    protected abstract suspend fun runPhase(): AbstractGamePhase?

    /**
     * 阶段结束
     */
    protected open suspend fun onEnd() {}

    /**
     * 等待条件满足
     *
     * @param [interval] 间隔 (ms)
     * @param [condition] 条件
     */
    protected suspend fun waitFor(interval: Long, condition: () -> Boolean) {
        while (scope.isActive && !condition()) {
            delay(interval)
        }
    }

}