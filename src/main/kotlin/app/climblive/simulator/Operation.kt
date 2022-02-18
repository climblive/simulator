package app.climblive.simulator

enum class Operation(val method: String, val path: String) {
    GetContender("GET", "/contenders/findByCode?code={code}"),
    GetCompClasses("GET", "/contests/{id}/compClasses"),
    GetProblems("GET", "/contests/{id}/problems"),
    GetTicks("GET", "/contenders/{id}/ticks"),
    UpdateContender("PUT", "/contenders/{id}"),
    CreateTick("POST", "/contenders/{id}/ticks"),
    UpdateTick("PUT", "/ticks/{id}"),
    DeleteTick("DELETE", "/ticks/{id}")
}