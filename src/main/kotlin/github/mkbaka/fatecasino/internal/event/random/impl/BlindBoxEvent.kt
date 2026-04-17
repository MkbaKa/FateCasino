package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.ServerThreadDispatcher
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.countdownBossBar
import github.mkbaka.fatecasino.internal.util.on
import github.mkbaka.fatecasino.internal.util.unregisterListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemType

object BlindBoxEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("万物盲盒").color(NamedTextColor.DARK_GREEN)

    override val phase: GamePhase
        get() = GamePhase.MID

    // 可能有什么极端地形需要这个事件拯救
    override val weight: Int = 4

    // 用 Material 随机不知道为啥总是会触发 CraftLegacy 那一堆垃圾信息
    private val validItemTypes: List<ItemType> = Registry.ITEM.stream().toList()

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val tempListener = object : Listener {}

        Component.empty()
            .append(Component.text("万物盲盒").color(NamedTextColor.DARK_GREEN))
            .append(Component.newline())
            .append(Component.text("   方块不再掉落它本身...").color(NamedTextColor.GRAY))
            .broadcast()

        Sound.BLOCK_CHEST_OPEN.broadcast(SoundCategory.MASTER, 1.0f, 0.5f)

        // 先注册监听器
        on<BlockBreakEvent>(listener = tempListener) {
            val session = context.playerSessions[player.uniqueId]
            if (session == null || !session.isActive) return@on

            val itemType = validItemTypes.random()
            val dropItem = itemType.createItemStack()

            isDropItems = false
            block.world.dropItem(block.location, dropItem)
        }

        // 创建 BossBar 倒计时
        val bossBar = countdownBossBar(
            context = context,
            title = LegacyComponentSerializer.legacySection().serialize(displayName),
            durationSeconds = 30
        )
        bossBar.run(scope)

        // 确保即使 scope 取消也能注销监听器
        withContext(NonCancellable + ServerThreadDispatcher) {
            unregisterListener(tempListener)
        }

        Component.empty()
            .append(Component.text("万物盲盒结束").color(NamedTextColor.DARK_GREEN))
            .append(Component.newline())
            .append(Component.text("   方块恢复正常掉落").color(NamedTextColor.GRAY))
            .broadcast()
    }

}