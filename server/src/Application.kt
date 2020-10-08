package ar.com.florius

import ar.com.florius.koncierge.Koncierge
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.mustachejava.DefaultMustacheFactory
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.mustache.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Mustache) {
        mustacheFactory = DefaultMustacheFactory("templates/mustache")
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    val koncierge = Koncierge()

    routing {
        get("/") {
            val parameters = call.parameters
            val context = parameters["context"] ?: "{}"
            val experiment = parameters["experiment"] ?: experiments.random()

            call.respondTemplate(
                "index.hbs",
                mapOf("experiment" to experiment, "context" to context)
            )
        }

        post("/api/eval") {
            val parameters = call.receive<EvalBody>()

            val exp = koncierge.parse(parameters.experiment)
            val result = koncierge.evaluate(exp, parameters.context).toMatrix()

            call.respond(result)
        }

        static("/static") {
            resources("static")
        }
    }
}

val experiments: List<String> = listOf(
    """
        {
            "EXP001": {
                "${'$'}children": {
                    "participating": { "${'$'}rand": { "${'$'}gt": 0.5 } },
                    "control": { "${'$'}always":  true }
                }
            }
        }
    """.trimIndent()
)

data class EvalBody(val context: String, val experiment: String)