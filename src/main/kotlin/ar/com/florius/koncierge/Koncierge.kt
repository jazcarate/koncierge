package ar.com.florius.koncierge

import ar.com.florius.koncierge.internal.ParseError
import ar.com.florius.koncierge.internal.evaluate
import ar.com.florius.koncierge.internal.parseAll
import ar.com.florius.koncierge.internal.types.Context
import ar.com.florius.koncierge.internal.types.Variant
import ar.com.florius.koncierge.internal.types.World
import ar.com.florius.koncierge.types.KonciergeException
import ar.com.florius.koncierge.types.KonciergeExperiment
import ar.com.florius.koncierge.types.KonciergeExperimentResult
import ar.com.florius.koncierge.types.KonciergeVariant
import arrow.core.Either
import com.google.gson.JsonParser
import java.time.ZonedDateTime

/**
 * Entrypoint for **koncierge**'s functionality
 */
class Koncierge {
    private val world: World = World.default

    /**
     * Change the random generator function.
     * Defaults to [kotlin.random.Random.nextFloat].
     *
     * **Caveat**: Should behave like [kotlin.random.Random.nextFloat]
     * returning a value uniformly distributed between `0` (inclusive) and `1` (exclusive)
     *
     * @param[f] Random generator function
     * @see kotlin.random.Random.nextFloat
     */
    fun changeRandomGen(f: (Unit) -> Float) {
        world.genChaos = f
    }

    /**
     * Change the date generator function.
     * Defaults to [java.time.ZonedDateTime.now]
     *
     * @param[f] date generator function
     * @see java.time.ZonedDateTime.now
     */
    fun changeDateGen(f: (Unit) -> ZonedDateTime) {
        world.getDate = f
    }

    /**
     * Parses a JSON experiment definition.
     *
     * @param[definition] JSON format of an experiment definition
     * @throws com.google.gson.JsonParseException if the [definition] text is not valid JSON
     * @throws KonciergeException if the [definition] does not match **koncierge**'s format
     */
    fun parse(definition: String): KonciergeExperiment {
        return KonciergeExperiment(
            parseAll(definition).orThrow()
        )
    }

    /**
     * Evaluates an experiment given a JSON context.
     *
     * If the context does not match an experiment, it will default that the context is not part of that variant.
     *
     * @param[experiment] An experiment definition from [ar.com.florius.koncierge.Koncierge.parse]
     * @param[context] A JSON formatted context.
     * @return A list of experiments and their variants that matched with the given context.
     * @throws com.google.gson.JsonParseException if the [context] text is not valid JSON
     */
    fun evaluate(experiment: KonciergeExperiment, context: String): KonciergeExperimentResult {
        return KonciergeExperimentResult(
            experiment.unExperiments.map {
                evaluate(world, Context(JsonParser.parseString(context), it))
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
