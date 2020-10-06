package ar.com.florius.koncierge

import com.google.gson.JsonElement

data class Context(
    val element: JsonElement
) {
    fun fmap(f: (JsonElement) -> JsonElement): Context {
        return Context(f(this.element))
    }
}

inline class Variant(val unVariant: String)

data class Experiment(
    val name: Variant,
    val condition: Evaluator,
    val children: List<Experiment>
)

sealed class ContextChanger // Context -> Context

data class Dive(val key: String) : ContextChanger()
object Random : ContextChanger()
object Chaos : ContextChanger()
object Date : ContextChanger()
object Size : ContextChanger()

sealed class Evaluator // Context -> Bool

data class LessThan(val x: Number) : Evaluator()
data class GreaterThan(val x: Number) : Evaluator()
data class Equal(val x: kotlin.Any) : Evaluator()
data class Always(val value: Boolean) : Evaluator()
data class And(val evals: List<Evaluator>) : Evaluator()
data class Or(val evals: List<Evaluator>) : Evaluator()
data class Any(val eval: Evaluator) : Evaluator()
data class All(val eval: Evaluator) : Evaluator()

data class Bind(val cc: ContextChanger, val eval: Evaluator) : Evaluator()