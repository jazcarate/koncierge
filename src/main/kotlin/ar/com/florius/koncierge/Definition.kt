package ar.com.florius.koncierge

import com.google.gson.JsonElement

data class Context(
    val element: JsonElement
)

inline class Variant(val unVariant: String)

data class Experiment(
    val name: Variant,
    val condition: Evaluator,
    val children: List<Experiment>
)

sealed class ContextChanger // Context -> Context

class Dive(val key: String) : ContextChanger()
object Random : ContextChanger()
object Chaos : ContextChanger()
object Date : ContextChanger()
class Compose(val cc1: ContextChanger, val cc2: ContextChanger) : ContextChanger()
object Id : ContextChanger()

sealed class Evaluator // Context -> Bool

class LessThan(val x: Number) : Evaluator()
class GreaterThan(val x: Number) : Evaluator()
class Equal(val x: Any) : Evaluator()
class Always(val value: Boolean) : Evaluator()
class And(val evals: List<Evaluator>) : Evaluator()
class Or(val evals: List<Evaluator>) : Evaluator()

class Bind(val cc: ContextChanger, val eval: Evaluator) : Evaluator()