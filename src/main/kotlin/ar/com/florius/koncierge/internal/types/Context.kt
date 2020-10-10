package ar.com.florius.koncierge.internal.types

import com.google.gson.JsonElement

data class Context(
    val element: JsonElement
) {
    fun fmap(f: (JsonElement) -> JsonElement): Context {
        return Context(f(this.element))
    }
}

