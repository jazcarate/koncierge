package ar.com.florius

import ar.com.florius.koncierge.Koncierge
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.mustachejava.DefaultMustacheFactory
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.mustache.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
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
            var example = parameters["context"] to parameters["experiment"]

            if (example.first === null && example.second === null) {
                example = experiments.random()
            }
            val (experiment, context) = example

            call.respondTemplate(
                "index.hbs",
                mapOf("experiment" to experiment, "context" to context)
            )
        }

        post("/api/eval") {
            val parameters = call.receive<EvalBody>()

            try {
                val exp = koncierge.parse(parameters.experiment)
                val result = koncierge.evaluate(exp, parameters.context).toMatrix()

                call.respond(result)
            } catch (ex: Exception) {
                call.respondText(
                    ex.localizedMessage,
                    contentType = ContentType.Text.Plain,
                    status = HttpStatusCode.BadRequest,
                )
            }
        }

        static("/static") {
            resources("static")
        }
    }
}

val experiments: List<Pair<String, String>> = listOf(
    """
        {
            "EXP001": {
                "${'$'}children": {
                    "participating": { "${'$'}rand": { "${'$'}gt": 0.5 } },
                    "control": { "${'$'}always":  true }
                }
            }
        }
    """.trimIndent() to """
        {}
    """.trimIndent(),
    """
        {
            "EXP001": {
                "${'$'}children": {
                    "participating": {
                        "version.number": {
                            "${'$'}gt": 3
                        }
                    },
                    "control": {}
                }
            }
        }
    """.trimIndent() to """
        {
            "version": {
                "number": 5
            }
        }
    """.trimIndent()
)

data class EvalBody(val context: String, val experiment: String)