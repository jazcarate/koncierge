package ar.com.florius.koncierge.internal

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

fun buildExperiment(evaluator: Evaluator): Experiment {
    return Experiment(
        Variant("EXP001"),
        evaluator,
        emptyList()
    )
}


fun jsonObject(vararg pairs: Pair<String, String>): JsonElement {
    val o = JsonObject()
    for (p in pairs)
        o.addProperty(p.first, p.second)
    return o
}

fun jsonArray(vararg elems: Number): JsonArray {
    val a = JsonArray()
    for (el in elems)
        a.add(el)
    return a
}
