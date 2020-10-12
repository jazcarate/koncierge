package ar.com.florius.koncierge.types

/**
 * This class has no useful logic; it's just a wrapper for [ar.com.florius.koncierge.types.KonciergeVariant] results.
 *
 * @constructor Creates [ar.com.florius.koncierge.types.KonciergeVariant] wrapper
 */
inline class KonciergeExperimentResult(val unVariants: List<KonciergeVariant>) {

    /**
     * Transform [KonciergeExperimentResult] into a matrix of results
     *
     * E.g. [
     *        ["EXP001", "control"],
     *        ["EXP002", "participating", "reb-button"]]
     *      ]
     *
     * @return List of experiments' variants. The first array index is each experiment, and the second level the experiment's variants  (in evaluating order)
     */
    fun toMatrix(): List<List<String>> {
        return unVariants.map { it.unVariant }
    }

    /**
     * Transform [KonciergeExperimentResult] into a matrix of results
     *
     * E.g. {
     *        "EXP001": ["control"],
     *        "EXP002": ["participating", "reb-button"]
     *      }
     *
     * @return Map of experiments' variants. Indexed by experiment name, with value of all matching children (in evaluating order)
     */
    fun toMap(): Map<String, List<String>> {
        return toMatrix()
            .map { (ex, variants) -> Pair(ex, variants) }
            .toMap()
    }
}

private operator fun <T> List<T>.component2(): List<T> = this.drop(1)