package github.mkbaka.fatecasino.internal.util

import github.mkbaka.fatecasino.internal.game.GameManager
import github.mkbaka.fatecasino.internal.phase.data.PlayerSession
import org.bukkit.entity.Player

val Player.session: PlayerSession?
    get() = GameManager.currentPhase?.context?.playerSessions[uniqueId]

val currentSessions: Collection<PlayerSession>
    get() = GameManager.currentPhase?.context?.playerSessions?.values ?: emptyList()
