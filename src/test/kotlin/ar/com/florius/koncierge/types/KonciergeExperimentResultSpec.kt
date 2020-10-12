package ar.com.florius.koncierge.types

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class KonciergeExperimentResultSpec : ShouldSpec({
    context("an experiment result") {
        val res = KonciergeExperimentResult(
            listOf(
                KonciergeVariant(listOf("EXP001", "control")),
                KonciergeVariant(listOf("EXP002", "participating", "button-red"))
            )
        )

        should("map to matrix") {
            res.toMatrix() shouldBe listOf(
                listOf("EXP001", "control"),
                listOf("EXP002", "participating", "button-red"),
            )
        }

        should("map to Map") {
            res.toMap() shouldBe mapOf(
                "EXP001" to listOf("control"),
                "EXP002" to listOf("participating", "button-red"),
            )
        }
    }

})
