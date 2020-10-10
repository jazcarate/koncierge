package ar.com.florius.koncierge.internal.definition

/**
 * Type level data to change a given [ar.com.florius.koncierge.internal.types.Context]
 *
 * Can be though of a function: `Context -> Context`
 */
sealed class ContextChanger

data class Dive(val key: String) : ContextChanger()
object Random : ContextChanger()
object Chaos : ContextChanger()
object Date : ContextChanger()
object Size : ContextChanger()

/**
 * Type level data to evaluate a given [ar.com.florius.koncierge.internal.types.Context] into a [Boolean],
 * if the context matches or not the definition
 *
 * Can be though of a function: `Context -> Boolean`
 */
sealed class Evaluator

data class LessThan(val x: Number) : Evaluator()
data class GreaterThan(val x: Number) : Evaluator()
data class Equal(val x: kotlin.Any) : Evaluator()
data class Always(val value: Boolean) : Evaluator()
data class And(val evals: List<Evaluator>) : Evaluator()
data class Or(val evals: List<Evaluator>) : Evaluator()
data class Any(val eval: Evaluator) : Evaluator()
data class All(val eval: Evaluator) : Evaluator()
data class Not(val inner: Evaluator) : Evaluator()

/**
 * The glue to tie [ContextChanger] with [Evaluator]
 */
data class Bind(val cc: ContextChanger, val eval: Evaluator) : Evaluator()