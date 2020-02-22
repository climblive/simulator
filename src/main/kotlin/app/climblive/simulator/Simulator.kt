package app.climblive.simulator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.asciitable.CWC_LongestLine
import de.vandermeer.asciithemes.u8.U8_Grids
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import java.io.File
import java.math.RoundingMode
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis


object Configuration {
    var apiUrl = "https://api.test.climblive.app"
    var minDelay = 0L
    var maxDelay = 0L
    var removeOnly = false
    var forceRegister = false
}

class Simulator : CliktCommand() {
    val apiUrl: String by option("--api-url", help = "API URL").default("https://api.test.climblive.app")
    val minDelay: Long by option("--delay-min", help = "Minimum delay in milliseconds").long().default(1000L)
    val maxDelay: Long by option("--delay-max", help = "Maximum delay in milliseconds").long().default(5000L)
    val contenderLimit: Int? by option("-l", "--contender-limit", help = "Contender limit").int()
    val runTime: Long by option("-r", "--run-time", help = "Maximum run time in seconds").long().default(60)
    val removeOnly: Boolean by option("--remove-only", help = "Only remove ticks").flag(default = false)
    val forceRegister: Boolean by option("--force-register", help = "Force register even if already registered").flag(default = false)
    val contenderFile: String by argument("input", help = "Registration codes")

    private val statistics = mutableMapOf<Operation, OperationStatistics>()

    override fun run() {
        Configuration.apiUrl = apiUrl
        Configuration.minDelay = minDelay
        Configuration.maxDelay = maxDelay
        Configuration.removeOnly = removeOnly
        Configuration.forceRegister = forceRegister

        runBlocking {
            val reportsChannel = Channel<OperationReport>()
            val contenders = mutableListOf<Contender>()
            val contendersReady: MutableList<String> = mutableListOf()

            var registrationCodes: MutableList<String> = mutableListOf()
            File(contenderFile).forEachLine { registrationCodes.add(it) }
            contenderLimit?.let { registrationCodes = registrationCodes.subList(0, it) }

            val comRadio = BroadcastChannel<Command>(registrationCodes.size)
            registrationCodes.forEach { contenders.add(Contender(it, reportsChannel, comRadio)) }

            val statisticsReceived = async {
                receiveStats(reportsChannel)
            }

            val simulations = contenders.map {
                GlobalScope.launch {
                    it.start()
                }
            }

            val millis = measureTimeMillis {
                val subscription = comRadio.openSubscription()
                for (command in subscription) {
                    if (command.command == Command.CommandType.READY_TO_CLIMB) {
                        contendersReady.add(command.registrationCode)

                        if (contendersReady.size == contenders.size) {
                            break
                        }
                    }
                }
            }

            println("${contenders.size} contenders entered the contest in ${millis}ms")

            registrationCodes.forEach {
                comRadio.send(Command(it, Command.CommandType.START_CLIMBING))
            }

            val simulation = async {
                delay(runTime * 1000)
                simulations.forEach { it.cancel() }
                simulations.forEach { it.join() }

                statisticsReceived.cancel()
                statisticsReceived.join()
            }

            simulation.await()

            val total = OperationStatistics(null)

            val at = AsciiTable()
            at.getContext().setGrid(U8_Grids.borderDoubleLight())
            at.getRenderer().setCWC(CWC_LongestLine())
            at.addRule()
            at.addRow("Operation", "Samples", "Min", "Max", "Average").setPaddingLeft(1).setPaddingRight(1)
            at.addRule()

            for (stats in statistics.values) {
                at.addRow(
                    "${stats.operation?.method} ${stats.operation?.path}",
                    stats.samples,
                    "${stats.min}ms",
                    "${stats.max}ms",
                    "${stats.getAverage().toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)}ms")
                    .setPaddingLeft(1).setPaddingRight(1)
                at.addRule()

                total.min = min(total.min, stats.min)
                total.max = max(total.max, stats.max)
                total.samples += stats.samples
                total.total += stats.total
            }

            at.addRow(
                "Total",
                total.samples,
                "${total.min}ms",
                "${total.max}ms",
                "${total.getAverage().toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)}ms")
                .setPaddingLeft(1).setPaddingRight(1)
            at.addRule()

            println(at.render())
        }
    }

    private suspend fun receiveStats(channel: Channel<OperationReport>) {
        while (true) {
            var numOperations = 0
            var totalTime = 0L

            try {
                withTimeout(1000) {
                    for (report in channel) {
                        numOperations += 1
                        totalTime += report.millis

                        statistics.putIfAbsent(report.operation, OperationStatistics(report.operation))
                        statistics[report.operation]?.addSample(report.millis)
                    }
                }
            } catch (ex: TimeoutCancellationException) {
            }

            if (numOperations == 0) {
                continue
            }

            println("${numOperations.toString().padStart(5)} OPS averaging @ ${totalTime / numOperations}ms")
        }
    }
}

fun main(args: Array<String>) = Simulator().main(args)