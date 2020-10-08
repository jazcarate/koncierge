package ar.com.florius.koncierge.internal

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import io.kotest.core.spec.style.ShouldSpec
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
                        ) shouldBe emptyList()
                    }
                }

                context("context with the key to dive") {
                    should("dive and call the evaluator") {
                        val context = Context(jsonObject("beta" to "yes"))

                        run(
                            world,
                            context,
                            experiment
                        ) shouldBe listOf(Variant("EXP001"))
                    }

                    should("valuate to a disabled") {
                        val context = Context(jsonObject("beta" to "no"))

                        run(
                            world,
                            context,
                            experiment
                        ) shouldBe emptyList()
                    }
                }
            }

            context("bind of an array") {
                val experiment = buildExperiment(
                    Bind(
                        Dive("2"),
                        Equal(30)
                    )
                )

                should("dive by position") {
                    val context = Context(jsonArray(10, 20, 30))

                    run(
                        world,
                        context,
                        experiment
                    ) shouldBe listOf(Variant("EXP001"))
                }

                should("fail if not an array") {
                    val context = Context(jsonObject("beta" to "no"))

                    run(
                        world,
                        context,
                        experiment
                    ) shouldBe emptyList()
                }
            }

            context("dive of an object") {
                val experiment = buildExperiment(
                    Bind(
                        Dive("foo"),
                        Equal(30)
                    )
                )

                should("fail to dive into an array") {
                    val context = Context(jsonArray(10, 20, 30))

                    run(
                        world,
                        context,
                        experiment
                    ) shouldBe emptyList()
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
                        ) shouldBe emptyList()
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
                        ) shouldBe listOf(Variant("EXP001"))
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
                        ) shouldBe listOf(Variant("EXP001"))
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
                        ) shouldBe emptyList()
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
                        ) shouldBe listOf(experiment.name)
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
                        ) shouldBe emptyList()
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
                        ) shouldBe listOf(experiment.name)
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
                        ) shouldBe emptyList()
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
                        ) shouldBe emptyList()
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
                        ) shouldBe listOf(experiment.name)
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
                            val context = Context(jsonArray(1))

                            should("not match") {
                                run(
                                    world,
                                    context,
                                    experiment
                                ) shouldBe emptyList()
                            }
                        }

                        context("long") {
                            val context = Context(jsonArray(1, 2, 3))

                            should("not match") {
                                run(
                                    world,
                                    context,
                                    experiment
                                ) shouldBe listOf(experiment.name)
                            }
                        }
                    }

                    context("of an string") {
                        context("short") {
                            val context = Context(JsonPrimitive("h"))

                            should("not match") {
                                run(
                                    world,
                                    context,
                                    experiment
                                ) shouldBe emptyList()
                            }
                        }

                        context("long") {
                            val context = Context(JsonPrimitive("hello!"))

                            should("not match") {
                                run(
                                    world,
                                    context,
                                    experiment
                                ) shouldBe listOf(experiment.name)
                            }
                        }
                    }

                    context("of an object") {
                        context("short") {
                            val context = Context(jsonObject("a" to "1"))

                            should("not match") {
                                run(
                                    world,
                                    context,
                                    experiment
                                ) shouldBe emptyList()
                            }
                        }

                        context("long") {
                            val context = Context(jsonObject("a" to "1", "b" to "2", "c" to "3"))

                            should("not match") {
                                run(
                                    world,
                                    context,
                                    experiment
                                ) shouldBe listOf(experiment.name)
                            }
                        }
                    }

                    context("of null") {
                        val context = Context(JsonNull.INSTANCE)

                        should("not match") {
                            run(
                                world,
                                context,
                                experiment
                            ) shouldBe emptyList()
                        }
                    }

                    context("something else") {
                        val context = Context(JsonPrimitive(true))

                        should("fails to parse") {
                            run(
                                world,
                                context,
                                experiment
                            ) shouldBe emptyList()
                        }
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
                        ) shouldBe listOf(experiment.name)
                    }
                }

                context("with a different context") {
                    val context = Context(JsonPrimitive("foo"))

                    should("not match") {
                        run(
                            world,
                            context,
                            experiment
                        ) shouldBe emptyList()
                    }
                }

                should("the same context should yeild the same result") {
                    val res1 = run(world, Context(JsonNull.INSTANCE), experiment)
                    val res2 = run(world, Context(JsonNull.INSTANCE), experiment)

                    res1 shouldBe listOf(experiment.name)
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
                    chaos = 1.0F
                    val res1 = run(world, Context(JsonNull.INSTANCE), experiment)
                    chaos = 0.0F
                    val res2 = run(world, Context(JsonNull.INSTANCE), experiment)


                    res1 shouldBe listOf(experiment.name)
                    res2 shouldBe emptyList()
                }
            }

            context("experiment with an not") {
                val experiment = buildExperiment(
                    Not(Equal("yes"))
                )

                should("negates the predicate, matching") {
                    run(
                        world, Context(JsonNull.INSTANCE), experiment
                    ) shouldBe listOf(experiment.name)
                }

                should("negates the predicate, not matching") {
                    run(
                        world, Context(JsonPrimitive("yes")), experiment
                    ) shouldBe emptyList()
                }
            }

            context("experiment with an compose") {
                context("a compose of dives") {
                    val experiment = buildExperiment(
                        Bind(Dive("foo"), Bind(Dive("bar"), Equal("yes")))
                    )

                    context("an empty context") {
                        val context = Context(JsonNull.INSTANCE)

                        should("fail to dive") {
                            run(
                                world,
                                context,
                                experiment
                            ) shouldBe emptyList()
                        }
                    }

                    context("a context with only the first layer of dive") {
                        val context = Context(jsonObject("foo" to "yes"))

                        should("fail to dive deep") {
                            run(
                                world,
                                context,
                                experiment
                            ) shouldBe emptyList()
                        }
                    }

                    context("a context with all the layers of dive") {
                        val contextJson = JsonObject()
                        contextJson.add("foo", jsonObject("bar" to "yes"))
                        val context = Context(contextJson)

                        should("match the deep level") {
                            run(
                                world,
                                context,
                                experiment
                            ) shouldBe listOf(experiment.name)
                        }
                    }
                }
            }

            context("contexts with an any") {
                val experiment = buildExperiment(
                    Any(
                        Equal(3)
                    )
                )

                context("an array context") {
                    context("at least one matches") {
                        val context = Context(
                            jsonArray(3, 2)
                        )

                        should("match") {
                            run(
                                world,
                                context,
                                experiment
                            ) shouldBe listOf(experiment.name)
                        }
                    }

                    context("none matches") {
                        val context = Context(
                            jsonArray(2)
                        )

                        should("not match") {
                            run(
                                world,
                                context,
                                experiment
                            ) shouldBe emptyList()
                        }
                    }

                    context("empty list") {
                        val context = Context(JsonArray())

                        should("not match") {
                            run(
                                world,
                                context,
                                experiment
                            ) shouldBe emptyList()
                        }
                    }
                }

                context("not an array") {
                    context("fails to parse") {

                        should("match") {
                            run(
                                world,
                                Context(JsonPrimitive(3)),
                                experiment
                            ) shouldBe emptyList()
                        }
                    }
                }

                xcontext("an object context") {
                    context("at least one matches") {
                        val context = Context(
                            jsonObject("foo" to "3")
                        )

                        should("match") {
                            run(
                                world,
                                context,
                                experiment
                            ) shouldBe listOf(experiment.name)
                        }
                    }

                    context("none matches") {
                        val context = Context(
                            jsonArray(2)
                        )

                        should("not match") {
                            run(
                                world,
                                context,
                                experiment
                            ) shouldBe emptyList()
                        }
                    }

                    context("empty list") {
                        val context = Context(JsonArray())

                        should("not match") {
                            run(
                                world,
                                context,
                                experiment
                            ) shouldBe emptyList()
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
                    ) shouldBe listOf(experiment.name, matchingVariant.name)
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
                    ) shouldBe listOf(experiment.name, firstMatchingVariant.name)
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
                    ) shouldBe listOf(experiment.name)
                }
            }
        }
    }
})
