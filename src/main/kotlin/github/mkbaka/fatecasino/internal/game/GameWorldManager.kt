package github.mkbaka.fatecasino.internal.game

import github.mkbaka.fatecasino.internal.util.info
import github.mkbaka.fatecasino.internal.util.logEx
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.WorldType

object GameWorldManager {

    private val warmupWorlds: MutableList<World> = mutableListOf()
    private val createdWorlds: MutableList<World> = mutableListOf()
    private var warmupIndex: Int = 0

    private const val WORLD_PREFIX = "fatecasino_game_"

    /**
     * 非固定结构的随机世界似乎只能同步创建
     * 每次开始游戏才创建可能会导致卡顿影响体验
     * 塞到插件加载阶段提前创建几个世界算了
     */
    fun warmup(count: Int) {
        repeat(count) {
            val world = createGameWorld()
            warmupWorlds.add(world)
            info("成功预热世界 ${world.name} , 当前可用数量 (${warmupWorlds.size}/${count})")
        }
    }

    fun getGameWorld(): World {
        // 如果提前创建的世界用完了 那就只能重新创建了
        if (warmupIndex >= warmupWorlds.size) {
            val world = createGameWorld()
            info("使用全新世界 ${world.name}")
            return world
        }

        val world = warmupWorlds[warmupIndex++]
        info("使用预热世界 ${world.name}")
        return world
    }

    fun createGameWorld(): World {
        val worldName = WORLD_PREFIX + (warmupWorlds.size + 1)

        info("尝试创建世界 $worldName")

        val creator = WorldCreator(worldName)
            .type(WorldType.NORMAL)
            .generateStructures(true)
            .hardcore(false)

        val world = Bukkit.createWorld(creator) ?: error("世界创建失败")
        createdWorlds.add(world)

        info("世界创建成功 $worldName")
        return world
    }

    fun deleteGameWorld(world: World) {
        if (!isGameWorld(world)) error("尝试删除非游戏世界 ${world.name}")

        info("尝试删除世界 ${world.name}")

        deleteWorld(world)

        warmupWorlds.remove(world)
        createdWorlds.remove(world)

        info("世界删除成功 ${world.name}")
    }

    fun cleanup() {
        info("尝试清理 ${createdWorlds.size} 个游戏世界")
        for (world in createdWorlds) {
            deleteWorld(world)
        }
        warmupWorlds.clear()
        createdWorlds.clear()
        info("清理成功")
    }

    fun isGameWorld(world: World) = world.name.startsWith(WORLD_PREFIX)

    private fun deleteWorld(world: World) {
        val mainWorld = Bukkit.getWorld("world") ?: error("无法获取主世界")

        teleportPlayers(world, mainWorld)

        Bukkit.unloadWorld(world, false)
        deleteWorldFolder(world)
    }

    private fun deleteWorldFolder(world: World) {
        val folder = world.worldFolder
        if (!folder.exists()) return

        try {
            folder.deleteRecursively()
        } catch (e: Throwable) {
            logEx(e, "尝试删除世界文件时出错")
        }
    }

    private fun teleportPlayers(from: World, to: World) {
        for (player in from.players) {
            player.teleport(to.spawnLocation)
        }
    }

}