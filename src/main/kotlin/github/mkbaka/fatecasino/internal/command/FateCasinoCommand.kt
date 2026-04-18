package github.mkbaka.fatecasino.internal.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import github.mkbaka.fatecasino.FateCasino
import github.mkbaka.fatecasino.internal.event.slotmachine.RewardEvent
import github.mkbaka.fatecasino.internal.event.slotmachine.RewardLevel
import github.mkbaka.fatecasino.internal.event.slotmachine.SlotMachinePhase
import github.mkbaka.fatecasino.internal.game.CallbackManager
import github.mkbaka.fatecasino.internal.game.GameManager
import github.mkbaka.fatecasino.internal.phase.manager.RandomEventManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import kotlinx.coroutines.launch
import github.mkbaka.fatecasino.internal.misc.QuantumChestplate
import github.mkbaka.fatecasino.internal.misc.RandomSword
import github.mkbaka.fatecasino.internal.misc.SlotMachineBow
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType

object FateCasinoCommand {

    val main = Commands.literal("fatecasino")
        .then(
            Commands.literal("accept")
                .then(
                    Commands.argument<String>(
                        "source",
                        StringArgumentType.string()
                    ).then(
                        Commands.argument<String>(
                            "group",
                            StringArgumentType.string()
                        ).executes { context ->
                            val source = context.getArgument<String>("source", String::class.java)
                            val group = context.getArgument<String>("group", String::class.java)
                            CallbackManager.consume(source, group)

                            Command.SINGLE_SUCCESS
                        }
                    ).executes { context ->
                        val source = context.getArgument<String>("source", String::class.java)
                        CallbackManager.consume(source)

                        Command.SINGLE_SUCCESS
                    }
                )
        ).then(
            Commands.literal("event")
                .requires { FateCasino.isDev || it.sender.isOp }
                .then(
                    Commands.argument<String>(
                        "name",
                        StringArgumentType.string()
                    ).suggests { _, builder ->
                        RandomEventManager.allEvents.forEach { event ->
                            builder.suggest(event::class.simpleName)
                        }
                        builder.buildFuture()
                    }.executes { ctx ->
                        triggerEvent(ctx)
                    }
                )
        ).then(
            Commands.literal("reward")
                .requires { FateCasino.isDev || it.sender.isOp }
                .then(
                    Commands.literal("ticket")
                        .then(
                            Commands.argument<Int>("amount", IntegerArgumentType.integer(1))
                                .executes { ctx -> executeReward(ctx, RewardType.TICKET) }
                                .then(levelArgument())
                                .executes { ctx -> executeReward(ctx, RewardType.TICKET) }
                                .then(phaseArgument())
                                .executes { ctx -> executeReward(ctx, RewardType.TICKET) }
                        )
                )
                .then(
                    Commands.literal("item")
                        .then(
                            Commands.argument<String>("material", StringArgumentType.string())
                                .suggests { _, builder ->
                                    Material.entries.forEach { builder.suggest(it.name) }
                                    builder.buildFuture()
                                }
                                .executes { ctx -> executeReward(ctx, RewardType.ITEM) }
                                .then(
                                    Commands.argument<Int>("count", IntegerArgumentType.integer(1))
                                        .executes { ctx -> executeReward(ctx, RewardType.ITEM) }
                                        .then(levelArgument())
                                        .executes { ctx -> executeReward(ctx, RewardType.ITEM) }
                                        .then(phaseArgument())
                                        .executes { ctx -> executeReward(ctx, RewardType.ITEM) }
                                )
                        )
                )
                .then(
                    Commands.literal("heal")
                        .then(
                            Commands.argument<Double>("hearts", DoubleArgumentType.doubleArg(0.5))
                                .executes { ctx -> executeReward(ctx, RewardType.HEAL) }
                                .then(levelArgument())
                                .executes { ctx -> executeReward(ctx, RewardType.HEAL) }
                                .then(phaseArgument())
                                .executes { ctx -> executeReward(ctx, RewardType.HEAL) }
                        )
                )
                .then(
                    Commands.literal("effect")
                        .then(
                            Commands.argument<String>("type", StringArgumentType.string())
                                .suggests { _, builder ->
                                    Registry.POTION_EFFECT_TYPE.forEach { builder.suggest(it.key.key) }
                                    builder.buildFuture()
                                }
                                .then(
                                    Commands.argument<Int>("duration", IntegerArgumentType.integer(1))
                                        .executes { ctx -> executeReward(ctx, RewardType.EFFECT) }
                                        .then(
                                            Commands.argument<Int>(
                                                "amplifier",
                                                IntegerArgumentType.integer(0)
                                            )
                                                .executes { ctx -> executeReward(ctx, RewardType.EFFECT) }
                                                .then(levelArgument())
                                                .executes { ctx -> executeReward(ctx, RewardType.EFFECT) }
                                                .then(phaseArgument())
                                                .executes { ctx -> executeReward(ctx, RewardType.EFFECT) }
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("damage")
                        .then(
                            Commands.argument<Double>("hearts", DoubleArgumentType.doubleArg(0.5))
                                .executes { ctx -> executeReward(ctx, RewardType.DAMAGE) }
                                .then(
                                    Commands.argument<Boolean>("clearArmor", BoolArgumentType.bool())
                                        .executes { ctx -> executeReward(ctx, RewardType.DAMAGE) }
                                        .then(levelArgument())
                                        .executes { ctx -> executeReward(ctx, RewardType.DAMAGE) }
                                        .then(phaseArgument())
                                        .executes { ctx -> executeReward(ctx, RewardType.DAMAGE) }
                                )
                        )
                )
                .then(
                    Commands.literal("special")
                        .then(
                            Commands.argument<String>("action", StringArgumentType.string())
                                .suggests { _, builder ->
                                    RewardEvent.Special.Action.entries.forEach { builder.suggest(it.name) }
                                    builder.buildFuture()
                                }
                                .executes { ctx -> executeReward(ctx, RewardType.SPECIAL) }
                                .then(levelArgument())
                                .executes { ctx -> executeReward(ctx, RewardType.SPECIAL) }
                                .then(phaseArgument())
                                .executes { ctx -> executeReward(ctx, RewardType.SPECIAL) }
                        )
                )
                .then(
                    Commands.literal("powerful")
                        .then(
                            Commands.argument<String>("type", StringArgumentType.string())
                                .suggests { _, builder ->
                                    builder.suggest("RandomSword")
                                    builder.suggest("SlotMachineBow")
                                    builder.suggest("QuantumChestplate")
                                    builder.buildFuture()
                                }
                                .executes { ctx -> executeReward(ctx, RewardType.POWERFUL) }
                                .then(levelArgument())
                                .executes { ctx -> executeReward(ctx, RewardType.POWERFUL) }
                                .then(phaseArgument())
                                .executes { ctx -> executeReward(ctx, RewardType.POWERFUL) }
                        )
                )
        )
        .build()

    private fun triggerEvent(ctx: CommandContext<CommandSourceStack>): Int {
        val name = ctx.getArgument<String>("name", String::class.java)
        val sender = ctx.source.sender

        val event = RandomEventManager.allEvents.find { it::class.simpleName == name }
        if (event == null) {
            sender.sendMessage(Component.text("未知事件: $name").color(NamedTextColor.RED))
            return Command.SINGLE_SUCCESS
        }

        val phase = GameManager.currentPhase
        if (phase == null) {
            sender.sendMessage(Component.text("游戏未在运行").color(NamedTextColor.RED))
            return Command.SINGLE_SUCCESS
        }

        // 启动协程执行事件
        phase.scope.launch {
            event.execute(phase.context, phase.scope)
        }

        sender.sendMessage(
            Component.text("已触发事件: ")
                .color(NamedTextColor.GREEN)
                .append(event.displayName)
        )

        return Command.SINGLE_SUCCESS
    }

    // ===== Reward 测试命令辅助函数 =====

    private enum class RewardType {
        TICKET, ITEM, HEAL, EFFECT, DAMAGE, SPECIAL, POWERFUL
    }

    private fun levelArgument() = Commands.argument<String>("level", StringArgumentType.string())
        .suggests { _, builder ->
            RewardLevel.entries.forEach { builder.suggest(it.name) }
            builder.buildFuture()
        }

    private fun phaseArgument() = Commands.argument<String>("phase", StringArgumentType.string())
        .suggests { _, builder ->
            listOf("BAIT", "GREED", "ABYSS").forEach { builder.suggest(it) }
            builder.buildFuture()
        }

    private fun getLevel(ctx: CommandContext<CommandSourceStack>): RewardLevel {
        return try {
            RewardLevel.valueOf(ctx.getArgument<String>("level", String::class.java))
        } catch (_: Exception) {
            RewardLevel.LOSE
        }
    }

    private fun getPhase(ctx: CommandContext<CommandSourceStack>): SlotMachinePhase {
        return try {
            when (ctx.getArgument<String>("phase", String::class.java)) {
                "GREED" -> SlotMachinePhase.GREED
                "ABYSS" -> SlotMachinePhase.ABYSS
                else -> SlotMachinePhase.BAIT
            }
        } catch (_: Exception) {
            SlotMachinePhase.BAIT
        }
    }

    private fun executeReward(ctx: CommandContext<CommandSourceStack>, type: RewardType): Int {
        val sender = ctx.source.sender as? Player ?: run {
            ctx.source.sender.sendMessage(Component.text("仅玩家可执行此命令").color(NamedTextColor.RED))
            return Command.SINGLE_SUCCESS
        }

        val level = getLevel(ctx)
        val phase = getPhase(ctx)

        val event: RewardEvent = when (type) {
            RewardType.TICKET -> {
                RewardEvent.Ticket(ctx.getArgument<Int>("amount", Int::class.java))
            }

            RewardType.ITEM -> {
                val materialName = ctx.getArgument<String>("material", String::class.java)
                val material = Material.matchMaterial(materialName) ?: Material.DIAMOND
                val count = try {
                    ctx.getArgument<Int>("count", Int::class.java)
                } catch (_: Exception) {
                    1
                }
                RewardEvent.SimpleItem(material, count)
            }

            RewardType.HEAL -> {
                RewardEvent.Heal(ctx.getArgument<Double>("hearts", Double::class.java))
            }

            RewardType.EFFECT -> {
                val typeName = ctx.getArgument<String>("type", String::class.java)
                val effectType = PotionEffectType.getByName(typeName) ?: PotionEffectType.SPEED
                val duration = try {
                    ctx.getArgument<Int>("duration", Int::class.java)
                } catch (_: Exception) {
                    10
                }
                val amplifier = try {
                    ctx.getArgument<Int>("amplifier", Int::class.java)
                } catch (_: Exception) {
                    1
                }
                RewardEvent.Effect(effectType, duration, amplifier)
            }

            RewardType.DAMAGE -> {
                val hearts = ctx.getArgument<Double>("hearts", Double::class.java)
                val clearArmor = try {
                    ctx.getArgument<Boolean>("clearArmor", Boolean::class.java)
                } catch (_: Exception) {
                    false
                }
                RewardEvent.Damage(hearts, clearArmor)
            }

            RewardType.SPECIAL -> {
                val actionName = ctx.getArgument<String>("action", String::class.java)
                val action = try {
                    RewardEvent.Special.Action.valueOf(actionName)
                } catch (_: Exception) {
                    RewardEvent.Special.Action.THUNDER_ALL
                }
                RewardEvent.Special(action)
            }

            RewardType.POWERFUL -> {
                val typeName = ctx.getArgument<String>("type", String::class.java)
                when (typeName) {
                    "RandomSword" -> RewardEvent.PowerfulItem(
                        nameSupplier = { RandomSword.name },
                        itemSupplier = { RandomSword.getter }
                    )
                    "SlotMachineBow" -> RewardEvent.PowerfulItem(
                        nameSupplier = { SlotMachineBow.name },
                        itemSupplier = { SlotMachineBow.getter }
                    )
                    "QuantumChestplate" -> RewardEvent.PowerfulItem(
                        nameSupplier = { QuantumChestplate.name },
                        itemSupplier = { QuantumChestplate.getter }
                    )
                    else -> RewardEvent.PowerfulItem(
                        nameSupplier = { RandomSword.name },
                        itemSupplier = { RandomSword.getter }
                    )
                }
            }
        }

        sender.sendMessage(
            Component.text("[测试] 触发奖励事件: ")
                .color(NamedTextColor.GREEN)
                .append(event.name)
                .append(Component.text(" | $level | $phase").color(NamedTextColor.GRAY))
        )

        event.execute(sender, level, phase)

        return Command.SINGLE_SUCCESS
    }

}