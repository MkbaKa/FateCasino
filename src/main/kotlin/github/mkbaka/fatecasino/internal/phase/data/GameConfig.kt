package github.mkbaka.fatecasino.internal.phase.data

import org.bukkit.Difficulty
import org.bukkit.Material

data class GameConfig(
    // 大厅配置
    val lobby: LobbyConfig = LobbyConfig(),
    // 等待阶段配置
    val wait: WaitConfig = WaitConfig(),
    // 游玩阶段配置
    val playing: PlayingConfig = PlayingConfig(),
) {

    data class LobbyConfig(
        // 平台材质
        val platform: Material = Material.BARRIER,
        // 平台半径
        val platformRadius: Int = 15,
        // 墙面材质
        val wall: Material = Material.BARRIER,
        // 墙面高度
        val wallHeight: Int = 5,
        // 平台位置相对于大厅坐标的偏移量
        // 防止玩家半截身子卡平台里 cos 安德森上校
        val platformYOffset: Int = -2,

        // 边界范围
        val borderSize: Double = 500.0
    )

    data class WaitConfig(
        // 500ms 内的下蹲判断为双击
        val doubleClickWindowMS: Long = 500,
        // 至少游玩人数
        val minPlayers: Int = 2,
        // 满足游玩人数后 等待多少秒开始游戏
        val waitForPlayers: Int = 10
    )

    data class PlayingConfig(
        // 需要判断胜利者时 如果存在离线的玩家 等待重连的时间
        val offlineWaitSeconds: Int = 5,
        // 只剩一名玩家存活时 延迟判断最后赢家
        // 这样同归于尽就不会有一个人胜出了
        val confirmDelayMS: Long = 1000,
        // 和平时间
        // 该时间内游戏为和平模式 且不会自然获得命运券
        val peaceTimeMS: Long = 30_000,
        //  和平时间结束后的游戏难度
        val difficulty: Difficulty = Difficulty.NORMAL,
        // 命运券上限
        val maxTicket: Int = 10,

        // 缩圈配置
        // 每次缩小的比例
        val borderShrinkRatio: Double = 0.7,
        // 缩圈阶段列表 (从和平时间结束后开始计时)
        val shrinkPhases: List<ShrinkPhase> = listOf(
            // 2分钟时触发第一次缩圈
            ShrinkPhase(120_000, shrinkDurationSeconds = 60),
            // 4分钟时触发
            ShrinkPhase(240_000, shrinkDurationSeconds = 45),
            // 6分钟时触发
            ShrinkPhase(360_000, shrinkDurationSeconds = 40),
            // 8分钟时触发
            ShrinkPhase(480_000, shrinkDurationSeconds = 35),
            // 10分钟时触发
            ShrinkPhase(600_000, shrinkDurationSeconds = 30),
        ),
        // 最小边界大小
        val borderMinSize: Double = 50.0
    )

    /**
     * 缩圈阶段
     * @param triggerTimeMS 从和平时间结束后触发缩圈的时间点
     * @param shrinkDurationSeconds 本次缩圈耗时 (秒)
     */
    data class ShrinkPhase(
        val triggerTimeMS: Long,
        val shrinkDurationSeconds: Long,
    )

}