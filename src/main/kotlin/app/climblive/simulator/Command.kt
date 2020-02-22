package app.climblive.simulator

data class Command(val registrationCode: String, val command: CommandType) {
    enum class CommandType {
        READY_TO_CLIMB,
        START_CLIMBING
    }
}