package ar.com.florius.koncierge.internal

import ar.com.florius.koncierge.internal.definition.*
import ar.com.florius.koncierge.internal.definition.Any
import ar.com.florius.koncierge.internal.types.Context
import ar.com.florius.koncierge.internal.types.Experiment
import ar.com.florius.koncierge.internal.types.Variant
import ar.com.florius.koncierge.internal.types.World
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.ZoneOffset
import java.time.ZonedDateTime


class RunnerSpec : ShouldSpec({

    context("in a world") {
        val now = ZonedDateTime.of(2020, 10, 4, 14, 0, 0, 0, ZoneOffset.UTC)
        var chaos = 0.4F // chosen by fair dice roll.

        val world = object : World {
            override var getDate: (Unit) -> ZonedDateTime = { now }
            override var genChaos: (Unit) -> Float = { chaos }
        }

        context("with no children") {
            context("experiment with a bind") {
                val experiment = buildExperiment(
                    Bind(
                        Dive("beta"),
                        Equal(JsonPrimitive("yes"))
                    )
                )

                context("empty context") {
                    should("fails to dive") {
                        val context = Context(JsonNull.INSTANCE, experiment)

                        evaluate(
                            world,
                            context
                        ).shouldNotMatch()
                    }
                }

                context("context with the key to dive") {
                    should("dive and call the evaluator") {
                        val context = Context(jsonObject("beta" to "yes"), experiment)

                        evaluate(
                            world,
                            context
                        ).shouldMatch(Variant("EXP001"))
                    }

                    should("valuate to a disabled") {
                        val context = Context(jsonObject("beta" to "no"), experiment)

                        evaluate(
                            world,
                            context
                        ).shouldNotMatch()
                    }
                }
            }

            context("bind of an array") {
                val experiment = buildExperiment(
                    Bind(
                        Dive("2"),
                        Equal(JsonPrimitive(30))
                    )
                )

                should("dive by position") {
                    val context = Context(jsonArray(10, 20, 30), experiment)

                    evaluate(
                        world,
                        context
                    ).shouldMatch(Variant("EXP001"))
                }

                should("fail if not an array") {
                    val context = Context(jsonObject("beta" to "no"), experiment)

                    evaluate(
                        world,
                        context
                    ).shouldNotMatch()
                }
            }

            context("dive of an object") {
                val experiment = buildExperiment(
                    Bind(
                        Dive("foo"),
                        Equal(JsonPrimitive(30))
                    )
                )

                should("fail to dive into an array") {
                    val context = Context(jsonArray(10, 20, 30), experiment)

                    evaluate(
                        world,
                        context
                    ).shouldNotMatch()
                }
            }

            context("experiment with a date context") {
                context("checking is after tomorrow") {
                    val experiment = buildExperiment(
                        Bind(
                            Date,
                            GreaterThan(now.plusDays(1).toEpochSecond())
                        )
                    )

                    should("not match") {
                        evaluate(
                            world,
                            Context(JsonNull.INSTANCE, experiment)
                        ).shouldNotMatch()
                    }
                }

                context("checking is after yesterday") {
                    val experiment = buildExperiment(
                        Bind(
                            Date,
                            GreaterThan(now.minusDays(1).toEpochSecond())
                        )
                    )

                    should("matches the experiment") {
                        evaluate(
                            world,
                            Context(JsonNull.INSTANCE, experiment)
                        ).shouldMatch(Variant("EXP001"))
                    }
                }

                context("checking is before tomorrow") {
                    val experiment = buildExperiment(
                        Bind(
                            Date,
                            LessThan(now.plusDays(1).toEpochSecond())
                        )
                    )

                    should("matches the experiment") {
                        evaluate(
                            world,
                            Context(JsonNull.INSTANCE, experiment)
                        ).shouldMatch(Variant("EXP001"))
                    }
                }

                context("checking is before yesterday") {
                    val experiment = buildExperiment(
                        Bind(
                            Date,
                            LessThan(now.minusDays(1).toEpochSecond())
                        )
                    )

                    should("not match") {
                        evaluate(
                            world,
                            Context(JsonNull.INSTANCE, experiment)
                        ).shouldNotMatch()
                    }
                }
            }

            context("experiment with a always context") {
                context("checking true == true") {
                    val experiment = buildExperiment(
                        Always(true)
                    )

                    should("matches the experiment") {
                        evaluate(
                            world,
                            Context(JsonNull.INSTANCE, experiment)
                        ).shouldMatch(experiment.name)
                    }
                }

                context("checking false == true") {
                    val experiment = buildExperiment(
                        Always(false)
                    )

                    should("not match") {
                        evaluate(
                            world,
                            Context(JsonNull.INSTANCE, experiment)
                        ).shouldNotMatch()
                    }
                }
            }

            context("experiment with an OR") {
                context("checking true or false") {
                    val experiment = buildExperiment(
                        Or(
                            listOf(
                                Always(true),
                                Always(false)
                            )
                        )
                    )

                    should("not match") {
                        evaluate(
                            world,
                            Context(JsonNull.INSTANCE, experiment)
                        ).shouldMatch(experiment.name)
                    }
                }

                context("checking false or false") {
                    val experiment = buildExperiment(
                        Or(
                            listOf(
                                Always(false),
                                Always(false)
                            )
                        )
                    )

                    should("not match") {
                        evaluate(
                            world,
                            Context(JsonNull.INSTANCE, experiment)
                        ).shouldNotMatch()
                    }
                }
            }

            context("experiment with an AND") {
                context("checking true and false") {
                    val experiment = buildExperiment(
                        And(
                            listOf(
                                Always(true),
                                Always(false)
                            )
                        )
                    )

                    should("not match") {
                        evaluate(
                            world,
                            Context(JsonNull.INSTANCE, experiment)
                        ).shouldNotMatch()
                    }
                }

                context("checking true and true") {
                    val experiment = buildExperiment(
                        And(
                            listOf(
                                Always(true),
                                Always(true)
                            )
                        )
                    )

                    should("matches the experiment") {
                        evaluate(
                            world,
                            Context(JsonNull.INSTANCE, experiment)
                        ).shouldMatch(experiment.name)
                    }
                }
            }

            context("experiment with size") {
                context("checking the size") {
                    val experiment = buildExperiment(
                        Bind(
                            Size,
                            GreaterThan(2)
                        )
                    )

                    context("of an array") {
                        context("short") {
                            val context = Context(jsonArray(1), experiment)

                            should("not match") {
                                evaluate(
                                    world,
                                    context
                                ).shouldNotMatch()
                            }
                        }

                        context("long") {
                            val context = Context(jsonArray(1, 2, 3), experiment)

                            should("not match") {
                                evaluate(
                                    world,
                                    context
                                ).shouldMatch(experiment.name)
                            }
                        }
                    }

                    context("of an string") {
                        context("short") {
                            val context = Context(JsonPrimitive("h"), experiment)

                            should("not match") {
                                evaluate(
                                    world,
                                    context
                                ).shouldNotMatch()
                            }
                        }

                        context("long") {
                            val context = Context(JsonPrimitive("hello!"), experiment)

                            should("not match") {
                                evaluate(
                                    world,
                                    context
                                ).shouldMatch(experiment.name)
                            }
                        }
                    }

                    context("of an object") {
                        context("short") {
                            val context = Context(jsonObject("a" to "1"), experiment)

                            should("not match") {
                                evaluate(
                                    world,
                                    context
                                ).shouldNotMatch()
                            }
                        }

                        context("long") {
                            val context = Context(jsonObject("a" to "1", "b" to "2", "c" to "3"), experiment)

                            should("not match") {
                                evaluate(
                                    world,
                                    context
                                ).shouldMatch(experiment.name)
                            }
                        }
                    }

                    context("of null") {
                        val context = Context(JsonNull.INSTANCE, experiment)

                        should("not match") {
                            evaluate(
                                world,
                                context
                            ).shouldNotMatch()
                        }
                    }

                    context("something else") {
                        val context = Context(JsonPrimitive(true), experiment)

                        should("fails to parse") {
                            evaluate(
                                world,
                                context
                            ).shouldNotMatch()
                        }
                    }
                }
            }

            context("experiment with an random") {
                val experiment = buildExperiment(
                    Bind(
                        Random,
                        Equal(
                            JsonPrimitive(
                                0.09776043
                            )
                        ) // Generated once for `null` context and EXP001
                    )
                )

                context("with a empty context") {
                    val context = Context(JsonNull.INSTANCE, experiment)

                    should("matches the experiment") {
                        evaluate(
                            world,
                            context
                        ).shouldMatch(experiment.name)
                    }
                }

                context("with a different context") {
                    val context = Context(JsonPrimitive("foo"), experiment)

                    should("not match") {
                        evaluate(
                            world,
                            context
                        ).shouldNotMatch()
                    }
                }

                context("the same context") {
                    val context = Context(JsonNull.INSTANCE, experiment)

                    context("with the same experiment") {
                        should("yield the same result") {
                            val res1 = evaluate(world, context)
                            val res2 = evaluate(world, context)

                            res1.shouldMatch(experiment.name)
                            res2.shouldMatch(experiment.name)
                        }
                    }

                    context("with different experiments") {
                        val otherExperiment =
                            context.copy(experiment = experiment.copy(name = Variant("Something different")))

                        should("yield the different result") {
                            val res1 = evaluate(world, context)
                            val res2 = evaluate(world, otherExperiment)

                            res1.shouldContainExactly(experiment.name)
                            res2.shouldNotMatch()
                        }
                    }
                }
            }

            context("experiment with an chaos") {
                val experiment = buildExperiment(
                    Bind(
                        Chaos,
                        GreaterThan(0.5)
                    )
                )

                should("distribute variants") {
                    chaos = 0.99F
                    val res1 = evaluate(world, Context(JsonNull.INSTANCE, experiment))
                    chaos = 0.0F
                    val res2 = evaluate(world, Context(JsonNull.INSTANCE, experiment))


                    res1.shouldMatch(experiment.name)
                    res2.shouldNotMatch()
                }
            }

            context("experiment with an not") {
                val experiment = buildExperiment(
                    Not(Equal(JsonPrimitive("yes")))
                )

                should("negates the predicate, matching") {
                    evaluate(
                        world, Context(JsonNull.INSTANCE, experiment)
                    ).shouldMatch(experiment.name)
                }

                should("negates the predicate, not matching") {
                    evaluate(
                        world, Context(JsonPrimitive("yes"), experiment)
                    ).shouldNotMatch()
                }
            }

            context("experiment with an compose") {
                context("a compose of dives") {
                    val experiment = buildExperiment(
                        Bind(Dive("foo"), Bind(Dive("bar"), Equal(JsonPrimitive("yes"))))
                    )

                    context("an empty context") {
                        val context = Context(JsonNull.INSTANCE, experiment)

                        should("fail to dive") {
                            evaluate(
                                world,
                                context
                            ).shouldNotMatch()
                        }
                    }

                    context("a context with only the first layer of dive") {
                        val context = Context(jsonObject("foo" to "yes"), experiment)

                        should("fail to dive deep") {
                            evaluate(
                                world,
                                context
                            ).shouldNotMatch()
                        }
                    }

                    context("a context with all the layers of dive") {
                        val contextJson = JsonObject()
                        contextJson.add("foo", jsonObject("bar" to "yes"))
                        val context = Context(contextJson, experiment)

                        should("match the deep level") {
                            evaluate(
                                world,
                                context
                            ).shouldMatch(experiment.name)
                        }
                    }
                }
            }

            context("contexts with an any") {
                val experiment = buildExperiment(
                    Any(
                        Equal(JsonPrimitive(3))
                    )
                )

                context("an array context") {
                    context("at least one matches") {
                        val context = Context(
                            jsonArray(3, 2),
                            experiment
                        )

                        should("match") {
                            evaluate(
                                world,
                                context
                            ).shouldMatch(experiment.name)
                        }
                    }

                    context("none matches") {
                        val context = Context(
                            jsonArray(2),
                            experiment
                        )

                        should("not match") {
                            evaluate(
                                world,
                                context
                            ).shouldNotMatch()
                        }
                    }

                    context("empty list") {
                        val context = Context(JsonArray(), experiment)

                        should("not match") {
                            evaluate(
                                world,
                                context
                            ).shouldNotMatch()
                        }
                    }
                }

                context("not an array") {
                    context("fails to parse") {

                        should("match") {
                            evaluate(
                                world,
                                Context(JsonPrimitive(3), experiment),
                            ).shouldNotMatch()
                        }
                    }
                }

                xcontext("an object context") {
                    context("at least one matches") {
                        val context = Context(
                            jsonObject("foo" to "3"),
                            experiment
                        )

                        should("match") {
                            evaluate(
                                world,
                                context
                            ).shouldMatch(experiment.name)
                        }
                    }

                    context("none matches") {
                        val context = Context(
                            jsonArray(2),
                            experiment
                        )

                        should("not match") {
                            evaluate(
                                world,
                                context
                            ).shouldNotMatch()
                        }
                    }

                    context("empty list") {
                        val context = Context(JsonArray(), experiment)

                        should("not match") {
                            evaluate(
                                world,
                                context
                            ).shouldNotMatch()
                        }
                    }
                }
            }
        }

        context("with children") {
            context("one matching") {
                val matchingVariant = Experiment(
                    Variant("control"),
                    Always(true),
                    emptyList()
                )
                val notMatchingVariant = Experiment(
                    Variant("experiment"),
                    Always(false),
                    emptyList()
                )

                val experiment = Experiment(
                    Variant("EXP001"),
                    Always(true),
                    listOf(matchingVariant, notMatchingVariant)
                )

                should("match the one children") {
                    evaluate(
                        world,
                        Context(JsonNull.INSTANCE, experiment)
                    ).shouldMatch(experiment.name, matchingVariant.name)
                }
            }

            context("two matching") {
                val firstMatchingVariant = Experiment(
                    Variant("active"),
                    Always(true),
                    emptyList()
                )
                val secondMatchingVariant = Experiment(
                    Variant("control"),
                    Always(true),
                    emptyList()
                )

                val experiment = Experiment(
                    Variant("EXP001"),
                    Always(true),
                    listOf(firstMatchingVariant, secondMatchingVariant)
                )

                should("match the first children") {
                    evaluate(
                        world,
                        Context(JsonNull.INSTANCE, experiment)
                    ).shouldMatch(experiment.name, firstMatchingVariant.name)
                }
            }

            context("no matching children") {
                val notMatching = Experiment(
                    Variant("control"),
                    Always(false),
                    emptyList()
                )

                val experiment = Experiment(
                    Variant("EXP001"),
                    Always(true),
                    listOf(notMatching)
                )

                should("does not match the whole experiment") {
                    evaluate(
                        world,
                        Context(JsonNull.INSTANCE, experiment)
                    ).shouldMatch(experiment.name)
                }
            }

            context("parent doesn't match") {

                val experiment = Experiment(
                    Variant("EXP001"),
                    Always(false),
                    listOf(
                        Experiment(
                            Variant("control"),
                            Always(true),
                            emptyList()
                        )
                    )
                )

                should("does not match the whole experiment") {
                    evaluate(
                        world,
                        Context(JsonNull.INSTANCE, experiment)
                    ).shouldNotMatch()
                }
            }
        }
    }
})

private fun <T> List<T>.shouldMatch(vararg expected: T) {
    return this.shouldBe(expected)
}

private fun List<Variant>.shouldNotMatch() {
    return withClue("The experiments should be empty") {
        this.shouldBeEmpty()
    }
}