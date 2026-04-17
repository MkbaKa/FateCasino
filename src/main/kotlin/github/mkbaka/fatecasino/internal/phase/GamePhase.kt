package github.mkbaka.fatecasino.internal.phase

import kotlin.math.roundToLong

/**
 * @param [timeRangeMS] 从和平结束后开始算的阶段区间
 */
enum class GamePhase(val timeRangeMS: LongRange) {

    // 前一分半
    EARLY(0L..sec2MS(1.5)),

    // 一分半到六分
    MID(sec2MS(1.5)..sec2MS(6.0)),

    // 六分钟以后
    // 应该不会一把游戏开一个小时吧()
    LATE(sec2MS(6.0)..sec2MS(60.0)),

    ;

}

private fun sec2MS(sec: Double) = (sec * 60 * 1000).roundToLong()