package ar.com.florius.koncierge.types

/**
 * This class has no useful logic; it's just a wrapper for [ar.com.florius.koncierge.types.KonciergeVariant] results.
 *
 * @constructor Creates [ar.com.florius.koncierge.types.KonciergeVariant] wrapper
 */
inline class KonciergeExperimentResult(val unVariants: List<KonciergeVariant>) {

    /**
     * Transform this into something easily consumable
     *
     * @return List of experiments's variants. The first array index is each expeirment, and the second level the experiment's varants.
     */
    fun toMatrix(): List<List<String>> {
        return unVariants.map { it.unVariant }
    }
}