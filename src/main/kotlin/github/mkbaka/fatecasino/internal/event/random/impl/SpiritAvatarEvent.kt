package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.FateCasino
import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.CountdownBossBar
import github.mkbaka.fatecasino.internal.util.ServerThreadDispatcher
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import github.mkbaka.fatecasino.internal.util.countdownBossBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.boss.BarColor
import org.bukkit.entity.Player

object SpiritAvatarEvent : RandomEvent {

    private val SCALE_KEY = NamespacedKey(FateCasino.INSTANCE, "spirit_avatar_scale")
    private val ENTITY_RANGE_KEY = NamespacedKey(FateCasino.INSTANCE, "spirit_avatar_entity_range")
    private val BLOCK_RANGE_KEY = NamespacedKey(FateCasino.INSTANCE, "spirit_avatar_block_range")
    private val ATTACK_DAMAGE_KEY = NamespacedKey(FateCasino.INSTANCE, "spirit_avatar_attack_damage")

    override val displayName: Component
        get() = Component.text("武魂真身").color(NamedTextColor.GOLD)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int
        get() = 3

    override suspend fun execute(
        context: GameContext,
        scope: CoroutineScope
    ) {
        val session = context.playerSessions.values.filter { it.isActive }.random()

        val player = session.playerOrNull ?: return

        // 创建 BossBar 倒计时
        val bossBar = countdownBossBar(
            context = context,
            titleProvider = { "${LegacyComponentSerializer.legacySection().serialize(displayName)} - ${player.name}" },
            durationSeconds = 30,
            colorStrategy = CountdownBossBar.fixedColor(BarColor.YELLOW)
        )

        // 启动 BossBar 倒计时
        scope.launch { bossBar.run(scope) }

        scope.callSync {
            val scaleAttr = player.getAttribute(Attribute.SCALE)!!
            val entityRangeAttr = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)!!
            val blockRangeAttr = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)!!
            val attackDamageAttr = player.getAttribute(Attribute.ATTACK_DAMAGE)!!

            scaleAttr.addModifier(
                AttributeModifier(SCALE_KEY, 3.0, AttributeModifier.Operation.ADD_SCALAR)
            )
            entityRangeAttr.addModifier(
                AttributeModifier(ENTITY_RANGE_KEY, 3.0, AttributeModifier.Operation.ADD_SCALAR)
            )
            blockRangeAttr.addModifier(
                AttributeModifier(BLOCK_RANGE_KEY, 3.0, AttributeModifier.Operation.ADD_SCALAR)
            )
            attackDamageAttr.addModifier(
                AttributeModifier(ATTACK_DAMAGE_KEY, 1.5, AttributeModifier.Operation.ADD_SCALAR)
            )
        }

        Component.empty()
            .append(Component.text("武魂真身").color(NamedTextColor.GOLD))
            .append(Component.newline())
            .append(Component.text("   感觉自己强的可怕!").color(NamedTextColor.GRAY))
            .broadcast()

        delay(30_000L)

        // 清理属性修改
        withContext(NonCancellable + ServerThreadDispatcher) {
            cleanup(player)
        }

        Component.empty()
            .append(Component.text("武魂真身").color(NamedTextColor.GOLD))
            .append(Component.newline())
            .append(Component.text("   真身效果已消散...").color(NamedTextColor.GRAY))
            .broadcast()
    }

    fun cleanup(player: Player) {
        val scaleAttr = player.getAttribute(Attribute.SCALE)!!
        val entityRangeAttr = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)!!
        val blockRangeAttr = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)!!
        val attackDamageAttr = player.getAttribute(Attribute.ATTACK_DAMAGE)!!

        scaleAttr.removeModifier(SCALE_KEY)
        entityRangeAttr.removeModifier(ENTITY_RANGE_KEY)
        blockRangeAttr.removeModifier(BLOCK_RANGE_KEY)
        attackDamageAttr.removeModifier(ATTACK_DAMAGE_KEY)
    }

}