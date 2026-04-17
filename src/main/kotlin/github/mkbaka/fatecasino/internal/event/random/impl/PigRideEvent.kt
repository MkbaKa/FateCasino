package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.misc.RandomStrollGoal
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Pig
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDismountEvent

object PigRideEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("全员变猪").color(NamedTextColor.GOLD)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int = 3

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val active = context.playerSessions.values.filter { it.isActive }
        if (active.isEmpty()) return

        Component.empty()
            .append(Component.text("全员变猪!").color(NamedTextColor.GOLD))
            .append(Component.newline())
            .append(Component.text("   每个人都骑上一只失控的极速猪!").color(NamedTextColor.GRAY))
            .broadcast()

        Sound.ENTITY_PIG_AMBIENT.broadcast(SoundCategory.MASTER, 1.0f, 1.5f)

        // 存储所有生成的猪用于清理
        val spawnedPigs = mutableListOf<Entity>()

        // 阻止玩家下猪
        val tempListener = object : Listener {}
        on<EntityDismountEvent>(listener = tempListener) {
            // 只阻止玩家从猪上下来
            if (entity !is Player) return@on
            if (dismounted !is Pig) return@on
            if (dismounted !in spawnedPigs) return@on

            // 不准下来
            isCancelled = true
        }

        scope.callSync {
            for (session in active) {
                val player = session.playerOrNull ?: continue
                val loc = player.location.clone()

                // 在玩家位置生成一只猪
                val pig = player.world.spawn(
                    loc,
                    Pig::class.java
                ) { entity ->
                    // 让他到处乱跑
                    RandomStrollGoal.applyTo(
                        mob = entity,
                        probability = 0.6,
                        speed = 3.0,
                        radius = 15
                    )
                }

                spawnedPigs.add(pig)

                // 玩家骑上猪
                pig.addPassenger(player)
            }
        }

        val durationSeconds = 10
        val bossBar = countdownBossBar(
            context = context,
            title = LegacyComponentSerializer.legacySection().serialize(displayName),
            durationSeconds = durationSeconds
        )
        bossBar.run(scope)

        // 清理所有猪
        withContext(NonCancellable + ServerThreadDispatcher) {
            unregisterListener(tempListener)

            for (pig in spawnedPigs) {
                if (!pig.isValid) continue

                pig.passengers.forEach { passenger ->
                    pig.removePassenger(passenger)
                }

                pig.remove()
            }
        }

        Component.empty()
            .append(Component.text("猪猪狂欢结束了!").color(NamedTextColor.GOLD))
            .append(Component.newline())
            .append(Component.text("   一切恢复平静...").color(NamedTextColor.GRAY))
            .broadcast()
        Sound.ENTITY_PIG_DEATH.broadcast(SoundCategory.MASTER, 1.0f, 1.0f)
    }

}