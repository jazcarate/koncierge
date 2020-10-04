package ar.com.florius.koncierge

import com.google.gson.*
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import java.time.ZoneOffset
import java.time.ZonedDateTime


class DefinitionSpec : ShouldSpec({

    context("in a world") {
        val now = ZonedDateTime.of(2020, 10, 4, 14, 0, 0, 0, ZoneOffset.UTC)
        var chaos = 4 // chosen by fair dice roll.

        val world = object : World {
            override fun getDate(): ZonedDateTime {
                return now
            }

            override fun genGson(): Gson {
                return GsonBuilder()
                    .create()
            }

            override fun genChaos(): Number {
                return chaos
            }
        }

        context("with no children") {
            context("experiment with a bind") {
                val experiment = buildExperiment(
                    Bind(
                        Dive("beta"),
                        Equal("yes")
                    )
                )

                context("empty context") {
                    should("fails to dive") {
                        val context = Context(JsonNull.INSTANCE)

                        run(
                            world,
                            context,
                            experiment
                        ) shouldBeLeft EvalError("Can only dive in objects. null is neither")
                    }
                }

                context("context with the key to dive") {
                    should("dive and call the evaluator") {
                        val contextJson = JsonObject()
                        contextJson.addProperty("beta", "yes")
                        val context = Context(contextJson)

                        run(
                            world,
                            context,
                            experiment
                        ) shouldBeRight listOf(Variant("EXP001"))
                    }

                    should("valuate to a disabled") {
                        val contextJson = JsonObject()
                        contextJson.addProperty("beta", "no")
                        val context = Context(contextJson)

                        run(
                            world,
                            context,
                            experiment
                        ) shouldBeRight emptyList()
                    }
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
                        run(
                            world,
                            Context(JsonNull.INSTANCE),
                            experiment
                        ) shouldBeRight emptyList()
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
                        run(
                            world,
                            Context(JsonNull.INSTANCE),
                            experiment
                        ) shouldBeRight listOf(Variant("EXP001"))
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
                        run(
                            world,
                            Context(JsonNull.INSTANCE),
                            experiment
                        ) shouldBeRight listOf(Variant("EXP001"))
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
                        run(
                            world,
                            Context(JsonNull.INSTANCE),
                            experiment
                        ) shouldBeRight emptyList()
                    }
                }
            }

            context("experiment with a always context") {
                context("checking true == true") {
                    val experiment = buildExperiment(
                        Always(true)
                    )

                    should("matches the experiment") {
                        run(
                            world,
                            Context(JsonNull.INSTANCE),
                            experiment
                        ) shouldBeRight listOf(experiment.name)
                    }
                }

                context("checking false == true") {
                    val experiment = buildExperiment(
                        Always(false)
                    )

                    should("not match") {
                        run(
                            world,
                            Context(JsonNull.INSTANCE),
                            experiment
                        ) shouldBeRight emptyList()
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
                        run(
                            world,
                            Context(JsonNull.INSTANCE),
                            experiment
                        ) shouldBeRight listOf(experiment.name)
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
                        run(
                            world,
                            Context(JsonNull.INSTANCE),
                            experiment
                        ) shouldBeRight emptyList()
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
                        run(
                            world,
                            Context(JsonNull.INSTANCE),
                            experiment
                        ) shouldBeRight emptyList()
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
                        run(
                            world,
                            Context(JsonNull.INSTANCE),
                            experiment
                        ) shouldBeRight listOf(experiment.name)
                    }
                }
            }

            context("experiment with an random") {
                val experiment = buildExperiment(
                    Bind(
                        Random,
                        Equal(0.0015799436F) // Generated once for `null` context
                    )
                )

                context("with a empty context") {
                    val context = Context(JsonNull.INSTANCE)

                    should("matches the experiment") {
                        run(
                            world,
                            context,
                            experiment
                        ) shouldBeRight listOf(experiment.name)
                    }
                }

                context("with a different context") {
                    val context = Context(JsonPrimitive("foo"))

                    should("not match\"") {
                        run(
                            world,
                            context,
                            experiment
                        ) shouldBeRight emptyList()
                    }
                }

                should("the same context should yeild the same result") {
                    val res1 = run(world, Context(JsonNull.INSTANCE), experiment)
                    val res2 = run(world, Context(JsonNull.INSTANCE), experiment)

                    res1 shouldBeRight listOf(experiment.name)
                    res1 shouldBe res2
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
                    chaos = 1
                    val res1 = run(world, Context(JsonNull.INSTANCE), experiment)
                    chaos = 0
                    val res2 = run(world, Context(JsonNull.INSTANCE), experiment)


                    res1 shouldBeRight listOf(experiment.name)
                    res2 shouldBeRight emptyList()
                }
            }

            context("experiment with an id") {
                context("a silly experiment") {
                    val experiment = buildExperiment(
                        Bind(
                            Id,
                            Equal("yes")
                        )
                    )

                    should("match the same context") {
                        run(
                            world,
                            Context(JsonPrimitive("yes")),
                            experiment
                        ) shouldBeRight listOf(experiment.name)
                    }

                    should("should not match other context") {
                        run(
                            world,
                            Context(JsonPrimitive("no")),
                            experiment
                        ) shouldBeRight emptyList()
                    }
                }
            }

            context("experiment with an compose") {
                context("a compose of dives") {
                    val experiment = buildExperiment(
                        Bind(
                            Compose(
                                Dive("foo"),
                                Dive("bar")
                            ),
                            Equal("yes")
                        )
                    )

                    context("an empty context") {
                        val context = Context(JsonNull.INSTANCE)

                        should("fail to dive") {
                            run(
                                world,
                                context,
                                experiment
                            ) shouldBeLeft EvalError("Can only dive in objects. null is neither")
                        }
                    }

                    context("a context with only the first layer of dive") {
                        val contextJson = JsonObject()
                        contextJson.addProperty("foo", "yes")
                        val context = Context(contextJson)

                        should("fail to dive deep") {
                            run(
                                world,
                                context,
                                experiment
                            ) shouldBeLeft EvalError("Can only dive in objects. \"yes\" is neither")
                        }
                    }

                    context("a context with all the layers of dive") {
                        val innerObject = JsonObject()
                        innerObject.addProperty("bar", "yes")

                        val contextJson = JsonObject()
                        contextJson.add("foo", innerObject)
                        val context = Context(contextJson)

                        should("match the deep level") {
                            run(
                                world,
                                context,
                                experiment
                            ) shouldBeRight listOf(experiment.name)
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
                    run(
                        world,
                        Context(JsonNull.INSTANCE),
                        experiment
                    ) shouldBeRight listOf(experiment.name, matchingVariant.name)
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
                    run(
                        world,
                        Context(JsonNull.INSTANCE),
                        experiment
                    ) shouldBeRight listOf(experiment.name, firstMatchingVariant.name)
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
                    run(
                        world,
                        Context(JsonNull.INSTANCE),
                        experiment
                    ) shouldBeRight listOf(experiment.name)
                }
            }
        }
    }
})

private fun buildExperiment(evaluator: Evaluator): Experiment {
    return Experiment(
        Variant("EXP001"),
        evaluator,
        emptyList()
    )
}