package github.mkbaka.fatecasino.internal.phase.impl

import github.mkbaka.fatecasino.internal.phase.AbstractGamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.phase.data.PlayerSession
import github.mkbaka.fatecasino.internal.util.*
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class WaitPhase(
    context: GameContext, scope: CoroutineScope
) : AbstractGamePhase(context, scope), Listener {

    override val name: String
        get() = "等待开始"

    private var broadcastJob: Job? = null

    // 双击检测时间
    private val sneakCache: MutableMap<UUID, Long> = hashMapOf()

    // 已准备的玩家
    private val confirmed: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    // 开始游戏
    private val startSignal = CompletableDeferred<Unit>()

    private var countdownJob: Job? = null

    // 只会在倒计时的协程使用
    private var countdownSeconds: Int = 0

    override suspend fun onStart() {
        info("进入等待阶段")

        broadcastJob = scope.launch {
            while (isActive) {
                Component.empty()
                    .append(Component.text("双击 ").color(NamedTextColor.WHITE))
                    .append(Component.keybind("key.sneak").color(NamedTextColor.YELLOW))
                    .append(Component.text(" 切换准备状态").color(NamedTextColor.WHITE))
                    .broadcastActionbar()
                delay(50L)
            }
        }

        on<PlayerJoinEvent>(listener = this) {
            player.gameMode = GameMode.SURVIVAL
            player.teleport(context.lobbyLocation!!)
        }

        on<PlayerQuitEvent>(listener = this) {
            confirmed.remove(player.uniqueId)
        }

        on<PlayerToggleSneakEvent>(listener = this) {
            // 不判断起身
            if (!isSneaking) return@on

            val now = System.currentTimeMillis()
            val lastTime = sneakCache[player.uniqueId]

            if (lastTime != null && now < lastTime + context.config.wait.doubleClickWindowMS) {
                // 指定时间内的第二次按下
                sneakCache.remove(player.uniqueId)
                changeState(player)
            } else {
                // 第一次点击 记录时间
                sneakCache[player.uniqueId] = now
            }
        }

        on<PlayerRespawnEvent>(listener = this) {
            respawnLocation = context.lobbyLocation!!
        }
    }

    override suspend fun runPhase(): AbstractGamePhase? {
        // 等人
        startSignal.await()

        if (!scope.isActive) return null

        info("游戏即将开始, 共 ${confirmed.size} 名玩家...")

        scope.callSync {
            // 初始化数据 只取准备且在线的玩家
            val players = confirmed.mapNotNull { Bukkit.getPlayer(it) }

            for (player in players) {
                context.playerSessions[player.uniqueId] = PlayerSession(player.uniqueId, player.name)
            }

            // 没准备的玩家变成观察者模式
            for (player in Bukkit.getOnlinePlayers()) {
                if (player.uniqueId !in confirmed) {
                    player.gameMode = GameMode.SPECTATOR
                }
            }
        }

        return PlayingPhase(context, scope)
    }

    override suspend fun onEnd() {
        broadcastJob?.cancel()
        countdownJob?.cancel()
        unregisterListener(this)
        sneakCache.clear()
        info("等待阶段结束")
    }

    private fun tryStartCountdown() {
        if (countdownJob != null) return

        val minPlayers = context.config.wait.minPlayers
        // 人数不够则不启动倒计时
        if (confirmed.size < minPlayers) return

        countdownSeconds = context.config.wait.waitForPlayers

        Component.empty()
            .append(Component.text("准备人数已达标, 游戏将在 ").color(NamedTextColor.WHITE))
            .append(Component.text(countdownSeconds).color(NamedTextColor.GREEN))
            .append(Component.text(" 秒后开始!").color(NamedTextColor.WHITE))
            .broadcast()

        countdownJob = scope.launch {
            while (countdownSeconds > 0 && isActive) {
                delay(1000L)

                // 若倒计时中有人取消准备导致人数不足则不继续执行
                if (confirmed.size < minPlayers) {
                    Component.empty()
                        .append(Component.text("准备人数不足，倒计时已取消")).color(NamedTextColor.RED)
                        .broadcast()

                    countdownSeconds = 0
                    countdownJob = null
                    cancel()
                    return@launch
                }

                countdownSeconds--

                // 小巧思
                val pitch: Float =
                    0.5f + 1.5f * (1.0f - countdownSeconds.toFloat() / context.config.wait.waitForPlayers.toFloat())
                Sound.BLOCK_NOTE_BLOCK_PLING.broadcast(
                    SoundCategory.MASTER,
                    1.0f,
                    pitch
                )

                if (countdownSeconds > 0) {
                    Component.empty()
                        .append(Component.text("游戏将在 ").color(NamedTextColor.WHITE))
                        .append(Component.text(countdownSeconds).color(NamedTextColor.YELLOW))
                        .append(Component.text(" 秒后开始").color(NamedTextColor.WHITE))
                        .broadcast()
                } else {
                    Component.empty()
                        .append(
                            Component.text("游戏开始!")
                                .color(NamedTextColor.GREEN)
                        ).broadcast()

                    Component.empty()
                        .append(Component.text("前 ").color(NamedTextColor.WHITE))
                        .append(
                            Component.text(context.config.playing.peaceTimeMS / 1000)
                                .color(NamedTextColor.YELLOW)
                        )
                        .append(Component.text(" 秒处于和平时间!").color(NamedTextColor.WHITE))
                        .broadcast()

                    scope.launch {
                        createSequence(
                            text = "「所有命运的馈赠, 早已在暗中标好了价格.」",
                            componentBuilder = { char, _ ->
                                val component = Component.text(char)

                                if (char == '「' || char == '」') {
                                    component.color(NamedTextColor.WHITE)
                                } else {
                                    component
                                }
                            },
                            nextCharBuilder = { char, _ ->
                                val component = Component.text(char)
                                    .decorate(TextDecoration.OBFUSCATED)

                                if (char == '「' || char == '」') {
                                    component.color(NamedTextColor.WHITE)
                                } else {
                                    component
                                }
                            }
                        ).forEach { component ->
                            Title.title(
                                Component.empty(),
                                component,
                                0,
                                5,
                                2
                            ).broadcast()
                            delay(50L)
                        }

                        delay(100L)

                        Title.title(
                            Component.empty(),
                            Component.empty()
                                .append(
                                    Component.text("「").color(NamedTextColor.DARK_GRAY)
                                        .decorate(TextDecoration.STRIKETHROUGH)
                                ).append(
                                    Component.text("所有命运的馈赠, 早已在暗中标好了价格.")
                                        .color(NamedTextColor.DARK_GRAY)
                                        .decorate(TextDecoration.STRIKETHROUGH)
                                ).append(
                                    Component.text("」").color(NamedTextColor.DARK_GRAY)
                                        .decorate(TextDecoration.STRIKETHROUGH)
                                ),
                            0,
                            3,
                            2
                        ).broadcast()
                    }

                    startSignal.complete(Unit)
                }
            }
        }
    }

    private fun changeState(player: Player) {
        val uuid = player.uniqueId
        if (uuid in confirmed) {
            confirmed.remove(uuid)
            Sound.BLOCK_STONE_BUTTON_CLICK_OFF.playSound(player, SoundCategory.MASTER, 1.0f, 0.5f)
            Component.empty()
                .append(Component.text("当前状态 ").color(NamedTextColor.WHITE))
                .append(Component.text("未准备").color(NamedTextColor.RED))
                .sendMessage(player)
        } else {
            confirmed.add(uuid)
            Sound.BLOCK_STONE_BUTTON_CLICK_ON.playSound(player, SoundCategory.MASTER, 1.0f, 1.0f)
            Component.empty()
                .append(Component.text("当前状态 ").color(NamedTextColor.WHITE))
                .append(Component.text("已准备").color(NamedTextColor.GREEN))
                .sendMessage(player)
        }

        val confirmedCount = confirmed.size
        val needCount = context.config.wait.minPlayers

        val msg = Component.empty()
            .append(Component.text("当前已有 ").color(NamedTextColor.WHITE))
            .append(Component.text(confirmedCount).color(NamedTextColor.YELLOW))
            .append(Component.text(" 名玩家准备参与游戏").color(NamedTextColor.WHITE))

        if (confirmedCount < needCount) {
            msg.append(Component.text(", 至少需要 ").color(NamedTextColor.WHITE))
                .append(Component.text(needCount).color(NamedTextColor.YELLOW))
                .append(Component.text(" 名玩家才能开始游戏").color(NamedTextColor.WHITE))
                .broadcast()
        } else {
            msg.broadcast()
        }

        tryStartCountdown()
    }

}