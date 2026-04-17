package github.mkbaka.fatecasino.internal.phase.data

import org.bukkit.Location
import org.bukkit.World
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class GameContext(
    val world: World,
    val config: GameConfig,
    val currentSession: GameSession,
    val playerSessions: MutableMap<UUID, PlayerSession> = ConcurrentHashMap(),
) {

    var lobbyLocation: Location? = null

    /** 通货膨胀 价格倍率 默认 1 */
    var priceMultiplier: Int = 1

    /** 零元购 价格覆写 null 表示不覆写 */
    var priceOverride: Int? = null

    /** 悬赏通缉令 被悬赏的玩家 UUID */
    var bountyTargetId: UUID? = null

}