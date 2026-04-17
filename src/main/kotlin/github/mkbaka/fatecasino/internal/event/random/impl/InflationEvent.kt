package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.ServerThreadDispatcher
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.countdownBossBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Sound
import org.bukkit.SoundCategory

object InflationEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("通货膨胀").color(NamedTextColor.RED)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int = 2

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        context.priceMultiplier = 2

        Component.empty()
            .append(Component.text("通货膨胀!").color(NamedTextColor.RED))
            .append(Component.newline())
            .append(Component.text("   接下来 1 分钟内, 赌场所有消费价格翻倍!").color(NamedTextColor.GRAY))
            .broadcast()

        Sound.ENTITY_VILLAGER_NO.broadcast(SoundCategory.MASTER, 1.0f, 1.5f)

        val durationSeconds = 60
        val endTime = System.currentTimeMillis() + durationSeconds * 1000L

        // BossBar 提示
        val bossBar = countdownBossBar(
            context = context,
            title = LegacyComponentSerializer.legacySection().serialize(displayName),
            durationSeconds = durationSeconds
        )
        bossBar.show()

        var lastDisplayedSeconds = durationSeconds

        while (scope.isActive && System.currentTimeMillis() < endTime) {
            // 当时间变化时更新 BossBar 进度
            val remainingSeconds = ((endTime - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            if (remainingSeconds != lastDisplayedSeconds) {
                bossBar.updateProgress(remainingSeconds)
                lastDisplayedSeconds = remainingSeconds
            }

            delay(1000L)
        }

        withContext(NonCancellable + ServerThreadDispatcher) {
            bossBar.hide()
            context.priceMultiplier = 1
        }

        Component.empty()
            .append(Component.text("通货膨胀结束!").color(NamedTextColor.GREEN))
            .append(Component.newline())
            .append(Component.text("   价格已恢复正常").color(NamedTextColor.GRAY))
            .broadcast()
    }

}