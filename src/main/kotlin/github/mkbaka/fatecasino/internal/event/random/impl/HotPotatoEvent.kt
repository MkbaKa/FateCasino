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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.util.Vector
import java.util.UUID

object HotPotatoEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("烫手山芋").color(NamedTextColor.RED)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int = 3

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val active = context.playerSessions.values.filter { it.isActive }
        if (active.size < 2) return

        val initial = active.random()

        var holder: UUID = initial.owner

        val tempListener = object : Listener {}

        Component.empty()
            .append(Component.text("烫手山芋!").color(NamedTextColor.RED))
            .append(Component.newline())
            .append(Component.text("   近战击中其他人转移炸弹, 30 秒后持有者爆炸!").color(NamedTextColor.GRAY))
            .broadcast()

        Sound.ENTITY_TNT_PRIMED.broadcast(SoundCategory.MASTER, 1.0f, 1.0f)

        // 先注册监听器
        on<EntityDamageByEntityEvent>(listener = tempListener) {
            val target = entity as? Player ?: return@on
            val attacker = damager as? Player ?: return@on

            // 只有当前持有者击中别人才转移
            if (attacker.uniqueId == holder && target.uniqueId != holder) {
                holder = target.uniqueId

                Component.empty()
                    .append(Component.text("炸弹转移到了 ").color(NamedTextColor.RED))
                    .append(Component.text(target.name).color(NamedTextColor.YELLOW))
                    .append(Component.text(" 手中!").color(NamedTextColor.RED))
                    .broadcast()

                Sound.ENTITY_TNT_PRIMED.broadcast(SoundCategory.MASTER, 0.5f, 1.5f)
            }
        }

        // 创建 BossBar 倒计时 动态显示持有者
        val bossBar = countdownBossBar(
            context = context,
            titleProvider = {
                val holderName = Bukkit.getPlayer(holder)?.name ?: "未知"
                LegacyComponentSerializer.legacySection().serialize(displayName) + " - 持有者: $holderName"
            },
            durationSeconds = 30
        )
        bossBar.run(scope)

        // 爆炸持有者
        scope.callSync {
            val holderSession = context.playerSessions[holder]
            val holder = holderSession?.playerOrNull
            if (holder != null && holderSession.isActive) {
                // 给他一个落地水的表演机会
//                holder.damage(100.0)
                holder.velocity = holder.velocity.add(Vector(0.0, 5.0, 0.0))
                Component.empty()
                    .append(Component.text(holder.name).color(NamedTextColor.YELLOW))
                    .append(Component.text(" 被炸弹炸飞了!").color(NamedTextColor.RED))
                    .broadcast()
                holder.location.world.createExplosion(holder.location, 0f, false, false)
                Sound.ENTITY_GENERIC_EXPLODE.broadcast(SoundCategory.MASTER, 1.0f, 1.0f)
            }
        }

        // 清理
        withContext(NonCancellable + ServerThreadDispatcher) {
            unregisterListener(tempListener)
        }
    }

}