package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.ServerThreadDispatcher
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import github.mkbaka.fatecasino.internal.util.countdownBossBar
import github.mkbaka.fatecasino.internal.util.playSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player

object JusticeThunderEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("天降正义").color(NamedTextColor.YELLOW)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int = 2

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val active = context.playerSessions.values.filter { it.isActive }
        if (active.isEmpty()) return

        // 随机索敌
        val target = active.random()
        val targetPlayer = target.playerOrNull ?: return

        Component.empty()
            .append(Component.text("天降正义!").color(NamedTextColor.YELLOW))
            .append(Component.newline())
            .append(Component.text("   雷云正在聚集在 ").color(NamedTextColor.GRAY))
            .append(Component.text(targetPlayer.name).color(NamedTextColor.YELLOW))
            .append(Component.text(" 头顶...").color(NamedTextColor.GRAY))
            .broadcast()

        // 5秒倒计时 显示雷云粒子
        val durationSeconds = 5
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

            scope.callSync {
                val player = target.playerOrNull
                if (player != null && target.isActive) {
                    val loc = player.location.clone().add(0.0, 3.0, 0.0)

                    // 雷云粒子效果
                    player.world.spawnParticle(Particle.CLOUD, loc, 30, 1.0, 0.5, 1.0, 0.0)

                    // 倒计时音效
                    val remaining = ((endTime - System.currentTimeMillis()) / 1000).toInt()
                    if (remaining > 0) {
                        Sound.BLOCK_NOTE_BLOCK_HAT.playSound(player, SoundCategory.MASTER, 1.0f, 1.5f)
                    }
                }
            }

            delay(500L)
        }

        withContext(NonCancellable + ServerThreadDispatcher) {
            bossBar.hide()
        }

        // 闪电劈下
        scope.callSync {
            val player = target.playerOrNull
            if (player != null && target.isActive) {
                val loc = player.location

                // 视觉闪电效果
                loc.world.strikeLightningEffect(loc)
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER.broadcast(SoundCategory.MASTER, 1.0f, 1.0f)

                // 找到周围5格内的所有玩家
                val nearbyPlayers = loc.getNearbyEntities(5.0, 5.0, 5.0)
                    .filterIsInstance<Player>()
                    .filter { p -> context.playerSessions[p.uniqueId]?.isActive == true }

                if (nearbyPlayers.isEmpty()) {
                    // 只有目标一人 自己承担全部伤害
                    player.damage(20.0)
                } else {
                    // 伤害均摊
                    val perPlayerDamage = 20.0 / nearbyPlayers.size
                    for (victim in nearbyPlayers) {
                        victim.damage(perPlayerDamage)
                    }

                    Component.empty()
                        .append(Component.text("闪电劈下").color(NamedTextColor.YELLOW))
                        .append(Component.newline())
                        .append(Component.text("   ").color(NamedTextColor.GRAY))
                        .append(Component.text(nearbyPlayers.size).color(NamedTextColor.YELLOW))
                        .append(Component.text(" 名玩家均摊了 ").color(NamedTextColor.GRAY))
                        .append(Component.text(20).color(NamedTextColor.YELLOW))
                        .append(Component.text(" 点伤害!").color(NamedTextColor.GRAY))
                        .broadcast()
                }
            }
        }
    }

}