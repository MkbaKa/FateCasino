package github.mkbaka.fatecasino.internal.boot

import github.mkbaka.fatecasino.internal.command.FateCasinoCommand
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents

class FateCasinoBootstrap : PluginBootstrap {

    override fun bootstrap(context: BootstrapContext) {
        context.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
            commands.registrar().register(FateCasinoCommand.main)
        }
    }

}