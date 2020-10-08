package ar.com.florius.koncierge.internal

import java.time.ZonedDateTime
import kotlin.random.Random.Default.nextFloat

interface World {
    var getDate: (Unit) -> ZonedDateTime
    var genChaos: (Unit) -> Float // Like random, this needs to be a value uniformly distributed between 0 (inclusive) and 1 (exclusive).

    companion object {
        val default = object : World {
            override var getDate: (Unit) -> ZonedDateTime = { ZonedDateTime.now() }
            override var genChaos: (Unit) -> Float = { nextFloat() }
        }
    }
}
