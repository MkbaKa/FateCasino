package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import github.mkbaka.fatecasino.internal.util.countdownBossBar
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object BlindGlowEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("致盲与高亮").color(NamedTextColor.RED)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int = 3

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val active = context.playerSessions.values.filter { it.isActive }
        val durationSeconds = 30
        val durationTicks = durationSeconds * 20

        // 先播报事件
        Component.empty()
            .append(Component.text("黑暗森林降临").color(NamedTextColor.RED))
            .append(Component.newline())
            .append(Component.text("   看得见彼此, 看不清地形...").color(NamedTextColor.GRAY))
            .broadcast()

        Sound.ENTITY_WITCH_AMBIENT.broadcast(SoundCategory.MASTER, 1.0f, 0.5f)

        // 先应用药水效果
        scope.callSync {
            for (session in active) {
                val player = session.playerOrNull ?: continue
                player.addPotionEffect(
                    PotionEffect(PotionEffectType.BLINDNESS, durationTicks, 0)
                )
                player.addPotionEffect(
                    PotionEffect(PotionEffectType.GLOWING, durationTicks, 0)
                )
            }
        }

        // BossBar 倒计时显示效果剩余时间
        val bossBar = countdownBossBar(
            context = context,
            title = LegacyComponentSerializer.legacySection().serialize(displayName),
            durationSeconds = durationSeconds
        )
        bossBar.run(scope)
    }

}