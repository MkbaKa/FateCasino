package github.mkbaka.fatecasino.internal.gui

import github.mkbaka.fatecasino.internal.phase.manager.Bounty
import github.mkbaka.fatecasino.internal.game.GameManager
import github.mkbaka.fatecasino.internal.phase.impl.PlayingPhase
import github.mkbaka.fatecasino.internal.gui.slotmachine.SlotMachineSession
import github.mkbaka.fatecasino.internal.menu.Menu
import github.mkbaka.fatecasino.internal.menu.menu
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.buildItem
import github.mkbaka.fatecasino.internal.util.playSound
import github.mkbaka.fatecasino.internal.util.sendMessage
import github.mkbaka.fatecasino.internal.util.session
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory

object CasinoGUI {

    val menu: Menu = menu {
        pattern(
            "#########",
            "##T#B#S##",
            "#########"
        )

        slot(
            '#', buildItem(
                material = Material.GRAY_STAINED_GLASS_PANE,
                meta = {
                    displayName(Component.empty())
                }
            )
        )

        slot(
            'T',
            buildItem(
                material = Material.IRON_INGOT,
                meta = {
                    Component.empty()
                        .append(Component.text("x").decorate(TextDecoration.OBFUSCATED))
                        .append(Component.text("1 券入口"))
                        .append(Component.text("x").decorate(TextDecoration.OBFUSCATED))
                        .let { customName(it) }

                    Component.empty()
                        .append(
                            Component.text("   精心包装的礼物...")
                                .color(NamedTextColor.GRAY)
                        )
                        .let { lore(listOf(it)) }
                }
            )
        ) { open(TrojanGiftGUI.menu) }

        slot(
            'B',
            buildItem(
                material = Material.GOLD_INGOT,
                meta = {
                    Component.empty()
                        .append(Component.text("悬赏入口"))
                        .color(NamedTextColor.GOLD)
                        .let { customName(it) }

                    Component.empty()
                        .append(Component.text("   发布悬赏追杀目标").color(NamedTextColor.GRAY))
                        .let { lore(listOf(it)) }
                }
            )
        ) {
            val session = player.session
            if (session == null) {
                Component.text("你不在这场游戏中")
                    .sendMessage(player)
                return@slot
            }

            open(SelectTargetGUI.createMenu(player.uniqueId) { targetId ->
                val target = Bukkit.getPlayer(targetId)
                if (target == null) {
                    Component.text("目标玩家不在线")
                        .sendMessage(player)
                    return@createMenu
                }

                open(BountyAmountGUI.createMenu(target.name) { amount ->
                    val phase = GameManager.currentPhase as? PlayingPhase
                    if (phase == null) {
                        Component.text("当前阶段无法悬赏玩家")
                            .sendMessage(player)
                        return@createMenu
                    }

                    session.consumeThen(
                        amount,
                        then = {
                            val bounty = Bounty(
                                issuerId = player.uniqueId,
                                issuerName = player.name,
                                targetId = targetId,
                                targetName = target.name,
                                amount = amount
                            )
                            phase.bountyManager.registerBounty(bounty)
                        },
                        deny = {
                            Sound.ENTITY_VILLAGER_NO.playSound(player, SoundCategory.MASTER, 1.0f, 1.0f)
                            Component.text("命运券不足").sendMessage(player)
                        }
                    )
                })
            })
        }

        slot(
            'S',
            buildItem(
                material = Material.DIAMOND,
                meta = {
                    Component.empty()
                        .append(Component.text("x").decorate(TextDecoration.OBFUSCATED))
                        .append(Component.text("神秘入口"))
                        .append(Component.text("x").decorate(TextDecoration.OBFUSCATED))
                        .let { customName(it) }

                    Component.empty()
                        .append(Component.text("   进入须先缴纳 ").color(NamedTextColor.GRAY))
                        .append(Component.text(3).color(NamedTextColor.YELLOW))
                        .append(Component.text(" 张命运券").color(NamedTextColor.GRAY))
                        .let { lore(listOf(it)) }
                }
            )
        ) {
            val session = player.session
            if (session == null) {
                Component.text("你不在这场游戏中")
                    .sendMessage(player)
                return@slot
            }

            // 没交过钱才要交钱
            // 交过就直接打开
            if (!session.slotMachineUsable) {
                val context = GameManager.currentPhase!!.context
                session.consumeThen(
                    (context.priceOverride ?: 3) * context.priceMultiplier,
                    then = {
                        session.slotMachineUsable = true
                        Sound.BLOCK_NOTE_BLOCK_BIT.broadcast(
                            SoundCategory.MASTER,
                            1.0f,
                            1.0f
                        )

                        SlotMachineSession(player).start()
                    },
                    deny = {
                        Sound.ENTITY_VILLAGER_NO.playSound(player, SoundCategory.MASTER, 1.0f, 1.0f)
                        Component.text("命运券不足").sendMessage(player)
                    }
                )
            } else {
                SlotMachineSession(player).start()
            }

        }
    }

}