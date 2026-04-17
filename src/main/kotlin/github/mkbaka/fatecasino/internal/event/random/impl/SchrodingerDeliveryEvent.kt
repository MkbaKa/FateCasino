package github.mkbaka.fatecasino.internal.event.random.impl

import github.mkbaka.fatecasino.internal.event.random.RandomEvent
import github.mkbaka.fatecasino.internal.phase.GamePhase
import github.mkbaka.fatecasino.internal.phase.data.GameContext
import github.mkbaka.fatecasino.internal.util.ServerThreadDispatcher
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.buildItem
import github.mkbaka.fatecasino.internal.util.callSync
import github.mkbaka.fatecasino.internal.util.countdownBossBar
import github.mkbaka.fatecasino.internal.util.giveItem
import github.mkbaka.fatecasino.internal.util.on
import github.mkbaka.fatecasino.internal.util.playerOrNull
import github.mkbaka.fatecasino.internal.util.sendMessage
import github.mkbaka.fatecasino.internal.util.unregisterListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.Chest
import org.bukkit.entity.Creeper
import org.bukkit.entity.EntityType
import org.bukkit.entity.Spider
import org.bukkit.entity.Zombie
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import org.checkerframework.checker.units.qual.m
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

object SchrodingerDeliveryEvent : RandomEvent {

    override val displayName: Component
        get() = Component.text("薛定谔的快递").color(NamedTextColor.DARK_PURPLE)

    override val phase: GamePhase
        get() = GamePhase.MID

    override val weight: Int = 3

    override suspend fun execute(context: GameContext, scope: CoroutineScope) {
        val random = ThreadLocalRandom.current()
        val active = context.playerSessions.values.filter { it.isActive }

        // 好东西和坏东西
        val goodItems = listOf(
            Material.DIAMOND_SWORD, Material.IRON_CHESTPLATE,
            Material.GOLDEN_APPLE, Material.DIAMOND_PICKAXE,
        )
        val badMobs = listOf(
            Creeper::class.java, Zombie::class.java, Spider::class.java
        )

        // 每个活跃玩家面前刷一个箱子
        val chestLocations = mutableMapOf<UUID, Location>()
        val chestOwners = mutableMapOf<Location, UUID>() // 反向映射: 箱子位置 -> 玩家UUID

        scope.callSync {
            for (session in active) {
                val player = session.playerOrNull ?: continue
                val direction = player.location.direction.normalize()
                val offset = Vector(direction.x * 2, 0.0, direction.z * 2)
                val loc = player.location.clone().add(offset)

                val chestBlock = loc.block
                chestBlock.type = Material.CHEST
                val chest = chestBlock.state as Chest

                // 随机填充内容
                chest.customName(Component.text("薛定谔的快递").color(NamedTextColor.DARK_PURPLE))
                chest.update()

                chestLocations[session.owner] = chestBlock.location
                chestOwners[chestBlock.location] = session.owner // 记录箱子主人
            }
        }

        Component.empty()
            .append(Component.text("薛定谔的快递").color(NamedTextColor.DARK_PURPLE))
            .append(Component.newline())
            .append(Component.text("   每人面前多了一个箱子, 打开有惊喜...").color(NamedTextColor.GRAY))
            .broadcast()

        Sound.BLOCK_CHEST_OPEN.broadcast(SoundCategory.MASTER, 1.0f, 0.5f)

        val tempListener = object : Listener {}
        val openedChests = mutableSetOf<Location>() // 改为记录已打开的箱子位置

        // 先注册监听器
        on<PlayerInteractEvent>(listener = tempListener) {
            if (hand != EquipmentSlot.HAND) return@on
            if (action != Action.RIGHT_CLICK_BLOCK) return@on

            val clickedBlock = clickedBlock ?: return@on
            if (clickedBlock.type != Material.CHEST) return@on

            val playerUUID = player.uniqueId
            val session = context.playerSessions[playerUUID]
            if (session == null || !session.isActive) return@on

            // 检查这个箱子是否已经被打开过（基位置）
            val chestLoc = clickedBlock.location
            if (chestLoc in openedChests) return@on
            openedChests.add(chestLoc)

            // 获取箱子主人的信息
            val chestOwnerUUID = chestOwners[clickedBlock.location]
            val chestOwnerName = chestOwnerUUID?.playerOrNull?.name ?: player.name

            if (random.nextBoolean()) {
                // 好东西: 把奖励塞进玩家背包
                val item = buildItem(goodItems.random())
                player.giveItem(item)

                Component.empty()
                    .append(Component.text("恭喜! ").color(NamedTextColor.GREEN))
                    .append(Component.text(chestOwnerName).color(NamedTextColor.YELLOW))
                    .append(Component.text(" 的快递里是宝贝!").color(NamedTextColor.GREEN))
                    .sendMessage(player)
            } else {
                // 坏东西: 刷 2 只怪物
                val loc = clickedBlock.location.clone().add(0.0, 1.0, 0.0)
                repeat(2) {
                    loc.world.spawn(loc, badMobs.random()) { mob ->
                        mob.customName(Component.text("薛定谔的惊喜").color(NamedTextColor.RED))
                    }
                }

                Component.empty()
                    .append(Component.text("不幸! ").color(NamedTextColor.RED))
                    .append(Component.text(chestOwnerName).color(NamedTextColor.YELLOW))
                    .append(Component.text(" 的快递里是怪物!").color(NamedTextColor.RED))
                    .sendMessage(player)

                Sound.ENTITY_CREEPER_HURT.broadcast(SoundCategory.MASTER, 0.5f, 1.0f)
            }

            // 移除箱子
            clickedBlock.type = Material.AIR
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
            for ((uuid, loc) in chestLocations) {
                if (loc !in openedChests && loc.block.type == Material.CHEST) {
                    loc.block.type = Material.AIR
                }
            }
        }

        Component.empty()
            .append(Component.text("薛定谔的快递").color(NamedTextColor.DARK_PURPLE))
            .append(Component.newline())
            .append(Component.text("   快递结束了...").color(NamedTextColor.GRAY))
            .broadcast()
    }

}