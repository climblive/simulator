package app.climblive.simulator.dto

import java.time.OffsetDateTime

data class TickDto (
    var id: Int?,
    var timestamp: OffsetDateTime?,
    var contenderId: Int,
    var problemId: Int,
    var isFlash: Boolean) {
}