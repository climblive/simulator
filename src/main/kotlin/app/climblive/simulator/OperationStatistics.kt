package app.climblive.simulator

import kotlin.math.max
import kotlin.math.min

data class OperationStatistics(
    val operation: Operation?,
    var total: Long = 0L,
    var samples: Long = 0L,
    var min: Long = Long.MAX_VALUE,
    var max: Long = 0) {

    fun addSample(millis: Long) {
        min = min(min, millis)
        max = max(max, millis)
        samples += 1
        total += millis
    }

    fun getAverage(): Double {
        return if (samples == 0L) {
            0.0
        } else {
            total.toDouble() / samples
        }
    }
}