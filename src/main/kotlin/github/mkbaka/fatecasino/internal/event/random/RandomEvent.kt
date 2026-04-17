package github.mkbaka.fatecasino.internal.event.random

import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.Component

interface RandomEvent {

    val displayName: Component

    val phase: GamePhase

    val weight: Int

    suspend fun execute(context: GameContext, scope: CoroutineScope)

}