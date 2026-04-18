package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import github.mkbaka.fatecasino.internal.util.countdownBossBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object GravityAnomalyEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("重力异常").color(NamedTextColor.LIGHT_PURPLE)

    override val phase: GamePhase
        get() = GamePhase.EARLY

    override val weight: Int = 2

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val active = context.playerSessions.values.filter { it.isActive }

        for (session in active) {
            val player = session.playerOrNull ?: continue
            scope.callSync {
                player.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, 30 * 20, 30))
                player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, 30 * 20, 0))
            }
        }

        Component.empty()
            .append(Component.text("重力异常").color(NamedTextColor.LIGHT_PURPLE))
            .append(Component.newline())
            .append(Component.text("   注意夜间飞行安全!").color(NamedTextColor.GRAY))
            .broadcast()

        Sound.ENTITY_PHANTOM_FLAP.broadcast(SoundCategory.MASTER, 1.0f, 0.5f)

        val durationSeconds = 30
        val startTime = System.currentTimeMillis()
        val endTime = startTime + durationSeconds * 1000L

        // BossBar 提示
        val bossBar = countdownBossBar(
            context = context,
            title = LegacyComponentSerializer.legacySection().serialize(displayName),
            durationSeconds = durationSeconds
        )
        bossBar.show()

        var lastDisplayedSeconds = durationSeconds

        while (scope.isActive && System.currentTimeMillis() < endTime) {
            val remainingSeconds = ((endTime - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            if (remainingSeconds != lastDisplayedSeconds) {
                bossBar.updateProgress(remainingSeconds)
                lastDisplayedSeconds = remainingSeconds
            }
            delay(1000L)
        }

        bossBar.hide()

        Sound.ENTITY_PHANTOM_FLAP.broadcast(SoundCategory.MASTER, 1.0f, 1.0f)
    }

}