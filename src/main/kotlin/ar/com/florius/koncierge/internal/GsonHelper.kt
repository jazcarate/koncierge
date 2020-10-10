package ar.com.florius.koncierge.internal

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Helper function to use the same settigns across all the [com.google.gson.Gson] parsers
 */
fun gson(): Gson {
    return GsonBuilder().create()
}