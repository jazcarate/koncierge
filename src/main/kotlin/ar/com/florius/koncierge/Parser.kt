package ar.com.florius.koncierge

import arrow.core.*
import arrow.core.computations.either
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.list.traverse.traverse
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.temporal.ChronoUnit


inline class ParseError(val s: String)

fun parseAll(s: String): Either<ParseError, List<Experiment>> {
    return parseAll(JsonParser.parseString(s))
}

fun parseAll(elem: JsonElement): Either<ParseError, List<Experiment>> {
    if (!elem.isJsonObject) {
        return ParseError("The first level of experiment definition needs to be an object. Got [$elem]").left()
    }
    val treeO = elem.asJsonObject
    if (treeO.size() == 0) {
        return ParseError("There needs to be at least one experiment definition. Got [$elem]").left()
    }

    return treeO.entrySet().map { (name, value) -> experiment(name, value) }
        .traverse(Either.applicative(), ::identity)
        .fix().map { it.fix() }
}

fun evaluator(def: JsonObject): Either<ParseError, Evaluator> {
    if (def.size() == 0)
        return Always(true).right()

    return def.entrySet().map { (key, element) -> evaluatorOne(key, element) }
        .traverse(Either.applicative(), ::identity).fix()
        .map { it.fix() }
        .map(::mergeEvaluators)
}

private fun experiment(name: String, element: JsonElement): Either<ParseError, Experiment> {
    return either.eager {
        val def = toObject(element).bind()

        val (childrenE, rest) = span(def, "\$children")

        val children = children(childrenE).bind()
        val condition = evaluator(rest).bind()

        Experiment(Variant(name), condition, children)
    }
}

private fun span(og: JsonObject, separator: String): Pair<JsonElement?, JsonObject> { // Ewwww. Mutation
    val copy = og.deepCopy()
    val removed = copy.remove(separator)
    return Pair(removed, copy)
}

private fun span(og: String, separator: String): Pair<String, String?> {
    val ss = og.split(delimiters = arrayOf(separator), limit = 2)
    return if (ss.size == 2) Pair(ss[0], ss[1]) else Pair(og, null)
}

private fun children(elem: JsonElement?): Either<ParseError, List<Experiment>> {
    return elem?.let(::parseAll) ?: emptyList<Experiment>().right()
}


private fun mergeEvaluators(it: ListK<Evaluator>) =
    if (it.size == 1) it[0] else And(it)

private fun evaluatorOne(key: String, element: JsonElement): Either<ParseError, Evaluator> {
    return when (key) {
        "\$lt" -> compare(element, ::LessThan)
        "\$gt" -> compare(element, ::GreaterThan)
        "\$eq" -> Equal(element).right()
        "\$always" -> always(element)
        "\$and" -> many(element, ::And)
        "\$or" -> many(element, ::Or)
        "\$any" -> list(element, ::Any)
        "\$all" -> list(element, ::All)
        else -> {
            val (firstKey, rest) = span(key, ".")
            val cc = contextChanger(firstKey)
            if (element.isJsonObject) {
                (if (rest == null) evaluator(element.asJsonObject) else evaluatorOne(rest, element))
                    .map { Bind(cc, it) }
            } else {
                Bind(cc, Equal(element)).right()
            }
        }
    }
}

private fun contextChanger(key: String): ContextChanger {
    return when (key) {
        "\$rand" -> Random
        "\$chaos" -> Chaos
        "\$date" -> Date
        "\$size" -> Size
        else -> Dive(key)
    }
}

private fun list(element: JsonElement, ctor: (Evaluator) -> Evaluator): Either<ParseError, Evaluator> {
    return either.eager {
        val o = toObject(element).bind()
        o.entrySet().map { (key, subElement) -> evaluatorOne(key, subElement) }
            .traverse(Either.applicative(), ::identity).fix()
            .map { it.fix() }
            .map(::mergeEvaluators)
            .map { ctor(it) }
            .bind()
    }
}

private fun many(element: JsonElement, ctor: (List<Evaluator>) -> Evaluator): Either<ParseError, Evaluator> {
    return either.eager {
        val o = toObject(element).bind()

        o.entrySet().map { (key, subElement) -> evaluatorOne(key, subElement) }
            .traverse(Either.applicative(), ::identity)
            .fix().map { ctor(it.fix()) }
            .bind()
    }
}

private fun compare(element: JsonElement, ctor: (Number) -> Evaluator): Either<ParseError, Evaluator> {
    return either.eager {
        val p = toPrimitive(element).bind()

        val date = date(p.asString)
        if (date != null)
            return@eager ctor(date)

        val n = toNumber(p).bind()
        ctor(n)
    }
}

private fun date(s: String?): Number? {
    return listOf("yyyy-MM-dd'T'HH:mm:ss.SSSZ", "EEE MMM dd yyyy HH:mm:ss 'GMT'Z (z)", "yyyy-MM-dd")
        .map(::SimpleDateFormat).mapNotNull {
            try {
                it.parse(s)
            } catch (e: ParseException) {
                null
            }
        }.firstOrNull()?.toInstant()?.toEpochMilli()
        ?.let { Duration.of(it, ChronoUnit.MILLIS).seconds }
}

private fun always(element: JsonElement): Either<ParseError, Evaluator> {
    return either.eager {
        val p = toPrimitive(element).bind()
        val b = toBoolean(p).bind()
        Always(b)
    }
}

private fun toObject(element: JsonElement): Either<ParseError, JsonObject> {
    return if (!element.isJsonObject)
        ParseError("Expecting an object. Got [$element]").left()
    else
        element.asJsonObject.right()
}

private fun toNumber(element: JsonPrimitive): Either<ParseError, Number> {
    if (!element.isNumber)
        return ParseError("Expecting a number. Got [$element]").left()
    return element.asNumber.right()
}

private fun toBoolean(element: JsonPrimitive): Either<ParseError, Boolean> {
    if (!element.isBoolean)
        return ParseError("Expecting a boolean. Got [$element]").left()
    return element.asBoolean.right()
}

private fun toPrimitive(element: JsonElement): Either<ParseError, JsonPrimitive> {
    if (!element.isJsonPrimitive)
        return ParseError("Expecting a primitive. Got [$element]").left()
    return element.asJsonPrimitive.right()
}