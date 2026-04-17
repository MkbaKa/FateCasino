package github.mkbaka.fatecasino.internal.phase.data

import github.mkbaka.fatecasino.internal.misc.ChipItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class PlayerSession(
    val owner: UUID,
    val playerName: String
) {

    /** 可能会存在协程外的主线程读写 所以不能用 Mutex */
    private val ticketLock = ReentrantReadWriteLock()

    /** 命运券数量 */
    private var ticket: Int = 0

    val player: Player
        get() = playerOrNull!!

    val playerOrNull: Player?
        get() = Bukkit.getPlayer(owner)

    var previousState: State = State.ACTIVE
        private set

    var state: State = State.ACTIVE
        set(value) {
            previousState = field
            field = value
        }

    val isActive: Boolean get() = state == State.ACTIVE
    val isSpectator: Boolean get() = state == State.SPECTATOR
    val isOffline: Boolean get() = state == State.OFFLINE

    var slotMachineUsable: Boolean = false

    fun giveTicket(operate: Int) = ticketLock.write { ticket += operate }

    fun setTicket(operate: Int) = ticketLock.write { ticket = operate }

    fun getTicket(): Int = ticketLock.read { ticket }

    fun consumeThen(
        require: Int,
        then: () -> Unit,
        deny: () -> Unit
    ) {
        ticketLock.write {
            if (ticket < require) {
                deny()
            } else {
                ticket -= require
                then()
            }
        }
    }

    // 我真没招了 难用的要死
    fun buildTicketTip(max: Int): Component {
        return Component.empty()
            .append(Component.text("(").color(NamedTextColor.GRAY))
            .append(Component.text(getTicket()).color(NamedTextColor.YELLOW))
            .append(Component.text("/").color(NamedTextColor.GRAY))
            .append(Component.text(max).color(NamedTextColor.YELLOW))
            .append(Component.text(")").color(NamedTextColor.GRAY))
    }

    fun giveChipItem() {
        player.inventory.addItem(ChipItem.getter)
    }

    enum class State { ACTIVE, SPECTATOR, OFFLINE }

}