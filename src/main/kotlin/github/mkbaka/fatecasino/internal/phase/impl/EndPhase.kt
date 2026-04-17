package github.mkbaka.fatecasino.internal.phase.impl

import github.mkbaka.fatecasino.internal.game.GameWorldManager
import github.mkbaka.fatecasino.internal.phase.AbstractGamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.phase.data.GameSession
import github.mkbaka.fatecasino.internal.util.callSync
import github.mkbaka.fatecasino.internal.util.info
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.world
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

class EndPhase(
    context: GameContext,
    scope: CoroutineScope
) : AbstractGamePhase(context, scope) {

    override val name: String
        get() = "游戏结束"

    override suspend fun runPhase(): AbstractGamePhase {
        info("本轮游戏结束... 尝试进入下一轮游戏...")

        val world = scope.async {
            callSync { GameWorldManager.getGameWorld() }
        }.await()

        return InitPhase(
            context.copy(
                world = world,
                config = context.config,
                currentSession = GameSession()
            ),
            scope
        ) {
            scope.callSync { GameWorldManager.deleteGameWorld(context.world) }
        }
    }

    override suspend fun onEnd() {
        info("结束阶段完成, 准备下一轮游戏")
    }

}