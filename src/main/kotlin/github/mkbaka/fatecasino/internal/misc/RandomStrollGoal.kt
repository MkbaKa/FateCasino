package github.mkbaka.fatecasino.internal.misc

import com.destroystokyo.paper.entity.ai.Goal
import com.destroystokyo.paper.entity.ai.GoalKey
import com.destroystokyo.paper.entity.ai.GoalType
import github.mkbaka.fatecasino.FateCasino
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Mob
import java.util.EnumSet
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.cos
import kotlin.math.sin

/**
 * 到处乱跑
 *
 * @param mob 目标生物
 * @param activateProbability 激活概率 (默认0.5, 即50%每tick检查)
 * @param strollSpeed 移动速度 (正常值=1.0, 更快=1.5+, 更慢=0.5-)
 * @param strollRadius 行走半径 (默认10格)
 */
class RandomStrollGoal(
    private val mob: Mob,
    private val activateProbability: Double = 0.5,
    private val strollSpeed: Double = 1.0,
    private val strollRadius: Int = 10
) : Goal<Mob> {

    private val random = ThreadLocalRandom.current()
    private var targetX: Double = 0.0
    private var targetY: Double = 0.0
    private var targetZ: Double = 0.0
    private var strolling: Boolean = false

    override fun shouldActivate(): Boolean {
        // 触发概率检查 - 比原版更频繁
        if (random.nextDouble() >= activateProbability) return false

        // 如果已经在移动中, 不重复激活
        if (strolling) return false

        // 生成随机目标位置
        val loc = mob.location
        val angle = random.nextDouble() * 2 * Math.PI
        val distance = random.nextDouble(1.0, strollRadius.toDouble())

        targetX = loc.x + cos(angle) * distance
        targetZ = loc.z + sin(angle) * distance
        targetY = loc.y // 保持当前Y轴高度 (寻路器会自动处理高度差异)

        return true
    }

    override fun shouldStayActive(): Boolean {
        // 如果还没到达目标, 保持激活
        return strolling
    }

    override fun start() {
        strolling = true
        // 使用寻路器移动到目标位置
        mob.pathfinder.moveTo(
            mob.world.getBlockAt(targetX.toInt(), targetY.toInt(), targetZ.toInt()).location,
            strollSpeed
        )
    }

    override fun tick() {
        // 检查是否到达目标或寻路失败
        if (!mob.pathfinder.hasPath()) {
            strolling = false
        }
    }

    override fun stop() {
        strolling = false
        // 停止移动
        mob.pathfinder.stopPathfinding()
    }

    override fun getKey(): GoalKey<Mob> =
        GoalKey.of(Mob::class.java, NamespacedKey(FateCasino.INSTANCE, KEY_NAME))

    override fun getTypes(): EnumSet<GoalType> =
        EnumSet.of(GoalType.MOVE)

    companion object {
        const val KEY_NAME = "random_stroll"

        /**
         * 为生物添加AI
         *
         * @param mob 目标生物
         * @param priority Goal优先级 (默认1, 最高优先级)
         * @param probability 激活概率
         * @param speed 移动速度 (正常值=1.0)
         * @param radius 行走半径
         */
        fun applyTo(
            mob: Mob,
            priority: Int = 1,
            probability: Double = 0.5,
            speed: Double = 1.0,
            radius: Int = 10
        ) {
            val goal = RandomStrollGoal(mob, probability, speed, radius)
            Bukkit.getMobGoals().addGoal(mob, priority, goal)
        }
    }
}