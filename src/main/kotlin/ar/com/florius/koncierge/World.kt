package ar.com.florius.koncierge

import java.time.ZonedDateTime

interface World {
    fun getDate(): ZonedDateTime
    fun genChaos(): Number
}