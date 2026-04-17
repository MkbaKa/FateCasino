package github.mkbaka.fatecasino

import github.mkbaka.fatecasino.internal.game.GameManager
import github.mkbaka.fatecasino.internal.game.GameWorldManager
import github.mkbaka.fatecasino.internal.menu.Menu
import github.mkbaka.fatecasino.internal.misc.ChipItem
import github.mkbaka.fatecasino.internal.util.AsyncDispatcher
import org.bukkit.plugin.java.JavaPlugin

class FateCasino : JavaPlugin() {

    override fun onEnable() {
        val warmupCount = if (isDev) 1 else 5
        GameWorldManager.warmup(warmupCount)

        GameManager.start()
    }

    override fun onDisable() {
        GameManager.stop()
        AsyncDispatcher.close()
    }

    init {
        INSTANCE = this
    }

    companion object {

        lateinit var INSTANCE: FateCasino

        val isDev: Boolean
            get() = INSTANCE.pluginMeta.version.endsWith("-dev")

    }

}