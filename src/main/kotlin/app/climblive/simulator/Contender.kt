package app.climblive.simulator

import app.climblive.simulator.dto.CompClassDto
import app.climblive.simulator.dto.ContenderDto
import app.climblive.simulator.dto.ProblemDto
import app.climblive.simulator.dto.TickDto
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class Contender(private val registrationCode: String, private val reportsChannel: Channel<OperationReport>, private val comRadio: BroadcastChannel<Command>) {

    class Problems(var done: MutableMap<Int, ProblemDto> = mutableMapOf(), var todo: MutableMap<Int, ProblemDto> = mutableMapOf())

    private var contender: ContenderDto? = null
    private var compClasses: List<CompClassDto> = emptyList()
    private var ticks: MutableMap<Int, TickDto> = mutableMapOf()
    private val problems: Problems = Problems()

    suspend fun start() {
        loadData()
        enterContest()

        comRadio.send(Command(registrationCode, Command.CommandType.READY_TO_CLIMB))

        val subscription = comRadio.openSubscription()
        for (command in subscription) {
            if (command.command == Command.CommandType.START_CLIMBING && command.registrationCode == registrationCode) {
                break
            }
        }

        applyDelay()

        while (true) {
            if (Configuration.removeOnly) {
                if (problems.done.isEmpty()) {
                    return
                }

                removeTick()
                continue
            }

            if (Random.nextInt(0, 100) == 0) {
                loadData()
                continue
            }

            when (Action.values().random()) {
                Action.ADD_TICK -> addTick()
                Action.UPDATE_TICK -> updateTick()
                Action.REMOVE_TICK -> removeTick()
                Action.SWITCH_CLASS -> switchClass()
                else -> {}
            }
        }
    }

    suspend fun loadData() {
        runOperation(Operation.GetContender, false) {
            contender = Fuel.get("${Configuration.apiUrl}/contender/findByCode?code=$registrationCode")
                .header("Authorization", "Regcode $registrationCode")
                .awaitObjectResult(Deserializers.ContenderDeserializer)
                .get()
        }

        runOperation(Operation.GetCompClasses, false) {
            compClasses = Fuel.get("${Configuration.apiUrl}/compClass")
                .header("Authorization", "Regcode $registrationCode")
                .awaitObjectResult(Deserializers.CompClassListDeserializer)
                .get()
        }

        runOperation(Operation.GetTicks, false) {
            ticks = Fuel.get("${Configuration.apiUrl}/tick")
                .header("Authorization", "Regcode $registrationCode")
                .awaitObjectResult(Deserializers.TickListDeserializer)
                .get()
                .associateBy { it.problemId }.toMutableMap()
        }

        runOperation(Operation.GetProblems, false) {
            Fuel.get("${Configuration.apiUrl}/problem")
                .header("Authorization", "Regcode $registrationCode")
                .awaitObjectResult(Deserializers.ProblemListDeserializer)
                .get()
                .forEach {
                    if (it.id in ticks) {
                        problems.done
                    } else {
                        problems.todo
                    }[it.id!!] = it
                }
        }
    }

    suspend fun enterContest() {
        if (!Configuration.forceRegister && contender?.name != null && contender?.compClassId != null) {
            return
        }

        contender?.name = fakeName()
        val compClass = compClasses.random()
        contender?.compClassId = compClass.id

        runOperation(Operation.UpdateContender, true) {
            contender = Fuel.put("${Configuration.apiUrl}/contender/${contender?.id}")
                .header("Authorization", "Regcode $registrationCode")
                .header("Content-Type", "application/json")
                .body(Deserializers.objectMapper.writeValueAsString(contender))
                .awaitObjectResult(Deserializers.ContenderDeserializer)
                .get()

            println("${contender?.registrationCode} picked name ${contender?.name} and joined class ${compClass.name} @ ${contender?.entered}")
        }
    }

    suspend fun switchClass() {
        val compClass = compClasses.random()
        contender?.compClassId = compClass.id

        runOperation(Operation.UpdateContender, true) {
            contender = Fuel.put("${Configuration.apiUrl}/contender/${contender?.id}")
                .header("Authorization", "Regcode $registrationCode")
                .header("Content-Type", "application/json")
                .body(Deserializers.objectMapper.writeValueAsString(contender))
                .awaitObjectResult(Deserializers.ContenderDeserializer)
                .get()
        }
    }

    suspend fun addTick() {
        if (problems.todo.isEmpty()) {
            return
        }

        val problem = problems.todo.values.random()
        val problemId: Int = problem.id!!

        var tick = TickDto(null, null, contender?.id!!, problemId, Random.nextBoolean())

        runOperation(Operation.CreateTick, true) {
            tick = Fuel.post("${Configuration.apiUrl}/tick")
                .header("Authorization", "Regcode $registrationCode")
                .header("Content-Type", "application/json")
                .body(Deserializers.objectMapper.writeValueAsString(tick))
                .awaitObjectResult(Deserializers.TickDeserializer)
                .get()
        }

        ticks[problemId] = tick
        problems.todo.remove(problemId)
        problems.done[problemId] = problem
    }

    suspend fun removeTick() {
        if (problems.done.isEmpty()) {
            return
        }

        val problem = problems.done.values.random()
        val problemId: Int = problem.id!!

        var tick = ticks[problemId]

        runOperation(Operation.DeleteTick, true) {
            Fuel.delete("${Configuration.apiUrl}/tick/${tick?.id}")
                .header("Authorization", "Regcode $registrationCode")
                .awaitStringResult()
        }

        ticks.remove(problemId)
        problems.done.remove(problemId)
        problems.todo[problemId] = problem
    }

    suspend fun updateTick() {
        if (problems.done.isEmpty()) {
            return
        }

        val problem = problems.done.values.random()
        val problemId: Int = problem.id!!

        var tick: TickDto = ticks[problemId]!!
        tick.isFlash = Random.nextBoolean()

        runOperation(Operation.UpdateTick, true) {
            tick = Fuel.put("${Configuration.apiUrl}/tick/${tick.id}")
                .header("Authorization", "Regcode $registrationCode")
                .header("Content-Type", "application/json")
                .body(Deserializers.objectMapper.writeValueAsString(tick))
                .awaitObjectResult(Deserializers.TickDeserializer)
                .get()
        }

        ticks[problemId] = tick
    }

    suspend fun runOperation(operation: Operation, applyDelay: Boolean, block: suspend () -> Unit) {
        val millis = measureTimeMillis {
            block()
        }

        reportsChannel.send(OperationReport(operation, millis))

        if (applyDelay) {
            applyDelay()
        }
    }

    private suspend fun applyDelay() {
        if (Configuration.minDelay == Configuration.maxDelay) {
            if (Configuration.minDelay == 0L) {
                return
            }

            delay(Configuration.minDelay)
        } else {
            delay(Random.nextLong(Configuration.minDelay, Configuration.maxDelay))
        }
    }
}