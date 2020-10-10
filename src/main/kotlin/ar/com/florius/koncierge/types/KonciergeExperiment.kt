package ar.com.florius.koncierge.types

import ar.com.florius.koncierge.internal.types.Experiment

/**
 * This class has no useful logic; it's just a wrapper for [ar.com.florius.koncierge.internal.types.Experiment] results.
 *
 * @constructor Creates [ar.com.florius.koncierge.internal.types.Experiment] wrapper
 */
inline class KonciergeExperiment(val unExperiments: List<Experiment>)