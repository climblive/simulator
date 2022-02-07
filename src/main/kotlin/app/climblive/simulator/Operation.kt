package app.climblive.simulator

enum class Operation(val method: String, val path: String) {
    GetContender("GET", "/contender/findByCode?code={code}"),
    GetCompClasses("GET", "/contest/{id}/compClass"),
    GetProblems("GET", "/contest/{id}/problem"),
    GetTicks("GET", "/contender/{id}/tick"),
    UpdateContender("PUT", "/contender/{id}"),
    CreateTick("POST", "/contender/{id}/tick"),
    UpdateTick("PUT", "/tick/{id}"),
    DeleteTick("DELETE", "/tick/{id}")
}