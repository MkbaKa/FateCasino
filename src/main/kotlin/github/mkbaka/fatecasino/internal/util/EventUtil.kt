package github.mkbaka.fatecasino.internal.util

import github.mkbaka.fatecasino.FateCasino
import github.mkbaka.fatecasino.internal.event.trojan.impl.PainMask
import github.mkbaka.fatecasino.internal.menu.Menu
import github.mkbaka.fatecasino.internal.misc.ChipItem
import github.mkbaka.fatecasino.internal.misc.QuantumChestplate
import github.mkbaka.fatecasino.internal.misc.RandomSword
import github.mkbaka.fatecasino.internal.misc.SlotMachineBow
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener

fun Listener.subscribeGamingEvents() {
    // 界面监听
    Menu.subscribe(listener = this)

    // 物品效果
    PainMask.subscribe(listener = this)
    ChipItem.subscribe(listener = this)
    RandomSword.subscribe(listener = this)
    SlotMachineBow.subscribe(listener = this)
    QuantumChestplate.subscribe(listener = this)
}

inline fun <reified T : Event> on(
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    listener: Listener = object : Listener {},
    noinline executor: T.() -> Unit
) {
    on(T::class.java, priority, ignoreCancelled, listener, executor)
}

fun <T : Event> on(
    eventClass: Class<T>,
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    listener: Listener = object : Listener {},
    executor: T.() -> Unit
) {
    Bukkit.getPluginManager()
        .registerEvent(
            eventClass, listener, priority, { _, event ->
                if (eventClass.isAssignableFrom(event::class.java)) {
                    executor.invoke(event as T)
                }
            }, FateCasino.INSTANCE, ignoreCancelled
        )
}

fun unregisterListener(listener: Listener) {
    HandlerList.unregisterAll(listener)
}