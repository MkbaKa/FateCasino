package github.mkbaka.fatecasino.internal.util

import github.mkbaka.fatecasino.internal.event.random.impl.SpiritAvatarEvent
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

val UUID.player: Player
    get() = playerOrNull!!

val UUID.playerOrNull: Player?
    get() = Bukkit.getPlayer(this)

fun resetAll() {
    for (player in Bukkit.getOnlinePlayers()) {
        player.reset()
    }
}

fun Player.reset() {
    gameMode = GameMode.SURVIVAL
    inventory.clear()
    health = maxHealth
    level = 0
    totalExperience = 0
    foodLevel = 20

    for (effect in activePotionEffects) {
        removePotionEffect(effect.type)
    }

    SpiritAvatarEvent.cleanup(this)
}

fun Player.giveItem(itemStack: ItemStack) {
    for ((_, item) in inventory.addItem(itemStack)) {
        world.dropItem(location, item)
    }
}