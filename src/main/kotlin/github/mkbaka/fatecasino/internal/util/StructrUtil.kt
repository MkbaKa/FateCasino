package github.mkbaka.fatecasino.internal.util

import github.mkbaka.fatecasino.internal.phase.data.GameConfig
import org.bukkit.Location
import org.bukkit.Material

fun Location.buildLobby(
    platform: Material,
    wall: Material,
    config: GameConfig.LobbyConfig
) {
    val baseX = blockX
    val baseY = blockY + config.platformYOffset
    val baseZ = blockZ

    // 生成底面
    val radius = config.platformRadius
    for (x in baseX - radius..baseX + radius) {
        for (z in baseZ - radius..baseZ + radius) {
            world.getBlockAt(x, baseY, z).type = platform
        }
    }

    for (xz in -radius..radius) {
        for (y in baseY..baseY + config.wallHeight - 1) {
            // 北面
            world.getBlockAt(baseX + xz, y, baseZ - radius).type = wall
            // 南面
            world.getBlockAt(baseX + xz, y, baseZ + radius).type = wall
            // 东面
            world.getBlockAt(baseX + radius, y, baseZ + xz).type = wall
            // 西面
            world.getBlockAt(baseX - radius, y, baseZ + xz).type = wall
        }
    }
}