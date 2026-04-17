package github.mkbaka.fatecasino.internal.game

import github.mkbaka.fatecasino.internal.phase.AbstractGamePhase
import github.mkbaka.fatecasino.internal.phase.PhaseRunner
import github.mkbaka.fatecasino.internal.phase.data.GameConfig
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.phase.data.GameSession
import github.mkbaka.fatecasino.internal.phase.impl.InitPhase
import github.mkbaka.fatecasino.internal.util.BukkitCoroutineScope
import github.mkbaka.fatecasino.internal.util.CountdownBossBar.Companion.clearAllBossBars
import github.mkbaka.fatecasino.internal.util.info
import github.mkbaka.fatecasino.internal.util.resetAll

object GameManager {

    private val scope = BukkitCoroutineScope()

    private val runner = PhaseRunner(scope)

    private var isRunning = false

    val currentPhase: AbstractGamePhase?
        get() = runner.currentPhase

    fun start(config: GameConfig = GameConfig()) {
        if (isRunning) {
            error("游戏已经在运行了")
        }

        isRunning = true
        info("正在启动游戏...")

        val world = GameWorldManager.getGameWorld()
        val context = GameContext(world, config, GameSession())
        runner.start(
            phase = InitPhase(context, scope),
            onComplete = {
                info("游戏结束")
            }
        )
    }

    fun stop() {
        if (!isRunning) return

        // 防止游戏过程中关闭服务器 导致上次游玩数据保留
        resetAll()

        clearAllBossBars()

        scope.cancelAndShutdown()

        isRunning = false

        GameWorldManager.cleanup()

        info("游戏已停止")
    }

}