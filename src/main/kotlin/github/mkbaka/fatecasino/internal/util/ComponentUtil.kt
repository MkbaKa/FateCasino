package github.mkbaka.fatecasino.internal.util

import github.mkbaka.fatecasino.internal.game.CallbackManager
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import java.util.UUID

fun Component.sendMessage(player: Player) {
    player.sendMessage(this)
}

fun Component.sendActionBar(player: Player) {
    player.sendActionBar(this)
}

fun Title.sendTitle(player: Player) {
    player.showTitle(this)
}

fun Sound.playSound(
    player: Player,
    category: SoundCategory,
    volume: Float,
    pitch: Float
) {
    player.playSound(player.location, this, category, volume, pitch)
}

fun Component.broadcast() {
    for (player in Bukkit.getOnlinePlayers()) {
        player.sendMessage(this)
    }
}

fun Component.broadcastActionbar() {
    for (player in Bukkit.getOnlinePlayers()) {
        player.sendActionBar(this)
    }
}

fun Title.broadcast() {
    for (player in Bukkit.getOnlinePlayers()) {
        player.showTitle(this)
    }
}

fun Sound.broadcast(
    category: SoundCategory,
    volume: Float,
    pitch: Float
) {
    for (player in Bukkit.getOnlinePlayers()) {
        player.playSound(player.location, this, category, volume, pitch)
    }
}

fun broadcastResetTitle() {
    for (player in Bukkit.getOnlinePlayers()) {
        player.resetTitle()
    }
}

fun Component.onClick(
    prefix: String = "non-prefix",
    group: String? = null,
    block: () -> Unit,
): Component {
    val uuid = UUID.randomUUID()
    val source = "${prefix}-$uuid"
    CallbackManager.register(source, group, block)
    val command = if (group != null) "fatecasino accept $source $group" else "fatecasino accept $source"
    return clickEvent(ClickEvent.runCommand(command))
}

/**
 * 基于文本生成一个打字机效果的 Component 序列
 *
 * @param text 要显示的文本内容
 * @param componentBuilder 字符渲染器 接收字符和索引 返回该字符的 Component
 * @param nextCharBuilder 下一帧字符的特殊渲染器 若为 null 则不添加特殊效果帧 只逐字显示
 *
 * @return Component 序列
 */
fun createSequence(
    text: String,
    componentBuilder: (char: Char, index: Int) -> Component,
    nextCharBuilder: ((char: Char, index: Int) -> Component)? = null
): List<Component> {
    val components = mutableListOf<Component>()
    val chars = text.toCharArray()

    for (i in chars.indices) {
        val revealedText = text.substring(0, i)
        val nextChar = chars[i]

        // 下一帧文本构建
        if (nextCharBuilder != null) {
            var preview = Component.empty()
            if (revealedText.isNotEmpty()) {
                for ((j, c) in revealedText.withIndex()) {
                    preview = preview.append(componentBuilder(c, j))
                }
            }
            preview = preview.append(nextCharBuilder(nextChar, i))
            components.add(preview)
        }

        // 正常显示帧
        var content = Component.empty()
        for ((j, c) in (revealedText + nextChar).withIndex()) {
            content = content.append(componentBuilder(c, j))
        }
        components.add(content)
    }

    return components
}