package github.mkbaka.fatecasino.internal.phase.impl

import github.mkbaka.fatecasino.internal.phase.AbstractGamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.broadcastActionbar
import github.mkbaka.fatecasino.internal.util.buildLobby
import github.mkbaka.fatecasino.internal.util.callSync
import github.mkbaka.fatecasino.internal.util.info
import github.mkbaka.fatecasino.internal.util.logEx
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asDeferred
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.Display
import org.bukkit.entity.TextDisplay
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.concurrent.ThreadLocalRandom
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Throwable
import kotlin.Unit
import kotlin.error
import kotlin.let
import kotlin.math.max
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

class InitPhase(
    context: GameContext,
    scope: CoroutineScope,
    private val postGenerated: (suspend () -> Unit)? = null
) : AbstractGamePhase(context, scope) {

    override val name: String
        get() = "初始化"

    private var tipJob: Job? = null

    override suspend fun onStart() {
        info("进入初始化阶段...")

        tipJob = scope.launch {
            var dots = 0
            while (isActive) {
                Component.empty()
                    .append(Component.text("正在随机地图位置"))
                    .append(Component.text(".".repeat(dots)))
                    .broadcastActionbar()

                delay(500)
                dots++
            }
        }
    }

    override suspend fun runPhase(): AbstractGamePhase {
        info("开始随机游戏地点...")

        val (loc, elapsed) = measureTimedValue {
            context.world.findLobbyLocation()
        }

        context.world.spawnLocation = loc

        info("大厅位置 ${loc.x} / ${loc.y} / ${loc.z}")
        info("耗时 ${elapsed.inWholeMilliseconds}ms")

        info("开始生成大厅...")
        val generated = scope.async {
            callSync {
                measureTime {
                    loc.buildLobby(
                        platform = context.config.lobby.platform,
                        wall = context.config.lobby.wall,
                        config = context.config.lobby
                    )
                }
            }
        }.await()
        info("大厅生成耗时 ${generated.inWholeMilliseconds}ms")

        context.lobbyLocation = loc

        return WaitPhase(context, scope)
    }

    override suspend fun onEnd() {
        tipJob?.cancel()

        val lobby = context.lobbyLocation ?: error("未获取到可用的大厅位置")

        scope.callSync {
            context.world.difficulty = Difficulty.PEACEFUL
            context.world.setGameRule(GameRules.FALL_DAMAGE, false)

            for (player in Bukkit.getOnlinePlayers()) {
                player.spigot().respawn()
                // 上个失明让传送看不到白茫茫的未加载区块
                player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 40, 1, false, false, false))
                player.teleport(lobby)
            }

            val border = context.world.worldBorder
            border.setCenter(lobby.x, lobby.z)
            border.size = context.config.lobby.borderSize

            // 有人想要规则介绍
            context.world.spawn(
                lobby.clone().add(0.0, 0.5, 0.0),
                TextDisplay::class.java
            ) { display ->
                display.text(
                    Component.empty()
                        .append(
                            Component.text("命运赌场")
                                .color(NamedTextColor.YELLOW)
                        )
                        .append(Component.newline())
                        .append(Component.newline())
                        .append(
                            Component.text("赌上这条性命")
                                .color(TextColor.color(139, 0, 0))
                        )
                        .append(Component.newline())
                        .append(
                            Component.text("去给高台上的看客演一出好戏吧!")
                                .color(TextColor.color(139, 0, 0))
                        )
                        .append(Component.newline())
                        .append(Component.newline())
                        .append(
                            Component.text("没有任何规则!")
                                .color(TextColor.color(255, 140, 0))
                        )
                )

                display.billboard = Display.Billboard.CENTER
                display.isSeeThrough = true
                display.backgroundColor = Color.fromARGB(0x00)
            }
        }

        try {
            postGenerated?.invoke()
        } catch (e: Throwable) {
            logEx(e, "执行 postGenerated 回调时出错")
        }

        info("初始化阶段结束")
    }

    private suspend fun World.findLobbyLocation(): Location {
        val centerX = 0
        val centerZ = 0

        val spawnY = getHighestBlockYAt(centerX, centerZ)
        val spawnBlock = getBlockAt(centerX, spawnY - 1, centerZ)

        fun findUsableLoc(startY: Int): Location? {
            var maxY = startY
            val radius = context.config.lobby.platformRadius

            for (x in centerX - radius..centerX + radius) {
                for (z in centerZ - radius..centerZ + radius) {
                    maxY = max(maxY, getHighestBlockYAt(x, z))
                }
            }

            // 抬高 20 格让玩家有视野观察附近地形
            if (maxY + context.config.lobby.wallHeight + 20.0 > maxHeight) return null

            return Location(this, centerX + 0.5, maxY + 20.0, centerZ + 0.5)
        }

        // 出生点可用
        if (spawnBlock.usable) {
            findUsableLoc(spawnY)?.let {
                return it
            }
        }

        // 出生点用不了
        val random = ThreadLocalRandom.current()
        while (true) {
            val offsetX = random.nextInt(-500, +500)
            val offsetZ = random.nextInt(-500, +500)

            val chunk = getChunkAtAsync(offsetX, offsetZ).asDeferred().await()

            val chunkBlockX = chunk.x shl 4
            val chunkBlockZ = chunk.z shl 4

            for (localX in 0..15) {
                for (localZ in 0..15) {
                    val posX = chunkBlockX + localX
                    val posZ = chunkBlockZ + localZ
                    val posY = getHighestBlockYAt(posX, posZ)

                    val block = getBlockAt(posX, posY, posZ)
                    if (!block.usable) continue

                    val loc = findUsableLoc(posY) ?: continue
                    return loc
                }
            }
        }
    }

    private val Block.usable: Boolean
        get() = !isLiquid && !isEmpty

//        val random = ThreadLocalRandom.current()
//
//        while (true) {
//            // 掏一个远点的坐标
//            val originX = random.nextInt(-500_000, 500_000)
//            val originZ = random.nextInt(-500_000, 500_000)
//
//            // 等待区块加载完毕
//            val chunk = getChunkAtAsync(originX, originZ).asDeferred().await()
//
//            // 乘 16 取区块左下角位置
//            val chunkBlockX = chunk.x shl 4
//            val chunkBlockZ = chunk.z shl 4
//
//            // 取区块中的最高点作为平台基准高度
//            for (localX in 0..15) {
//                for (localZ in 0..15) {
//                    val posX = chunkBlockX + localX
//                    val posZ = chunkBlockZ + localZ
//                    val posY = getHighestBlockYAt(posX, posZ)
//
//                    // 取最高点的方块
//                    val block = getBlockAt(posX, posY - 1, posZ)
//                    // 如果是 水/岩浆/空气 之类的就换一个点
//                    if (block.isLiquid || block.isEmpty) continue
//
//                    // 如果附近存在更高的点就去用更高的
//                    // 若 posY 的结果在半山腰 这样就能尽量避免变成山顶洞人
//                    var platformMaxY = posY
//                    for (x in posX - 10..posX + 10) {
//                        for (z in posZ - 10..posZ + 10) {
//                            platformMaxY = max(platformMaxY, getHighestBlockYAt(x, z))
//                        }
//                    }
//
//                    if (platformMaxY + 40 > maxHeight) continue
//
//                    return Location(this, posX + 0.5, platformMaxY + 20.0, posZ + 0.5)
//                }
//            }
//        }
//    }


}