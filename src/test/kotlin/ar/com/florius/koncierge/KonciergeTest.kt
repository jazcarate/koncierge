package ar.com.florius.koncierge

import ar.com.florius.koncierge.types.KonciergeException
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class KonciergeTest : ShouldSpec({
    context("with a Koncierge instance") {
        val koncierge = Koncierge()

        val simpleExperiment = """
            {
                "EXP001": {
                    "${'$'}children": {
                        "participating": { "${'$'}rand": { "${'$'}gt": 0.5 } },
                        "control": { "${'$'}always":  true }
                    }
                }
            }
        """.trimIndent()

        context("parse") {
            should("raise an error if the experiment is formatted badly") {
                val exception = shouldThrow<KonciergeException> {
                    koncierge.parse("foo")
                }
                exception.message shouldBe "The first level of experiment definition needs to be an object. Got [\"foo\"]"
            }

            should("be able to parse a simple example") {
                shouldNotThrow<KonciergeException> {
                    koncierge.parse(simpleExperiment)
                }
            }
        }

        context("evaluate") {
            context("with an experiment") {
                val experiment = koncierge.parse(simpleExperiment)

                should("be able to compute the experiments") {
                    val result = koncierge.evaluate(experiment, "{}").toMatrix()

                    result shouldBe listOf(listOf("EXP001", "participating"))
                }
            }
        }
    }
})
