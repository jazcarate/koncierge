package ar.com.florius.koncierge

import arrow.core.*
import arrow.core.computations.either
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.either.foldable.toList
import arrow.core.extensions.list.traverse.traverse
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
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
        is Equal -> (world.genGson().toJsonTree(condition.x) == context.element).right()
        is Always -> condition.value.right()
        is And -> many(world, context, condition.evals, ::and)
        is Or -> many(world, context, condition.evals, ::or)
        is Bind -> change(world, condition.cc, context).flatMap { evaluate(world, it, condition.eval) }
    }
}

private fun many(
    world: World,
    context: Context,
    evals: List<Evaluator>,
    f: (Iterable<Boolean>) -> Boolean
): Either<EvalError, Boolean> {
    return evals.traverse(Either.applicative()) { evaluate(world, context, it) }.fix()
        .map { f(it.fix()) }
}

private fun compare(context: Context, cmp: (Number) -> Boolean): Either<EvalError, Boolean> {
    return if (context.element.isJsonPrimitive) {
        val primitive = context.element.asJsonPrimitive
        if (primitive.isNumber) {
            cmp(primitive.asNumber).right()
        } else {
            EvalError("Can only compare numbers. $primitive is not a number").left()
        }
    } else {
        EvalError("Can only compare primitives. $context is not a primitive").left()
    }
}

private fun change(world: World, cc: ContextChanger, context: Context): Either<EvalError, Context> {
    return when (cc) {
        is Dive -> dive(cc.key, context.element).map(::Context).mapLeft(::EvalError)
        is Date -> Context(JsonPrimitive(world.getDate().toEpochSecond())).right()
        is Random -> Context(JsonPrimitive(hash(world, context))).right()
        is Chaos -> Context(JsonPrimitive(world.genChaos())).right()
        is Compose -> change(world, cc.cc1, context).flatMap { change(world, cc.cc2, it) }
        is Id -> context.right()
    }
}

private fun hash(world: World, context: Context): Float {
    return world.genGson().toJson(context.element).hashCode().absoluteValue.toFloat() / Int.MAX_VALUE
}

private fun dive(key: String, elem: JsonElement): Either<String, JsonElement> {
    return if (elem.isJsonObject) {
        val jsonObject = elem.asJsonObject
        if (jsonObject.has(key)) {
            jsonObject[key].right()
        } else {
            "$key is not present in $elem. Can't dive".left()
        }
    } else {
        "Can only dive in objects. $elem is neither".left()
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


