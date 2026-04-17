package github.mkbaka.fatecasino.internal.phase.manager

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.event.random.impl.*
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import kotlinx.coroutines.*
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.GameRules
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import java.util.EnumMap
import java.util.concurrent.ThreadLocalRandom

class RandomEventManager(
    private val context: GameContext,
    private val scope: CoroutineScope,
) {

    companion object {
        val allEvents: List<RandomEvent> = listOf(
            HotPotatoEvent,
            SharedFateEvent,
            BlindGlowEvent,
            InflationEvent,
            FreeShoppingEvent,
            BountyEvent,
            RobinHoodEvent,
            AirdropEvent,
            BlindBoxEvent,
            HotbarShuffleEvent,
            ButterFingersEvent,
            GravityAnomalyEvent,
            AbsoluteZeroEvent,
            MentalDecayEvent,
            LocalRainEvent,
            SchrodingerDeliveryEvent,
            BigMouthEvent,
            SpiritAvatarEvent,
            PositionSwapEvent,
            DisarmEvent,
            JusticeThunderEvent,
            PigRideEvent,
            AcidRainEvent
        )
    }

    private val events = allEvents

    private val random = ThreadLocalRandom.current()

    private var scheduleJob: Job? = null

    // 每个阶段已触发的事件
    private val triggered: MutableMap<GamePhase, MutableSet<RandomEvent>> = EnumMap(GamePhase::class.java)

    fun start() {
        val startMS = System.currentTimeMillis()
        val peaceEndMS = startMS + context.config.playing.peaceTimeMS
        scheduleJob = scope.launch {
            // 和平结束
            delay(context.config.playing.peaceTimeMS)

            callSync {
                // 难度改回去
                context.world.difficulty = context.config.playing.difficulty
                // 打开摔伤
                context.world.setGameRule(GameRules.FALL_DAMAGE, true)
            }

            while (isActive) {
                // 每隔 30 到 60 秒触发一次随机事件
                delay(random.nextLong(30_000L, 60_000L))
                val elapsed = System.currentTimeMillis() - peaceEndMS
                val event = selectEvent(elapsed)

                triggerEvent(event)
            }
        }
    }

    fun stop() {
        scheduleJob?.cancel()
    }

    private fun selectEvent(elapsed: Long): RandomEvent {
        val phase = GamePhase.entries
            .firstOrNull { elapsed in it.timeRangeMS }
            ?: GamePhase.LATE

        // 后面阶段可以访问前面阶段的事件池
        // EARLY -> 只 EARLY    MID -> EARLY+MID    LATE -> 全部
        val allowedPhases = GamePhase.entries.take(phase.ordinal + 1)

        val triggered = triggered.computeIfAbsent(phase) { mutableSetOf() }
        val usableEvents = events.filter { it.phase in allowedPhases && it !in triggered }

        // 如果当前阶段所有事件都已触发 清空记录重新开始
        if (usableEvents.isEmpty()) {
            triggered.clear()
            return weightedRandom(events.filter { it.phase in allowedPhases })
        }

        val selected = weightedRandom(usableEvents)
        triggered.add(selected)
        return selected
    }

    private fun weightedRandom(events: List<RandomEvent>): RandomEvent {
        // 把所有事件的权重合并后 基于总权重随机一个数值
        // 拿到的数值就是 权重区间
        val total = events.sumOf { it.weight }
        var random = random.nextInt(total)

        for (event in events) {
            // 如果区间减去事件权重还有剩余值 说明随机到的区间不是这个事件的权重区间
            // 当结果小于 0 后 说明事件区间没有被全消费完
            // 那么随机命中的结果就应该是这片区间
            random -= event.weight
            if (random < 0) return event
        }

        // 以防万一
        return events.last()
    }

    private suspend fun triggerEvent(event: RandomEvent) {
        broadcast(event)
        event.execute(context, scope)
    }

    private suspend fun broadcast(event: RandomEvent) {
        val name = LegacyComponentSerializer.legacySection().serialize(event.displayName)
        val bossBar = Bukkit.createBossBar("下一个随机事件: $name", BarColor.GREEN, BarStyle.SOLID)

        // 添加所有活跃玩家
        scope.callSync {
            for (session in context.playerSessions.values.filter { it.isActive }) {
                session.playerOrNull?.let { bossBar.addPlayer(it) }
            }
        }

        try {
            for (i in 20 downTo 1) {
                // 更新 boss bar 进度和颜色
                bossBar.progress = i / 20.0
                bossBar.color = when {
                    i >= 15 -> BarColor.GREEN
                    i >= 10 -> BarColor.YELLOW
                    i >= 5 -> BarColor.PINK
                    else -> BarColor.RED
                }

                Sound.BLOCK_NOTE_BLOCK_HARP.broadcast(SoundCategory.MASTER, 1.0f, 1.0f)
                delay(1000L)
            }
        } finally {
            bossBar.removeAll()
        }
    }


}