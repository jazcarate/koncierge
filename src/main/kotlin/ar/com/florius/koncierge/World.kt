package ar.com.florius.koncierge

import com.google.gson.Gson
import java.time.ZonedDateTime

interface World {
    fun getDate(): ZonedDateTime
    fun genGson(): Gson
    fun genChaos(): Number
}