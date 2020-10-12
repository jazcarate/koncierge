package ar.com.florius.koncierge.internal.types

import com.google.gson.JsonElement

data class Context(
    val element: JsonElement,
    val experiment: Experiment // TODO: Is this posible?
) {
    fun fmap(f: (JsonElement) -> JsonElement): Context {
        return Context(f(this.element), experiment)
    }

    fun fmapConst(elem: JsonElement): Context {
        return fmap { elem }
    }
}

