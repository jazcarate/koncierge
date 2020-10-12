package ar.com.florius.koncierge.internal

import ar.com.florius.koncierge.internal.definition.*
import ar.com.florius.koncierge.internal.definition.Any
import ar.com.florius.koncierge.internal.types.Context
import ar.com.florius.koncierge.internal.types.Experiment
import ar.com.florius.koncierge.internal.types.Variant
import ar.com.florius.koncierge.internal.types.World
import arrow.core.*
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.list.traverse.traverse
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.internal.LazilyParsedNumber
import kotlin.math.absoluteValue

/**
 * This class has no useful logic; it's just a wrapper an evaluation error.
 *
 * An evaluation error is considered as a negative match on evaluation.
 * e.g.: diving into an empty object, is an [EvalError], but will simply not match
 * the experiment definition when [evaluate] is called.
 *
 * @constructor Warps an evaluation error
 */
inline class EvalError(val s: String)

/**
 * Given a [world], a [context] (with an experiment), evaluates the list of variants for it.
 * *Note*: the experiment is a sinlge one, and not the experiment definition
 *
 * @param[world] The effect manager
 * @param[context] The context to evaluate
 *
 * @return list of variants that match the experiment
 */
fun evaluate(world: World, context: Context): List<Variant> {
    val one = activeOne(world, context)
    val selected =
        context.experiment.children
            .map { activeOne(world, context.copy(experiment = it)) }
            .mapNotNull { it.orNull() }
            .firstOrNull()
    return one.fold({ emptyList() }, { listOf(it) }) + (selected?.let { listOf(it) } ?: emptyList())
}

private fun activeOne(
    world: World,
    context: Context,
): Either<EvalError, Variant> {
    return evaluateOne(world, context, context.experiment.condition)
        .flatMap { Either.conditionally(it, { EvalError("Did not match") }, { context.experiment.name }) }
}

/**
 * [Evaluator] transformations
 */
private fun evaluateOne(world: World, context: Context, condition: Evaluator): Either<EvalError, Boolean> {
    return when (condition) {
        is LessThan -> compare(context) { a -> condition.x > a }
        is GreaterThan -> compare(context) { a -> condition.x < a }
        is Equal -> (gson().toJsonTree(condition.x) == context.element).right()
        is Not -> evaluateOne(world, context, condition.inner).map { it.not() }
        is Always -> condition.value.right()
        is And -> many(world, context, condition.evals, ::and)
        is Or -> many(world, context, condition.evals, ::or)
        is Any -> list(world, context, condition.eval, ::or)
        is All -> list(world, context, condition.eval, ::and)
        is Bind -> change(world, condition.cc, context).flatMap { evaluateOne(world, it, condition.eval) }
    }
}

private fun list(
    world: World,
    context: Context,
    eval: Evaluator,
    f: (Iterable<Boolean>) -> (Boolean)
): Either<EvalError, Boolean> {
    val element = context.element
    return if (element.isJsonArray) {
        element.asJsonArray.map { context.fmapConst(it) }
            .traverse(Either.applicative()) { evaluateOne(world, it, eval) }
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
    return evals.traverse(Either.applicative()) { evaluateOne(world, context, it) }
        .fix().map { it.fix() }
        .map { f(it) }
}

/**
 * [ContextChanger] transformations
 */
private fun change(world: World, cc: ContextChanger, context: Context): Either<EvalError, Context> {
    return when (cc) {
        is Dive -> dive(cc.key, context.element).map(context::fmapConst).mapLeft(::EvalError)
        is Date -> context.fmapConst(
            JsonPrimitive(
                world.getDate.invoke(Unit).toEpochSecond()
            )
        ).right()
        is Random -> context.fmap { JsonPrimitive(hash(it, context.experiment)) }.right()
        is Chaos -> context.fmapConst(JsonPrimitive(world.safeGenChaos())).right()
        is Size -> size(context.element).map { context.fmapConst(JsonPrimitive(it)) }.mapLeft(::EvalError)
    }
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

private fun size(element: JsonElement): Either<String, Number> {
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

private fun hash(element: JsonElement, experiment: Experiment): Float {
    return (gson().toJson(element) + experiment.name.unVariant)
        .hashCode().absoluteValue.toFloat() / Int.MAX_VALUE
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


