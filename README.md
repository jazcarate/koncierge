# koncierge 🛎
![tests](https://github.com/jazcarate/koncierge/workflows/tests/badge.svg)

## Rationale
We have a microservice that is in charge of knowing whether a user should be part of an experiment or not,
and if they are, whether they should be in the control group, or the experiment group.

The reasons why a user might participate or not are very varied, and change drastically from experiment to experiment.

We want to have the experiment's microservice be somewhat agnostic of the rest of the microservice infrastructure;
so whoever consumes the information must provide enough information about the user for us to choose what experiments
they should be part of.

We found that knowing if a user participates in an experiment, and it figures out what variant they fall into are really
similar concepts, so we came up with the idea of having _"variants all the way"_. So an experiment is really just a variant
on the world. The same syntax can apply to narrow down the focus of the experiment. A good example of this can be found
in the [Variant all the way](#variant-all-the-way) example.

## Usage
The library is divided into two major [_namespaces_](https://en.wikipedia.org/wiki/Kan_extension).
1. Parse _(Left Kan)_.
1. Interpret _(Right Kan)_.

A sub-section of the interpretation is also to validate that the given context can be matched to the rules.

### Parser
A function that takes the full JSON definition of the experiments and transforms it into objects that can be then [interpreted](#interpreter)

### Interpreter
A function that takes the [parsed](#parser) definition, the context provided, and a [`World`](https://en.wikipedia.org/wiki/Dependency_injection)
and outputs the list of experiments (and variants) the context is part of

### Context validation
We validate the context in an interpreter-agnostic way.
This means that even though semantically, some rules we might never call (see the [Uncalled for](#uncalled-for) example),
the context will still need to match every possible child rule.

## Syntax
We borrowed heavily for [Mongo's query language](https://docs.mongodb.com/manual/tutorial/query-documents/).

### Operators
These are the fields that start with a `$`.
There are two families of operators, describe below.

### Context changers
These operations change the context, either narrowing down the provided context, or by plucking some information from the `World`.
Think of them as `:: Context -> Context`.

| Name               | Description                                                                                             | Example                                          |
|--------------------|---------------------------------------------------------------------------------------------------------| -------------------------------------------------|
| any non-`$` string | Will dive into the context to the key with the same name. (can deep dive by combining keys with `.`s)   | `{ "foo.bar": "yes" }`                           |
| `$rand`            | Chooses a value at random between 0 and 1. Uses the current context as seed[*](#randomness)             | `{ "$rand": { "$gt": 0.5 } }`                    |
| `$chaos`           | Chooses a value at random between 0 and 1. This, compared to ☝️does not use the current context       | `{ "$chaos": { "$gt": 0.5 } }`                    |
| `$date`            | Changes the context with the current date. Useful when convincing with other comparison operator        | `{ "$date": { "$gt":  "2020-01-01 15:00:00" } }` |
| `$size`            | Changes the context to the size of the current context. This can be an array length, a string length or an object number of fields, or `0` if `null` | `{ "$size": { "$gt":  "1" } }` |
| `$not`             | Negates the context.                                                                                    | `{ "$not": { "$gt":  "1" } }` |

#### Evaluators
These operations yield weather the context is or not par of the variant.
Think of them as `:: Context -> Bool`.

| Name               | Description                                                                                             | Example                                          |
|--------------------|---------------------------------------------------------------------------------------------------------| -------------------------------------------------|
| `$lt`, `$gt`       | Will be enabled if the context if less than (`lt`) or greater than (`gt`) the value provided [**](#date)| `{ "$gt": 0.5 }`                                 |
| `$or`              | Will be enabled if any of the sub-operators are enabled                                                 | `{ "$or": { "beta": "yes", "$rand" : 0.5 } }`    |
| `$always`          | Will always evaluate to its (boolean) value, regardless of the context. See the [exists](#exists) example| `{ "$always": true }`                            |
| `$any`, `$all`     | Will be enabled if (any/all) the elements in the context (which should be an array) are enabled         | `{ "$any": { "$gt": 0.5 } }`                     |

Where a context changer can be nested, evaluators need to be terminal.

Even though there are both `$and` and `$eq` operators; they are rarely used, as they can be expressed more concisely.
Refer to the [And](#and) example for more information.

## Examples
### Simple
Everyone is participating in the experiment`EXP001`, and only half of the users (chosen at random) will see the experiment.
```json
{
    "EXP001": {
        "$children": {
            "participating": { "$rand": { "$gt": 0.5 } },
            "control": { "$always":  true }
        }
    }
}
```

The output for half the user base will be: `EXP001.participating` and `EXP001.control` for the other half.

### Variant all the way
```json
{
    "EXP001": {
        "$date": { "$gt": "2020-01-01 15:00:00" },
        "$children": {
            "participating": { "$rand": { "$gt": 0.5 } },
            "control": { "$always":  true }
        }
    }
}
```

The output will be the same as the [simple](#simple) example, but only if it is later than the January first 2020.

### Missing children
```json
{
    "EXP001": {
        "$children": {
          "never": { "$always":  false }
        }
    }
}
```
The output will be `EXP001` even if it has no active child.
```json
{
    "EXP001": {
        "beta": {
          "$always":  true,
          "$not": null
        }
    }
}
```
The output will be `EXP001` if the context has a key `beta`, and it is not `null`.

### Exists

### And
```json
{
    "EXP001": {
        "$date": { "$gt": "2020-01-01 15:00:00" },
        "beta": "yes"
    }
}
```
The output will be `EXP001` if the context has a key `beta` as `yes` **and** is queried after January first 2020.

### Uncalled for
```json
{
   "EXP001": {
        "$children": {
            "control": { "$always":  true },
            "participating": { "$rand": { "$gt": 0.5 } }
        }
    }
}
```

This will **always** output `EXP001.control`, as we parse children rules sequentially.

### More examples
You can check out the `/test` folder for more examples and edge cases.

## Extras
### Date
Both `$gt` and `$lt` only work with numbers, so dates will be compared by their epoch second.
The parser can interpret some date formats (read more about the date formats [here](https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html)).
Formats with no timezone, we choose the default system timezone. This is not recommended.

| Format | Example | Notes |
|--------|---------|-----|
| `yyyy-MM-dd` | 2020-10-05 | The time is at 00:00 from the system's timezone |
| `EEE MMM dd yyyy HH:mm:ss 'GMT'Z (z)` | Tue Oct 06 2020 00:30:00 GMT+0200 (Central European Summer Time) | JavaScript's default `Date` format |
| `yyyy-MM-dd'T'HH:mm:ss.SSSZ` | 2020-10-05T22:20:00.000+0200 | **Recommended** [ISO 8601-2:2019](https://www.iso.org/standard/70908.html) standard |

### Randomness
Even though `$rand` and `$chaos` might look similar; they differ on the seed for its randomness.
With `$rand`, any random value generated with the same context, will be the same output.

For this reason, most og the times you'll want to narrow down the context before applying `$rand`.
For example, given this context:
```json
{
    "user_id": 3,
    "last_login": "2020-10-15T10:00:00.000Z"
}
```
the `last_login` will probably change throughout the time, but we need the experiment to be fixed by the `user_id`.
In such a scenario, we can write the experiment as such:
```json
{
    "EXP001": {
        "user_id": {
            "$rand": { "$gt": 0.5 }
        }
    }
}
``` 

Every time the `user_id: 3` queries the experiment, the `$rand` value will be the same.

In contrast, `$chaos` will generate a new value each time, so there is no guarantee in what variant `user_id: 3` will fall.


### TODO
1. Escape the `$` to be able to match to `$` keys, and the `.` in keys to match not-nested keys with `.`
