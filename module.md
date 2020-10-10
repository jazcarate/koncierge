# Module koncierge

This library is trying to accommodate ease-of use for systems with no dependencies, whilst providing a _batteries included_
access to the inner workings.

# Package ar.com.florius.koncierge

Wrapper for simple usage of `koncierge`.

## Common usage

When you instantiate a `Koncierge` object, the effects _(generating a random number for `$chaos` and a date for `$date`)_ are managed by the `World` field.

`World` has sensible defaults, but you can change them for other purposes (e.g.: testing). Results are all wrapped in `inline class`'es so you don't need to worry about internal functional workings.
If you want to use the functional-style interfaces, refer to the `internal` package.

# Package ar.com.florius.koncierge.internal

Batteries included part.

## Abandon all hope, all yee who enter here!!

# Package ar.com.florius.koncierge.types

Types used throughout **koncierge**

# Package ar.com.florius.koncierge.internal.definition

The definition of the **koncierge**'s format.