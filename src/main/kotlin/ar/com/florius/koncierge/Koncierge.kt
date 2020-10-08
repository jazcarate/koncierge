package ar.com.florius.koncierge

import ar.com.florius.koncierge.internal.*
import arrow.core.Either
import com.google.gson.JsonParser
import java.time.ZonedDateTime


inline class KonciergeExperiment(val unExperiments: List<Experiment>)

inline class KonciergeExperimentResult(val unVariants: List<KonciergeVariant>) {
    fun toMatrix(): List<List<String>> {
        return unVariants.map { it.unVariant }
    }
}

inline class KonciergeVariant(val unVariant: List<String>)

class Koncierge {
    private val world: World = World.default

    fun changeRandomGen(f: (Unit) -> Float) {
        world.genChaos = f
    }

    fun changeDateGen(f: (Unit) -> ZonedDateTime) {
        world.getDate = f
    }

    fun parse(definition: String): KonciergeExperiment {
        return KonciergeExperiment(
            parseAll(definition).orThrow()
        )
    }

    fun evaluate(experiment: KonciergeExperiment, context: String): KonciergeExperimentResult {
        return KonciergeExperimentResult(
            experiment.unExperiments.map {
                run(world, Context(JsonParser.parseString(context)), it)
                    .map(Variant::unVariant)
            }.map(::KonciergeVariant)
        )
    }
}

private fun <B> Either<ParseError, B>.orThrow(): B {
    return when (this) {
        is Either.Right -> this.b
        is Either.Left -> throw KonciergeException(this.a.s)
    }
}
