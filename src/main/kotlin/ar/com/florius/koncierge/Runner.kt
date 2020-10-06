package ar.com.florius.koncierge

import arrow.core.*
import arrow.core.computations.either
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.either.foldable.toList
import arrow.core.extensions.list.traverse.traverse
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.internal.LazilyParsedNumber
import kotlin.math.absoluteValue

inline class EvalError(val s: String)

fun run(world: World, context: Context, experiment: Experiment): Either<EvalError, List<Variant>> {
    return either.eager {
        val one = activeOne(world, context, experiment).bind()
        val children =
            experiment.children.map { activeOne(world, context, it) }.traverse(Either.applicative(), ::identity)
                .fix().bind().fix()
        val selected = children.firstOrNull { it.isRight() } ?: Unit.left()
        one.fold({ emptyList() }, { listOf(it) + selected.toList() })
    }
}

private fun activeOne(
    world: World,
    context: Context,
    experiment: Experiment
): Either<EvalError, Either<Unit, Variant>> {
    return evaluate(world, context, experiment.condition)
        .map { Either.conditionally(it, { Unit }, { experiment.name }) }
}

private fun evaluate(world: World, context: Context, condition: Evaluator): Either<EvalError, Boolean> {
    return when (condition) {
        is LessThan -> compare(context) { a -> condition.x > a }
        is GreaterThan -> compare(context) { a -> condition.x < a }
        is Equal -> (gson().toJsonTree(condition.x) == context.element).right()
        is Always -> condition.value.right()
        is And -> many(world, context, condition.evals, ::and)
        is Or -> many(world, context, condition.evals, ::or)
        is Any -> list(world, context, condition.eval, ::or)
        is All -> list(world, context, condition.eval, ::and)
        is Bind -> change(world, condition.cc, context).flatMap { evaluate(world, it, condition.eval) }
    }
}

fun list(
    world: World,
    context: Context,
    eval: Evaluator,
    f: (Iterable<Boolean>) -> (Boolean)
): Either<EvalError, Boolean> {
    val element = context.element
    return if (element.isJsonArray) {
        element.asJsonArray.map(::Context)
            .traverse(Either.applicative()) { evaluate(world, it, eval) }
            .fix().map { it.fix() }
            .map { f(it) }
    } else {
        return EvalError("Can't \$any a non array. Got [$element]").left()
    }
}

private fun many(
    world: World,
    context: Context,
    evals: List<Evaluator>,
    f: (Iterable<Boolean>) -> Boolean
): Either<EvalError, Boolean> {
    return evals.traverse(Either.applicative()) { evaluate(world, context, it) }
        .fix().map { it.fix() }
        .map { f(it) }
}

private fun change(world: World, cc: ContextChanger, context: Context): Either<EvalError, Context> {
    return when (cc) {
        is Dive -> dive(cc.key, context.element).map(::Context).mapLeft(::EvalError)
        is Date -> Context(
            JsonPrimitive(
                world.getDate().toEpochSecond()
            )
        ).right()
        is Random -> context.fmap { JsonPrimitive(hash(it)) }.right()
        is Chaos -> Context(JsonPrimitive(world.genChaos())).right()
        is Size -> size(context.element).map { Context(JsonPrimitive(it)) }.mapLeft(::EvalError)
    }
}

private fun gson(): Gson {
    return GsonBuilder().create()
}

private fun compare(context: Context, cmp: (Number) -> Boolean): Either<EvalError, Boolean> {
    return if (context.element.isJsonPrimitive) {
        val primitive = context.element.asJsonPrimitive
        if (primitive.isNumber) {
            cmp(primitive.asNumber).right()
        } else {
            EvalError("Can only compare numbers. [$primitive] is not a number").left()
        }
    } else {
        EvalError("Can only compare primitives. [$context] is not a primitive").left()
    }
}

fun size(element: JsonElement): Either<String, Number> {
    if (element.isJsonObject)
        return element.asJsonObject.size().right()
    if (element.isJsonArray)
        return element.asJsonArray.size().right()
    if (element.isJsonNull)
        return 0.right()
    if (element.isJsonPrimitive) {
        val p = element.asJsonPrimitive
        if (p.isString)
            return p.asString.length.right()
    }
    return "Can't get size for [$element]".left()
}

private fun hash(element: JsonElement): Float {
    return gson().toJson(element).hashCode().absoluteValue.toFloat() / Int.MAX_VALUE
}

private fun dive(key: String, elem: JsonElement): Either<String, JsonElement> {
    return if (elem.isJsonObject) {
        val jsonObject = elem.asJsonObject
        if (jsonObject.has(key)) {
            jsonObject[key].right()
        } else {
            "[$key] is not present in [$elem]. Can't dive".left()
        }
    } else if (elem.isJsonArray) {
        try {
            val index = LazilyParsedNumber(key).toInt()
            elem.asJsonArray[index].right()
        } catch (e: NumberFormatException) {
            "Can only dive into arrays by a number. Got [$key]".left()
        }
    } else {
        "Can only dive in objects or arrays. [$elem] is neither".left()
    }
}

private fun and(xs: Iterable<Boolean>): Boolean {
    return xs.all { it }
}

private fun or(xs: Iterable<Boolean>): Boolean {
    return xs.any { it }
}

private operator fun Number.compareTo(x: Number): Int {
    return this.toDouble().compareTo(x.toDouble())
}


