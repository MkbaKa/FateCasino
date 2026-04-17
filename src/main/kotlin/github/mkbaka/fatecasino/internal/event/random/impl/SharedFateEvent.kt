package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.ServerThreadDispatcher
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import github.mkbaka.fatecasino.internal.util.countdownBossBar
import github.mkbaka.fatecasino.internal.util.on
import github.mkbaka.fatecasino.internal.util.unregisterListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import java.util.UUID

object SharedFateEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("命运共同体").color(NamedTextColor.LIGHT_PURPLE)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int = 2

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val active = context.playerSessions.values.filter { it.isActive }
        if (active.size < 2) return

        // 随机两两配对
        val shuffled = active.shuffled()
        val pairs = shuffled.chunked(2).filter { it.size == 2 }

        val pairedMap = mutableMapOf<UUID, UUID>()
        for (pair in pairs) {
            pairedMap[pair[0].owner] = pair[1].owner
            pairedMap[pair[1].owner] = pair[0].owner
        }

        // 播报配对信息
        scope.callSync {
            for (pair in pairs) {
                val p1 = pair[0].playerOrNull
                val p2 = pair[1].playerOrNull
                if (p1 != null && p2 != null) {
                    Component.empty()
                        .append(Component.text("命运共同体: ").color(NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text(p1.name).color(NamedTextColor.YELLOW))
                        .append(Component.text(" ↔ ").color(NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text(p2.name).color(NamedTextColor.YELLOW))
                        .broadcast()
                }
            }
        }

        Sound.ENTITY_EVOKER_CAST_SPELL.broadcast(SoundCategory.MASTER, 1.0f, 1.0f)

        val tempListener = object : Listener {}

        // 伤害转移监听
        on<EntityDamageEvent>(listener = tempListener) {
            val damaged = entity as? Player ?: return@on
            val partnerId = pairedMap[damaged.uniqueId] ?: return@on

            val partnerSession = context.playerSessions[partnerId]
            val partner = partnerSession?.playerOrNull
            if (partner != null && partnerSession.isActive) {
                // 转移伤害
                partner.health = (partner.health - finalDamage).coerceAtLeast(0.0)
            }
        }

        // 创建 BossBar 倒计时
        val bossBar = countdownBossBar(
            context = context,
            title = LegacyComponentSerializer.legacySection().serialize(displayName),
            durationSeconds = 30
        )
        bossBar.run(scope)

        // 清理
        withContext(NonCancellable + ServerThreadDispatcher) {
            unregisterListener(tempListener)
        }

        Component.empty()
            .append(Component.text("命运共同体").color(NamedTextColor.LIGHT_PURPLE))
            .append(Component.newline())
            .append(Component.text("   命运的纽带已解除...").color(NamedTextColor.GRAY))
            .broadcast()
    }

}