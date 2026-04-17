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
import java.util.concurrent.ThreadLocalRandom

object MentalDecayEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("精神衰弱").color(NamedTextColor.DARK_PURPLE)

    override val phase: GamePhase
        get() = GamePhase.LATE

    override val weight: Int = 3

    private val scarySounds = listOf(
        Sound.ENTITY_CREEPER_HURT to 0.8f,
        Sound.ENTITY_ENDERMAN_SCREAM to 1.0f,
        Sound.ENTITY_ENDERMAN_TELEPORT to 0.5f,
        Sound.BLOCK_GLASS_BREAK to 1.2f,
        Sound.ENTITY_ZOMBIE_INFECT to 0.7f,
        Sound.ENTITY_WITHER_AMBIENT to 0.4f,
        Sound.ENTITY_SPIDER_AMBIENT to 1.0f,
        Sound.ENTITY_SILVERFISH_STEP to 2.0f,
    )

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val random = ThreadLocalRandom.current()
        val durationSeconds = 30
        val startTime = System.currentTimeMillis()
        val endTime = startTime + durationSeconds * 1000L

        Component.empty()
            .append(Component.text("精神衰弱!").color(NamedTextColor.DARK_PURPLE))
            .append(Component.newline())
            .append(Component.text("   你听到了什么...?").color(NamedTextColor.GRAY))
            .broadcast()

        // 被动模式 BossBar：由外部循环控制更新
        val bossBar = countdownBossBar(
            context = context,
            title = LegacyComponentSerializer.legacySection().serialize(displayName),
            durationSeconds = durationSeconds
        )
        bossBar.show()

        try {
            while (scope.isActive && System.currentTimeMillis() < endTime) {
                val (sound, pitch) = scarySounds[random.nextInt(scarySounds.size)]
                val delayMs = random.nextLong(800L, 2000L)

                // 播放恐怖音效
                scope.callSync {
                    for (session in context.playerSessions.values.filter { it.isActive }) {
                        val player = session.playerOrNull ?: continue
                        player.playSound(player.location, sound, SoundCategory.MASTER, 0.6f, pitch)
                    }
                }

                // 计算剩余秒数并更新 BossBar
                val elapsedMs = System.currentTimeMillis() - startTime
                val remainingSeconds = ((durationSeconds * 1000L - elapsedMs) / 1000L).toInt().coerceAtLeast(0)
                bossBar.updateProgress(remainingSeconds)

                delay(delayMs)
            }
        } finally {
            bossBar.hide()
        }
    }

}