package github.mkbaka.fatecasino.internal.util

import github.mkbaka.fatecasino.FateCasino
import java.util.logging.Level

fun info(any: Any?) {
    FateCasino.INSTANCE.logger.info(any.toString())
}

fun warning(any: Any?) {
    FateCasino.INSTANCE.logger.warning(any.toString())
}

fun error(any: Any?) {
    FateCasino.INSTANCE.logger.severe(any.toString())
}

fun logEx(throwable: Throwable, msg: String) {
    FateCasino.INSTANCE.logger.log(Level.SEVERE, msg, throwable)
}