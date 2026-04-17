package github.mkbaka.fatecasino.internal.util

import github.mkbaka.fatecasino.FateCasino
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player
import java.util.UUID

class CountdownBossBar(
    val key: NamespacedKey,
    private val titleProvider: () -> String,
    private val playersProvider: () -> List<Player>,
    private val durationSeconds: Int,
    private val colorStrategy: (Int) -> BarColor = defaultColorStrategy(),
    private val onTick: (suspend (remainingSeconds: Int) -> Unit)? = null
) {

    private val bossBar = Bukkit.createBossBar(key, titleProvider(), BarColor.GREEN, BarStyle.SOLID)

    /**
     * 在协程 scope 中运行 自动取消并移除
     */
    suspend fun run(scope: CoroutineScope) {
        show()

        try {
            for (remaining in durationSeconds downTo 1) {
                if (!scope.isActive) break

                // 更新进度和颜色
                bossBar.progress = remaining / durationSeconds.toDouble()
                bossBar.color = colorStrategy(remaining)
                bossBar.setTitle(titleProvider())

                // 回调
                onTick?.invoke(remaining)

                delay(1000L)
            }
        } finally {
            hide()
        }
    }

    /**
     * 被动模式: 显示 bossbar 由外部逻辑控制更新
     */
    fun show() {
        activeKeys.add(key)
        bossBar.setTitle(titleProvider())
        bossBar.color = colorStrategy(durationSeconds)
        bossBar.progress = 1.0

        for (player in playersProvider()) {
            bossBar.addPlayer(player)
        }
    }

    /**
     * 被动模式: 更新进度
     */
    fun updateProgress(remainingSeconds: Int) {
        bossBar.progress = remainingSeconds / durationSeconds.toDouble()
        bossBar.color = colorStrategy(remainingSeconds)
        bossBar.setTitle(titleProvider())

        // 更新玩家列表 可能有玩家死亡或退出
        val currentPlayers = bossBar.players.toList()
        val newPlayers = playersProvider()

        for (player in currentPlayers) {
            if (player !in newPlayers) {
                bossBar.removePlayer(player)
            }
        }
        for (player in newPlayers) {
            if (player !in currentPlayers) {
                bossBar.addPlayer(player)
            }
        }
    }

    /**
     * 被动模式: 移除 bossbar
     */
    fun hide() {
        bossBar.removeAll()
        Bukkit.removeBossBar(key)
        activeKeys.remove(key)
    }

    companion object {
        private val activeKeys = mutableSetOf<NamespacedKey>()

        /**
         * 默认颜色策略: 绿→黄→红
         */
        fun defaultColorStrategy(): (Int) -> BarColor = { remaining ->
            when {
                remaining >= 10 -> BarColor.GREEN
                remaining >= 5 -> BarColor.YELLOW
                else -> BarColor.RED
            }
        }

        /**
         * 单色策略: 始终使用指定颜色
         */
        fun fixedColor(color: BarColor): (Int) -> BarColor = { color }

        /**
         * 清理所有活跃的 BossBar
         */
        fun clearAllBossBars() {
            for (key in activeKeys.toList()) {
                Bukkit.removeBossBar(key)
            }
            activeKeys.clear()
        }
    }

}

/**
 * 基于 GameContext 创建 CountdownBossBar 自动筛选存活玩家
 */
fun countdownBossBar(
    context: GameContext,
    title: String,
    durationSeconds: Int,
    colorStrategy: (Int) -> BarColor = CountdownBossBar.defaultColorStrategy(),
    onTick: (suspend (remainingSeconds: Int) -> Unit)? = null
): CountdownBossBar {
    val key = NamespacedKey(FateCasino.INSTANCE, "bossbar_${UUID.randomUUID()}")
    return CountdownBossBar(
        key = key,
        titleProvider = { title },
        playersProvider = { context.playerSessions.values.filter { it.isActive }.mapNotNull { it.playerOrNull } },
        durationSeconds = durationSeconds,
        colorStrategy = colorStrategy,
        onTick = onTick
    )
}

/**
 * 基于 GameContext 创建动态 title 的 CountdownBossBar
 */
fun countdownBossBar(
    context: GameContext,
    titleProvider: () -> String,
    durationSeconds: Int,
    colorStrategy: (Int) -> BarColor = CountdownBossBar.defaultColorStrategy(),
    onTick: (suspend (remainingSeconds: Int) -> Unit)? = null
): CountdownBossBar {
    val key = NamespacedKey(FateCasino.INSTANCE, "bossbar_${UUID.randomUUID()}")
    return CountdownBossBar(
        key = key,
        titleProvider = titleProvider,
        playersProvider = { context.playerSessions.values.filter { it.isActive }.mapNotNull { it.playerOrNull } },
        durationSeconds = durationSeconds,
        colorStrategy = colorStrategy,
        onTick = onTick
    )
}