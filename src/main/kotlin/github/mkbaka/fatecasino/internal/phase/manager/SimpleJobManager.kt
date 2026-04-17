package github.mkbaka.fatecasino.internal.phase.manager

import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import github.mkbaka.fatecasino.internal.util.sendActionBar
import github.mkbaka.fatecasino.internal.util.sendMessage
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Sound
import org.bukkit.SoundCategory

class SimpleJobManager(
    private val context: GameContext,
    private val scope: CoroutineScope
) {

    private val jobs = mutableListOf<Job>()

    fun start() {
        startBroadcastJob()
        startTicketJob()
    }

    fun stop() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    /**
     * 广播命运券状态
     */
    private fun startBroadcastJob() {
        val job = scope.launch {
            while (scope.isActive) {
                delay(1000L)

                val maxTicket = context.config.playing.maxTicket
                context.playerSessions.values
                    .asSequence()
                    .filter { it.isActive }
                    .forEach { session ->
                        val player = session.playerOrNull ?: return@forEach

                        Component.empty()
                            .append(Component.text("当前持有的命运券 ").color(NamedTextColor.WHITE))
                            .append(session.buildTicketTip(maxTicket))
                            .sendActionBar(player)
                    }
            }
        }
        jobs.add(job)
    }

    /**
     * 定时给予命运券
     */
    private fun startTicketJob() {
        val job = scope.launch {
            // 和平时间结束后开始
            delay(context.config.playing.peaceTimeMS)

            val maxTicket = context.config.playing.maxTicket
            while (scope.isActive) {
                delay(context.config.playing.peaceTimeMS)

                context.playerSessions.values
                    .asSequence()
                    .filter { it.isActive || it.isOffline }
                    .forEach { session ->
                        if (session.getTicket() >= maxTicket) {
                            val player = session.playerOrNull ?: return@forEach
                            Component.empty()
                                .append(Component.text("命运券已满 ").color(NamedTextColor.WHITE))
                                .append(session.buildTicketTip(maxTicket))
                                .append(Component.text(", 快去消费吧!").color(NamedTextColor.WHITE))
                                .sendActionBar(player)
                            return@forEach
                        }

                        session.giveTicket(1)
                        val player = session.playerOrNull ?: return@forEach
                        Component.empty()
                            .append(Component.text("你获得了 ").color(NamedTextColor.WHITE))
                            .append(Component.text("1").color(NamedTextColor.GREEN))
                            .append(Component.text(" 张命运券 ").color(NamedTextColor.WHITE))
                            .append(session.buildTicketTip(maxTicket))
                            .sendMessage(player)
                    }
            }
        }
        jobs.add(job)
    }

}