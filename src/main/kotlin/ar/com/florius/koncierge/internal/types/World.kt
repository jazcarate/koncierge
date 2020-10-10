package ar.com.florius.koncierge.internal.types

import java.time.ZonedDateTime
import kotlin.random.Random.Default.nextFloat

/**
 * Effects manager for **koncierge**'s evaluator
 */
interface World {
    var getDate: (Unit) -> ZonedDateTime
    var genChaos: (Unit) -> Float

    /**
     * Run the chaos generator function and assert the distribution
     *
     * @throws AssertionError if the generated value is outside the [0,1) boundaries
     */
    fun safeGenChaos(): Float {
        val chaos = this.genChaos.invoke(Unit)
        assert(chaos >= 0 && chaos < 1) { "Chaos generator should return a value uniformly distributed between `0` (inclusive) and `1` (exclusive)" }
        return chaos
    }

    companion object {
        /**
         * Default values for the [World].
         */
        val default = object : World {
            override var getDate: (Unit) -> ZonedDateTime = { ZonedDateTime.now() }
            override var genChaos: (Unit) -> Float = { nextFloat() }
        }
    }
}
