package github.mkbaka.fatecasino.internal.game

import java.util.*
import java.util.concurrent.ConcurrentHashMap

object CallbackManager {

    private val callbacks: MutableMap<String, () -> Unit> = ConcurrentHashMap()
    private val groups: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()

    /**
     * 创建回调
     * 若指定组名 则执行同组中任意一个回调时 删除其他同组回调
     *
     * @param [source]
     * @param [group]
     * @param [callback]
     */
    fun register(
        source: String,
        group: String? = null,
        callback: () -> Unit,
    ) {
        callbacks[source] = callback

        if (group != null) {
            groups.computeIfAbsent(group) { hashSetOf() }.add(source)
        }
    }

    /**
     * 执行回调
     * 若指定组 则删除其他同组回调
     *
     * @param [source]
     * @param [group]
     */
    fun consume(source: String, group: String? = null) {
        val callback = callbacks.remove(source) ?: return

        if (group != null) {
            groups[group]?.forEach { other ->
                callbacks.remove(other)
            }
        }

        callback.invoke()
    }

    fun createGroup(): String = "group-${UUID.randomUUID()}"

}