package app.climblive.simulator.dto

import java.time.OffsetDateTime

data class ContenderDto (
    var id: Int?,
    var compClassId: Int? = null,
    var contestId: Int,
    var registrationCode: String,
    var name: String? = null,
    var entered: OffsetDateTime? = null,
    var disqualified: Boolean,
    var finalPlacing: Int?)
