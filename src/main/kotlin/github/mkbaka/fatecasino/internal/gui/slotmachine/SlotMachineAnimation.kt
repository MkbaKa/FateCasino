package github.mkbaka.fatecasino.internal.gui.slotmachine

import github.mkbaka.fatecasino.internal.event.slotmachine.RewardEvent
import github.mkbaka.fatecasino.internal.event.slotmachine.RewardLevel
import github.mkbaka.fatecasino.internal.event.slotmachine.SlotMachinePhase
import github.mkbaka.fatecasino.internal.menu.Menu
import github.mkbaka.fatecasino.internal.util.playSound
import kotlinx.coroutines.delay
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import java.util.concurrent.ThreadLocalRandom

class SlotMachineAnimation(
    private val menu: Menu,
    private val phase: SlotMachinePhase,
    private val player: Player
) {

    private val inventory get() = menu.inventory

    /** 打乱后的每列元素 */
    private val columns = Array(3) {
        phase.scrollItems.toList().shuffled()
    }

    /** 每列当前的索引值 */
    private val offsets = IntArray(3)

    /** 每列的停止状态 */
    private val stopped = BooleanArray(3)

    /** 滚动计时 */
    private var elapsed = 0L

    var executed = false
        private set

    /**
     * 初始化界面
     */
    fun init(): SlotMachineAnimation {
        for ((column, scroll) in columns.withIndex()) {
            for (row in 0 until 3) {
                inventory.setItem(11 + (2 * column) + (9 * row), scroll[row].displayItem)
            }
        }

        return this
    }

    /**
     * 开始滚动
     */
    suspend fun run(stopChance: Float): Pair<RewardEvent?, RewardLevel> {
        executed = true

        var renderDelay = 100L

        while (!stopped.all { it }) {
            // 更新每列的索引
            for (column in 0..2) {
                if (!stopped[column]) {
                    offsets[column]++
                }
            }

            render()

            elapsed += renderDelay

            // 已滚动时间大于 3 秒后
            if (elapsed >= 3000L) {
                // 滚动越来越慢
                renderDelay = (renderDelay * 1.1).toLong().coerceAtMost(500L)

                // 看看有没有会停止的列
                for (column in 0..2) {
                    if (!stopped[column] && random(stopChance)) {
                        stopped[column] = true
                    }
                }
            }

            delay(renderDelay)
        }

        // 获取中间行的三个 ScrollItem
        val results = columns.mapIndexed { column, scroll ->
            val index = (offsets[column] + 1) % scroll.size
            scroll[index]
        }

        // 按 material 分组, 找出现最多的
        val grouped = results.groupBy { it.displayItem.type }
        val most = grouped.maxBy { it.value.size }

        val level = RewardLevel.entries[most.value.size - 1]
        // 三个不同物品时不触发任何事件
        val event = if (most.value.size == 1) null else most.value.first().event

        return event to level
    }

    private fun render() {
        for (column in 0..2) {
            val scroll = columns[column]
            // 11 是左上角的槽位索引
            val baseSlot = 11 + 2 * column

            for (row in 0..2) {
                // 取余可以对列表元素循环引用 这样就不用把列表塞的很长了
                val index = (offsets[column] + (2 - row)) % scroll.size
                // 当前列竖向对应的索引
                val slot = baseSlot + 9 * row
                inventory.setItem(slot, scroll[index].displayItem)
            }
        }

        Sound.BLOCK_LEVER_CLICK.playSound(player, SoundCategory.MASTER, 1.0f, 1.0f)
    }

    private fun random(stop: Float) =
        ThreadLocalRandom.current().nextFloat(1.0f) <= stop

}
