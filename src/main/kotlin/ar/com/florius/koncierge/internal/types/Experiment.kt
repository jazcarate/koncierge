package ar.com.florius.koncierge.internal.types

import ar.com.florius.koncierge.internal.definition.Evaluator

data class Experiment(
    val name: Variant,
    val condition: Evaluator,
    val children: List<Experiment>
)