package github.mkbaka.fatecasino.internal.phase.impl

import github.mkbaka.fatecasino.internal.phase.manager.BountyManager
import github.mkbaka.fatecasino.internal.phase.manager.BorderManager
import github.mkbaka.fatecasino.internal.phase.manager.RandomEventManager
import github.mkbaka.fatecasino.internal.phase.manager.SimpleJobManager
import github.mkbaka.fatecasino.internal.phase.AbstractGamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.phase.data.PlayerSession
import github.mkbaka.fatecasino.internal.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.entity.Firework
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

class PlayingPhase(
    context: GameContext,
    scope: CoroutineScope
) : AbstractGamePhase(context, scope), Listener {

    override val name: String
        get() = "游戏进行中"

    private val randomEventManager = RandomEventManager(context, scope)
    private val borderManager = BorderManager(context, scope)
    private val simpleJobManager = SimpleJobManager(context, scope)

    private val alivePlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    private val endSignal = CompletableDeferred<PlayerSession?>()

    private val mutex = Mutex()
    private var pendingEndJob: Job? = null

    val bountyManager = BountyManager(context, scope)

    override suspend fun onStart() {
        info("进入游戏阶段...")

        scope.callSync {
            // 删掉大厅
            context.lobbyLocation!!.buildLobby(
                platform = Material.AIR,
                wall = Material.AIR,
                config = context.config.lobby
            )

            // 来个音效
            Sound.ENTITY_GENERIC_EXPLODE.broadcast(
                SoundCategory.MASTER,
                1.0f,
                1.0f
            )

            // 重置状态
            context.playerSessions.values
                .forEach { session ->
                    session.player.reset()
                    session.giveChipItem()
                }

            // 关死亡不掉落
            context.world.setGameRule(GameRules.KEEP_INVENTORY, false)
            // 自动复活
            context.world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true)
        }

        alivePlayers.addAll(context.playerSessions.keys)

        gameEvent()
        stateControl()

        scope.callSync {
            context.world.time = 1000
        }

        randomEventManager.start()
        borderManager.start()
        simpleJobManager.start()
        bountyManager.start(this)
    }

    override suspend fun runPhase(): AbstractGamePhase? {
        val winner = endSignal.await()
        if (winner != null) {
            info("游戏结束, 胜者 ${winner.playerName}")
            broadcastWinner(winner)
        } else {
            info("游戏结束, 没有人胜利")
            Component.text("所有玩家死亡, 没有胜利者!")
                .color(NamedTextColor.RED)
                .broadcast()
        }

        return if (scope.isActive) EndPhase(context, scope) else null
    }

    override suspend fun onEnd() {
        randomEventManager.stop()
        borderManager.stop()
        simpleJobManager.stop()
        bountyManager.clearAll()
        unregisterListener(this)
        alivePlayers.clear()
        scope.callSync {
            resetAll()
        }

        info("游戏阶段结束")
    }

    private suspend fun checkGameEnd() {
        mutex.withLock {
            val activeSessions = context.playerSessions.values.filter { it.isActive }
            when (val activeCount = activeSessions.size) {
                // 全死完了
                0 -> startEndProcess(null)
                // 剩一个活着
                1 -> {
                    val maybeWinner = activeSessions.first()
                    // 可能会有存活但离线的玩家
                    val offlineAliveCount = alivePlayers.size - activeCount

                    if (offlineAliveCount > 0) {
                        // 给他一点时间重连
                        startEndProcess(maybeWinner, waitOffline = true)
                    } else {
                        // 没有其他活着的玩家了 延迟一会再判断
                        startEndProcess(maybeWinner)
                    }
                }

                else -> {
                    pendingEndJob?.cancel()
                    pendingEndJob = null
                }
            }
        }
    }

    private fun startEndProcess(winner: PlayerSession?, waitOffline: Boolean = false) {
        if (pendingEndJob != null) return

        pendingEndJob = scope.launch {
            if (waitOffline) {
                var remaining = context.config.playing.offlineWaitSeconds
                while (remaining > 0 && isActive) {
                    delay(1000L)
                    remaining--

                    val activeCount = context.playerSessions.values.count { it.isActive }
                    // 重连回来了
                    if (activeCount > 1) {
                        pendingEndJob = null
                        return@launch
                    }
                }
            }

            // 避免同归于尽时有一名玩家获胜
            delay(context.config.playing.confirmDelayMS)

            if (!isActive) return@launch

            val activeCount = context.playerSessions.values.count { it.isActive }
            when (activeCount) {
                0 -> endSignal.complete(null)
                // 只有一名玩家存活时 winner 不会传 null
                1 -> endSignal.complete(winner)
                else -> pendingEndJob = null
            }
        }
    }

    private fun broadcastWinner(winner: PlayerSession) {
        Component.empty()
            .append(Component.text("★ ").color(NamedTextColor.GOLD))
            .append(Component.text(winner.playerName).color(NamedTextColor.GREEN))
            .append(Component.text(" 获胜!").color(NamedTextColor.GOLD))
            .append(Component.text(" ★").color(NamedTextColor.GOLD))
            .broadcast()

        scope.launch {
            val random = ThreadLocalRandom.current()
            val colors = listOf(
                Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE,
                Color.PURPLE, Color.ORANGE, Color.FUCHSIA, Color.LIME
            )

            repeat(8) {
                // 0.5-0.8秒间隔
                delay(500L + random.nextInt(0, 300))

                scope.callSync {
                    val player = winner.playerOrNull ?: return@callSync
                    val loc = player.location.clone().add(
                        random.nextDouble(-1.5, 1.5),
                        0.0,
                        random.nextDouble(-1.5, 1.5)
                    )

                    val firework = loc.world.spawn(loc, Firework::class.java)
                    val meta = firework.fireworkMeta

                    meta.addEffect(
                        FireworkEffect.builder()
                            .with(FireworkEffect.Type.entries.random())
                            .withColor(colors.random())
                            .withFade(colors.random())
                            .trail(true)
                            .flicker(random.nextBoolean())
                            .build()
                    )
                    meta.power = 1

                    firework.fireworkMeta = meta
                }
            }
        }
    }

    private fun broadcastDeath(name: String, remaining: Int) {
        Component.empty()
            .append(Component.text(name).color(NamedTextColor.RED))
            .append(Component.text(" 已淘汰").color(NamedTextColor.RED))
            .append(Component.text(" | 剩余 ").color(NamedTextColor.WHITE))
            .append(Component.text(remaining).color(NamedTextColor.GREEN))
            .append(Component.text(" 名玩家").color(NamedTextColor.WHITE))
            .broadcast()
    }

    private fun gameEvent() {
        // 死亡检查
        on<PlayerDeathEvent>(listener = this) {
            if (player.uniqueId !in alivePlayers) return@on

            alivePlayers.remove(player.uniqueId)
            context.playerSessions[player.uniqueId]?.state = PlayerSession.State.SPECTATOR
            broadcastDeath(player.name, alivePlayers.size)

            player.gameMode = GameMode.SPECTATOR
            scope.launch { checkGameEnd() }
        }

        // 击杀奖励
        on<PlayerDeathEvent>(listener = this, priority = EventPriority.MONITOR) {
            val killer = player.killer
            if (killer != null && killer.uniqueId in alivePlayers) {
                val killerSession = context.playerSessions[killer.uniqueId]
                if (killerSession != null && killerSession.getTicket() < context.config.playing.maxTicket) {
                    killerSession.giveTicket(1)

                    Component.empty()
                        .append(Component.text("你击杀了 ").color(NamedTextColor.WHITE))
                        .append(Component.text(player.name).color(NamedTextColor.GREEN))
                        .append(Component.text(" 获得 ").color(NamedTextColor.WHITE))
                        .append(Component.text(1).color(NamedTextColor.GREEN))
                        .append(Component.text(" 张券!").color(NamedTextColor.WHITE))
                        .sendMessage(killer)
                }
            }
        }

        subscribeGamingEvents()
    }

    private fun stateControl() {
        on<PlayerQuitEvent>(listener = this) {
            val session = context.playerSessions[player.uniqueId] ?: return@on
            session.state = PlayerSession.State.OFFLINE
        }
        on<PlayerJoinEvent>(listener = this) {
            val session = context.playerSessions[player.uniqueId]
            if (session != null) {
                // 回滚之前的状态
                session.state = session.previousState
            } else {
                // 中途加入扔到观察者模式去
                player.gameMode = GameMode.SPECTATOR
                player.teleport(context.lobbyLocation!!)
            }
        }
    }

}