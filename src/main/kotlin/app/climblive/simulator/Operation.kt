package app.climblive.simulator

enum class Operation(val method: String, val path: String) {
    GetContender("GET", "/contender/findByCode?code=?"),
    GetCompClasses("GET", "/compClass"),
    GetProblems("GET", "/problem"),
    GetTicks("GET", "/tick"),
    UpdateContender("PUT", "/contender/?"),
    CreateTick("POST", "/tick"),
    UpdateTick("PUT", "/tick/?"),
    DeleteTick("DELETE", "/tick/?")
}