package app.climblive.simulator.dto

data class ProblemDto (
    var id: Int?,
    var colorId: Int,
    var color: ColorDto?,
    var contestId: Int,
    var number: Int,
    var name: String?,
    var points: Int,
    var flashBonus: Int?)