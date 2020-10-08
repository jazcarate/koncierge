package ar.com.florius.koncierge.internal

import com.google.gson.Gson
import com.google.gson.GsonBuilder

fun gson(): Gson {
    return GsonBuilder().create()
}