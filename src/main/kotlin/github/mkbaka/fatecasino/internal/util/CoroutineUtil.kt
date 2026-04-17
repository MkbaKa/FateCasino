package github.mkbaka.fatecasino.internal.util

import github.mkbaka.fatecasino.FateCasino
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

object ServerThreadDispatcher : CoroutineDispatcher() {

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return !Bukkit.isPrimaryThread()
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        Bukkit.getScheduler().runTask(FateCasino.INSTANCE, block)
    }

}

object AsyncDispatcher : CoroutineDispatcher() {

    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2)

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return Bukkit.isPrimaryThread()
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        executor.submit(block)
    }

    fun close() {
        executor.close()
    }

}

class BukkitCoroutineScope : CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext =
        job + AsyncDispatcher

    fun cancelAndShutdown() {
        job.cancel()
    }

}

suspend fun delayTick(tick: Long, plugin: Plugin = FateCasino.INSTANCE) {
    if (tick <= 0) return
    suspendCancellableCoroutine { continuation ->
        val task = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            continuation.resume(Unit)
        }, tick)
        continuation.invokeOnCancellation {
            task.cancel()
        }
    }
}

suspend fun <T> CoroutineScope.callSync(block: suspend () -> T) =
    withContext(ServerThreadDispatcher) {
        block()
    }

suspend fun <T> CoroutineScope.callAsync(block: suspend () -> T) =
    withContext(AsyncDispatcher) {
        block()
    }