package github.mkbaka.fatecasino.internal.gui.slotmachine

import github.mkbaka.fatecasino.internal.event.slotmachine.RewardEvent
import github.mkbaka.fatecasino.internal.event.slotmachine.RewardLevel
import github.mkbaka.fatecasino.internal.event.slotmachine.SlotMachinePhase
import github.mkbaka.fatecasino.internal.game.CallbackManager
import github.mkbaka.fatecasino.internal.game.GameManager
import github.mkbaka.fatecasino.internal.menu.Menu
import github.mkbaka.fatecasino.internal.menu.menu
import github.mkbaka.fatecasino.internal.phase.data.PlayerSession
import github.mkbaka.fatecasino.internal.util.buildItem
import github.mkbaka.fatecasino.internal.util.onClick
import github.mkbaka.fatecasino.internal.util.sendMessage
import github.mkbaka.fatecasino.internal.util.broadcast
import github.mkbaka.fatecasino.internal.util.callSync
import github.mkbaka.fatecasino.internal.util.playSound
import github.mkbaka.fatecasino.internal.util.sendTitle
import github.mkbaka.fatecasino.internal.util.session
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player

class SlotMachineSession(
    private val player: Player,
) {

    private val scope get() = GameManager.currentPhase!!.scope

    private val session: PlayerSession get() = player.session!!

    private var phase: SlotMachinePhase = SlotMachinePhase.BAIT

    private var greedLost: Boolean = false

    private var animation: SlotMachineAnimation? = null

    private var animationJob: Job? = null

    private var menu: Menu? = null

    fun start() {
        openAndSpin()
    }

    private fun openAndSpin() {
        menu = buildMenu()
        animation = SlotMachineAnimation(menu!!, phase, player).init()
        menu!!.open(player)
    }

    private fun buildMenu(): Menu {
        return menu {
            pattern(
                "#########",
                "##R#R#R##",
                "##R#R#R##",
                "##R#R#R##",
                "S#######S"
            )

            slot(
                '#', buildItem(
                    material = phase.borderMaterial,
                    meta = { customName(Component.empty()) }
                )
            )

            slot(
                'S', buildItem(
                    material = Material.MAGMA_CREAM,
                    meta = {
                        Component.empty()
                            .append(
                                Component.text("x")
                                    .color(NamedTextColor.GRAY)
                                    .decorate(TextDecoration.OBFUSCATED)
                            )
                            .append(
                                Component.text('点')
                                    .color(TextColor.color(255, 215, 0))
                            )
                            .append(
                                Component.text('击')
                                    .color(TextColor.color(255, 140, 0))
                            )
                            .append(
                                Component.text('启')
                                    .color(TextColor.color(220, 20, 0))
                            )
                            .append(
                                Component.text('动')
                                    .color(TextColor.color(139, 0, 0))
                                    .decorate(TextDecoration.ITALIC)
                            )
                            .append(
                                Component.text("x")
                                    .color(NamedTextColor.GRAY)
                                    .decorate(TextDecoration.OBFUSCATED)
                            )
                            .let { customName(it) }
                    }
                )
            ) {
                if (animation!!.executed) return@slot

                animationJob = scope.launch {
                    val (event, level) = animation!!.run(0.1f)
                    callSync { onSpinComplete(event, level) }
                }
            }

            onClose {
                animationJob?.cancel()
            }
        }
    }

    private fun onSpinComplete(event: RewardEvent?, level: RewardLevel) {
        if (event != null) {
            Component.empty()
                .append(Component.text("[命运赌场] ").color(NamedTextColor.GOLD))
                .append(
                    if (level == RewardLevel.JACKPOT) {
                        Component.text("大奖! ").color(NamedTextColor.GOLD)
                    }
                    else {
                        Component.empty()
                    }
                )
                .append(event.name)
                .sendMessage(player)

            event.execute(player, level, phase)
        } else {
            // 从阶段惩罚列表中随机抽取一个执行
            val random = phase.defaultPunishments.randomOrNull()

            Component.empty()
                .append(Component.text("[命运赌场] ").color(NamedTextColor.GOLD))
                .append(Component.text("命运交错...").color(NamedTextColor.GRAY))
                .append(
                    if (random != null) {
                        Component.text("奖励落空, 惩罚降临.").color(NamedTextColor.GRAY)
                    } else {
                        Component.text("你什么都没抽中.").color(NamedTextColor.GRAY)
                    }
                )
                .sendMessage(player)

            random?.execute(player, RewardLevel.LOSE, phase)
        }

        // 被事件整死了 不弹提示
        if (!player.isValid) return

        scope.launch {
            delay(1000)
            callSync { player.closeInventory() }
            // 要是能放图片就好了
            // 天使 人 恶魔.jpg
            sendDecision(level)
        }
    }

    private fun sendDecision(level: RewardLevel) {
        when (phase) {
            SlotMachinePhase.BAIT -> sendBaitDecision()
            SlotMachinePhase.GREED -> {
                if (level == RewardLevel.LOSE) {
                    greedLost = true
                    sendGreedLoseDecision()
                }
            }
        }
    }

    private fun sendBaitDecision() {
        val context = GameManager.currentPhase?.context ?: return

        val group = CallbackManager.createGroup()

        val leave = Component.empty()
            .append(Component.text("[收手离开]").color(NamedTextColor.GREEN))
            .onClick("slot", group) {
                Sound.BLOCK_NOTE_BLOCK_CHIME.playSound(player, SoundCategory.MASTER, 1.0f, 1.0f)
                Component.empty()
                    .append(Component.text("[命运赌场] ").color(NamedTextColor.GOLD))
                    .append(Component.text("你带着奖品离开了...明智? 还是懦弱?").color(NamedTextColor.GRAY))
                    .sendMessage(player)
            }

        val require = (context.priceOverride ?: 1) * context.priceMultiplier

        val continueBtn = Component.empty()
            .append(Component.text("[消耗"))
            .append(Component.text(require).color(NamedTextColor.YELLOW))
            .append(Component.text("券继续]"))
            .onClick("slot", group) {
                session.consumeThen(
                    require = require,
                    then = {
                        Sound.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(player, SoundCategory.MASTER, 1.0f, 1.0f)
                        phase = SlotMachinePhase.GREED
                        openAndSpin()
                    },
                    deny = {
                        Sound.ENTITY_VILLAGER_TRADE.playSound(player, SoundCategory.MASTER, 1.0f, 1.0f)
                        Component.text("命运券不足").color(NamedTextColor.RED).sendMessage(player)
                    }
                )
            }

        Component.empty()
            .append(Component.text("━━━ 赌徒之魂 ━━━").color(NamedTextColor.GOLD))
            .append(Component.newline())
            .append(Component.text("你赢得了奖品!").color(NamedTextColor.GREEN))
            .append(Component.newline())
            .append(Component.text("你感觉运气正旺...要不要再来一次?").color(NamedTextColor.GRAY))
            .append(Component.newline())
            .append(leave)
            .append(Component.text("    "))
            .append(continueBtn)
            .sendMessage(player)
    }

    private fun sendGreedLoseDecision() {
        val context = GameManager.currentPhase?.context ?: return

        val group = CallbackManager.createGroup()

        val flee = Component.empty()
            .append(Component.text("[带着残血逃跑]").color(NamedTextColor.GRAY))
            .decorate(TextDecoration.UNDERLINED)
            .onClick("slot", group) {
                Sound.ENTITY_ENDERMAN_TELEPORT.playSound(player, SoundCategory.MASTER, 1.0f, 1.0f)
                Component.empty()
                    .append(Component.text("[命运赌场] ").color(NamedTextColor.GOLD))
                    .append(Component.text("你带着残破的身躯逃离了深渊...").color(NamedTextColor.DARK_GRAY))
                    .sendMessage(player)
            }

        val require = (context.priceOverride ?: 1) * context.priceMultiplier

        val lastChance = Component.empty()
            .append(Component.text("[最后的机会 (消耗"))
            .append(Component.text(require).color(NamedTextColor.YELLOW))
            .append(Component.text("券)]"))
            .onClick("slot", group) {
                session.consumeThen(
                    require = require,
                    then = {
                        Sound.ENTITY_TNT_PRIMED.playSound(player, SoundCategory.MASTER, 1.0f, 1.0f)
                        phase = SlotMachinePhase.ABYSS
                        openAndSpin()
                    },
                    deny = {
                        Sound.ENTITY_VILLAGER_TRADE.playSound(player, SoundCategory.MASTER, 1.0f, 1.0f)
                        Component.text("命运券不足").color(NamedTextColor.RED).sendMessage(player)
                    }
                )
            }

        Component.empty()
            .append(Component.text("━━━ 无底深渊 ━━━").color(NamedTextColor.DARK_RED))
            .append(Component.newline())
            .append(Component.text("你输掉了...命运的惩罚降临.").color(NamedTextColor.RED))
            .append(Component.newline())
            .append(Component.text("但是...命运还留了一扇门.").color(NamedTextColor.GRAY))
            .append(Component.newline())
            .append(flee)
            .append(Component.text("    "))
            .append(lastChance)
            .append(Component.newline())
            .append(Component.text("5% ").color(NamedTextColor.GREEN))
            .append(Component.text("奇迹翻盘  |  ").color(NamedTextColor.GRAY))
            .append(Component.text("95% ").color(NamedTextColor.RED))
            .append(Component.text("倾家荡产").color(NamedTextColor.GRAY))
            .sendMessage(player)
    }

}