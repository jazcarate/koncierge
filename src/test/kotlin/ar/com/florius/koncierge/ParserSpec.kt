package ar.com.florius.koncierge

import com.google.gson.JsonPrimitive
import com.google.gson.internal.LazilyParsedNumber
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.ShouldSpec
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime


class ParserSpec : ShouldSpec({

    context("parsing all") {
        context("not an object") {
            should("fail to parse") {
                parseAll("[]") shouldBeLeft ParseError("The first level of experiment definition needs to be an object. Got [[]]")
            }
        }

        context("an empty object") {
            should("fail to parse") {
                parseAll("{}") shouldBeLeft ParseError("There needs to be at least one experiment definition. Got [{}]")
            }
        }

        context("an object with a wrong formatted key") {
            should("fail to parse") {
                parseAll("{ foo: 3 }") shouldBeLeft ParseError("Expecting an object. Got [3]")
            }
        }

        context("an object with no extra keys") {
            should("defaults to an always on evaluation") {
                parseAll("{ foo: {} }") shouldBeRight listOf(Experiment(Variant("foo"), Always(true), emptyList()))
            }
        }

        context("a evaluation of an always") {
            context("not a boolean") {
                should("fail to parse") {
                    parseAll("{ foo: { \$always: 3 } }") shouldBeLeft ParseError("Expecting a boolean. Got [3]")
                }
            }

            context("with a boolean") {
                should("parse the evaluation") {
                    parseAll("{ foo: { \$always: true } }") shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            Always(true),
                            emptyList()
                        )
                    )
                }
            }
        }

        context("a evaluation of an gt") {
            context("not a primitive") {
                should("fail to parse") {
                    parseAll("{ foo: { \$gt: {} } }") shouldBeLeft ParseError("Expecting a primitive. Got [{}]")
                }
            }

            context("not a number") {
                should("fail to parse") {
                    parseAll("{ foo: { \$gt: 'foo' } }") shouldBeLeft ParseError("Expecting a number. Got [\"foo\"]")
                }
            }

            context("with a number") {
                should("parse the evaluation") {
                    parseAll("{ foo: { \$gt: 3 } }") shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            GreaterThan(LazilyParsedNumber("3")),
                            emptyList()
                        )
                    )
                }
            }

            context("with a date") {
                context("in the simple format") {
                    should("parse the evaluation") {
                        parseAll("{ foo: { \$gt: '2020-10-05' } }") shouldBeRight listOf(
                            Experiment(
                                Variant("foo"),
                                GreaterThan(
                                    ZonedDateTime.of(2020, 10, 5, 0, 0, 0, 0, ZoneId.systemDefault()).toEpochSecond()
                                ),
                                emptyList()
                            )
                        )
                    }
                }

                context("in the JavaScript format") {
                    should("parse the evaluation") {
                        parseAll("{ foo: { \$gt: 'Tue Oct 06 2020 00:30:00 GMT+0200 (Central European Summer Time)' } }") shouldBeRight listOf(
                            Experiment(
                                Variant("foo"),
                                GreaterThan(
                                    ZonedDateTime.of(2020, 10, 6, 0, 30, 0, 0, ZoneOffset.ofHours(2)).toEpochSecond()
                                ),
                                emptyList()
                            )
                        )
                    }
                }

                context("in the ISO 8601 format") {
                    should("parse the evaluation") {
                        parseAll("{ foo: { \$gt: '2020-10-05T22:20:00.000+0200' } }") shouldBeRight listOf(
                            Experiment(
                                Variant("foo"),
                                GreaterThan(
                                    ZonedDateTime.of(2020, 10, 5, 22, 20, 0, 0, ZoneOffset.ofHours(2)).toEpochSecond()
                                ),
                                emptyList()
                            )
                        )
                    }
                }
            }
        }

        context("with multiple conditions") {
            val and = "{ foo: { \$always: false, \$gt: 3 } }"

            should("parse the evaluation") {
                parseAll(and) shouldBeRight listOf(
                    Experiment(
                        Variant("foo"),
                        And(listOf(Always(false), GreaterThan(LazilyParsedNumber("3")))),
                        emptyList()
                    )
                )
            }
        }

        context("a evaluation of an lt") {
            context("not a number") {
                should("fail to parse") {
                    parseAll("{ foo: { \$lt: {} } }") shouldBeLeft ParseError("Expecting a primitive. Got [{}]")
                }
            }

            context("with a number") {
                should("parse the evaluation") {
                    parseAll("{ foo: { \$lt: 3 } }") shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            LessThan(LazilyParsedNumber("3")),
                            emptyList()
                        )
                    )
                }
            }

            context("with a date") {
                context("in the simple format") {
                    should("parse the evaluation") {
                        parseAll("{ foo: { \$lt: '2020-10-05' } }") shouldBeRight listOf(
                            Experiment(
                                Variant("foo"),
                                LessThan(
                                    ZonedDateTime.of(2020, 10, 5, 0, 0, 0, 0, ZoneId.systemDefault()).toEpochSecond()
                                ),
                                emptyList()
                            )
                        )
                    }
                }

                context("in the JavaScript format") {
                    should("parse the evaluation") {
                        parseAll("{ foo: { \$lt: 'Tue Oct 06 2020 00:30:00 GMT+0200 (Central European Summer Time)' } }") shouldBeRight listOf(
                            Experiment(
                                Variant("foo"),
                                LessThan(
                                    ZonedDateTime.of(2020, 10, 6, 0, 30, 0, 0, ZoneOffset.ofHours(2)).toEpochSecond()
                                ),
                                emptyList()
                            )
                        )
                    }
                }

                context("in the ISO 8601 format") {
                    should("parse the evaluation") {
                        parseAll("{ foo: { \$lt: '2020-10-05T22:20:00.000+0200' } }") shouldBeRight listOf(
                            Experiment(
                                Variant("foo"),
                                LessThan(
                                    ZonedDateTime.of(2020, 10, 5, 22, 20, 0, 0, ZoneOffset.ofHours(2)).toEpochSecond()
                                ),
                                emptyList()
                            )
                        )
                    }
                }
            }
        }

        context("a evaluation of and") {
            context("with a mal formatted operation") {
                val badFormatted = "{ foo: { \$and: 3 } }"

                should("fail to parse") {
                    parseAll(badFormatted) shouldBeLeft ParseError("Expecting an object. Got [3]")
                }
            }

            context("with a well formatted operation") {
                val and = "{ foo: { \$and: { \$gt: 3, \$always: false } } }"

                should("parse the evaluation") {
                    parseAll(and) shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            And(
                                listOf(
                                    GreaterThan(LazilyParsedNumber("3")), Always(false)
                                )
                            ),
                            emptyList()
                        )
                    )
                }
            }
        }

        context("a evaluation of or") {
            context("with a mal formatted operation") {
                val badFormatted = "{ foo: { \$or: 3 } }"

                should("fail to parse") {
                    parseAll(badFormatted) shouldBeLeft ParseError("Expecting an object. Got [3]")
                }
            }

            context("with a well formatted operation") {
                val and = "{ foo: { \$or: { \$gt: 3, \$always: false } } }"

                should("parse the evaluation") {
                    parseAll(and) shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            Or(
                                listOf(
                                    GreaterThan(LazilyParsedNumber("3")), Always(false)
                                )
                            ),
                            emptyList()
                        )
                    )
                }
            }
        }

        context("a evaluation of any") {
            context("with a mal formatted operation") {
                val badFormatted = "{ foo: { \$any: 3 } }"

                should("fail to parse") {
                    parseAll(badFormatted) shouldBeLeft ParseError("Expecting an object. Got [3]")
                }
            }

            context("with a well formatted operation") {
                val and = "{ foo: { \$any: { \$always: false } } }"

                should("parse the evaluation") {
                    parseAll(and) shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            Any(Always(false)),
                            emptyList()
                        )
                    )
                }
            }

            context("with multiple conditions") {
                val and = "{ foo: { \$any: { \$always: false, \$gt: 3 } } }"

                should("parse the evaluation") {
                    parseAll(and) shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            Any(And(listOf(Always(false), GreaterThan(LazilyParsedNumber("3"))))),
                            emptyList()
                        )
                    )
                }
            }
        }

        context("a evaluation of all") {
            context("with a mal formatted operation") {
                val badFormatted = "{ foo: { \$all: 3 } }"

                should("fail to parse") {
                    parseAll(badFormatted) shouldBeLeft ParseError("Expecting an object. Got [3]")
                }
            }

            context("with a well formatted operation") {
                val and = "{ foo: { \$all: { \$always: false } } }"

                should("parse the evaluation") {
                    parseAll(and) shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            All(Always(false)),
                            emptyList()
                        )
                    )
                }
            }

            context("with multiple conditions") {
                val and = "{ foo: { \$all: { \$always: false, \$gt: 3 } } }"

                should("parse the evaluation") {
                    parseAll(and) shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            All(And(listOf(Always(false), GreaterThan(LazilyParsedNumber("3"))))),
                            emptyList()
                        )
                    )
                }
            }
        }

        context("a evaluation of a bind") {
            context("defaults to equal") {
                should("parse the evaluation") {
                    parseAll("{ foo: { 'bar': 'yes' } }") shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            Bind(Dive("bar"), Equal(JsonPrimitive("yes"))),
                            emptyList()
                        )
                    )
                }
            }

            context("a evaluation of single dive") {
                should("parse the evaluation") {
                    parseAll("{ foo: { bar: { \$always: true } } }") shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            Bind(Dive("bar"), Always(true)),
                            emptyList()
                        )
                    )
                }
            }

            context("can compose dives") {
                should("parse the evaluation") {
                    parseAll("{ foo: { bar: { biz: { \$always: true } } } }") shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            Bind(Dive("bar"), Bind(Dive("biz"), Always(true))),
                            emptyList()
                        )
                    )
                }
            }

            context("can compose dives with dot notation") {
                should("parse the evaluation") {
                    parseAll("{ foo: { 'bar.biz': { \$always: true } } }") shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            Bind(Dive("bar"), Bind(Dive("biz"), Always(true))),
                            emptyList()
                        )
                    )
                }
            }

            context("can dive into array by numbers") {
                should("parse the evaluation") {
                    parseAll("{ foo: { '3': { \$always: true } } }") shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            Bind(Dive("3"), Always(true)),
                            emptyList()
                        )
                    )
                }
            }

            context("a change of a random") {
                should("parse the evaluation") {
                    parseAll("{ foo: { \$rand: { \$always: true } } }") shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            Bind(Random, Always(true)),
                            emptyList()
                        )
                    )
                }
            }

            context("a change of a chaos") {
                should("chaos the evaluation") {
                    parseAll("{ foo: { \$chaos: { \$always: true } } }") shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            Bind(Chaos, Always(true)),
                            emptyList()
                        )
                    )
                }
            }

            context("a change of a date") {
                should("parse the evaluation") {
                    parseAll("{ foo: { \$date: { \$always: true } } }") shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            Bind(Date, Always(true)),
                            emptyList()
                        )
                    )
                }
            }

            context("a change of a size") {
                should("parse the evaluation") {
                    parseAll("{ foo: { \$size: { \$always: true } } }") shouldBeRight listOf(
                        Experiment(
                            Variant("foo"),
                            Bind(Size, Always(true)),
                            emptyList()
                        )
                    )
                }
            }
        }

        context("a evaluation of a equal") {
            should("parse the evaluation") {
                parseAll("{ foo: { \$eq: { biz: '3' } } }") shouldBeRight listOf(
                    Experiment(
                        Variant("foo"),
                        Equal(jsonObject("biz" to "3")),
                        emptyList()
                    )
                )
            }
        }

        context("some child variants") {
            should("parse the children recursively") {
                parseAll("{ foo: { \$children: { bar: {} }} }") shouldBeRight
                        listOf(
                            Experiment(
                                Variant("foo"), Always(true), listOf(
                                    Experiment(
                                        Variant("bar"), Always(true), emptyList()
                                    )
                                )
                            )
                        )
            }
        }
    }
})