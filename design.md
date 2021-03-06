# units2 -- design

## Why units2?

Quantitative domain-specific problems typically involve domain-specific units of measurement. My scientific research had me commenting and converting and juggling units manually for a while. My code was getting ugly. I decided there must be a better way: I should be writing *regular Clojure code* that deals with units automatically and behind the scenes (except when I'm explicitly working with them as first-class citizens of the language).

There were already unit libraries on offer: `frinj`, `minderbinder`, `meajure`, and what I guess are a few abandonned personal projects. However, they didn't do what I wanted. `frinj` and `minderbinder` can do unit conversions but can't do maths (and their DSLs are very unLISPy). `meajure` can do maths, but not conversions (and doesn't do dimensional analysis, either). None of these treat units of measurement as first-class citizens. And although the problem of units has obviously already been solved many times over the decades in LISP-family languages, it didn't occur to me to check.

Fortunately, there was a well-designed implementation of units in the Java standard library: JSR-363 (revised JSR-275) had been accepted a few months prior as `javax.measure`. So, I wrote some Java interop, cleaned up my code, and did my science. My wrapper code coevolved along with my science code for a while -- the science code got cleaner and cleaner as the `units` code got uglier and uglier. Eventually, a complete rewrite was necessary. To avoid breaking my science code, I copied `units` into a folder called `units2`.

I tell this story as a way to introduce the use case for this library. Although I've used it at the REPL for short calculations, my primary science code needed (i) to crunch lots of numbers with units and perform lots of conversions, all while (ii) talking to external libraries (for data analysis / plotting routines) that don't know about units.

## Design Choices

### Error Handling
Clojure is infamous for its cryptic error messages. Ideally Clojure would have a condition system based on Common Lisp's and designed for concurrency/parallelism/awesomeness. Instead, `units2` throws Exceptions.

We chose not to use the exceptions defined in JSR-363, nor to define our own, since predefined `java.lang` exceptions are easier to `try`/`catch`. `units2` relies on a combination of IllegalArgument and UnsupportedOperation exceptions, and always tries to explain which data are causing the error to be thrown.

Note that ArithmeticExceptions and ArityExceptions may occur when using the decorated versions of some clojure math ops. This behaviour is not prescribed by the `units2` API, but an accident of the behaviour of the undecorated functions they are built from (e.g. `clojure.core//` and `clojure.core/min`). Of course, the usual disclaimers when working with numbers in clojure apply:

    (/ 1 0) ; --> ArithmeticException
    (/ 1.0 0) ; --> Infinity

The next question is, why throw exceptions for every instance of failure? It simply turns out there are no instances where it'd be conceptually better to return `nil`, Clojure's "Not Found / Nothing" non-value. The IllegalArgument and UnsupportedOperation exceptions thrown by `units2` tell us the inputs to whatever throws the exception are conceptually bad inputs. It isn't the case that the value of `(ops/+ (minute 4) (meter 3))` exists but is meaningless; this operation is simply not well-defined. You can meaningfully look for the value associated to `:b` in `{:a 5 :c "j"}` and fail to find it, but you cannot meaningfully add distances to durations.

### Printing Dimensionful Quantities
The units in `units2` implement `IFn`. Even though the s-expression `(m 7)` is not self-evaluating and not atomic, it's basically a literal with the value "seven meters". Indeed, printing quantities with units as `(<unit> <value>)` means that it's easy to read them back into clojure after they're printed into character streams, even as parts of a nested data structure or an autogenerated source file.

So, why not print `#=(m 7)`? Firstly, this doesn't work, it'd have to be `#=(->amount 7 m)`. Secondly, `#=` isn't an officially supported feature of the Clojure reader, so it'd have to be `#units2.core.amount[7 m]`. But even that doesn't really work, because fooling with the reader always seems to break easy things related to referential transparency that are trivially solved with `(<unit> <value>)`.


### IFnUnits with `deftype` instead of Clojure's builtin data structures.
There are two obvious ways to add units to clojure: use some builtin data structure (representing units as data) or use vars' metadata.

#### Not map
The obvious approach for amounts with units is to have a map, like

    {meter-unit 40, centimeter-unit 4000, ...}

In this case, one would simply use `get` instead of `getValue`, and conversions would `assoc` new representations of the amount to the map.

Unfortunately, in Clojure one often finds keywords as keys in a map. With maps the syntax `(unit amount)` and `(amount unit)` are naively expected to both mean `(get amount unit)`, while we'd like the former to mean something like `(assoc amount (to unit amount))`. This violates the principle of least surprise (even though the surprise is based on an inadequate understanding of the language).

More problematically, and more generally, maps have a behaviour under functions of collections like `assoc` and `seq` that isn't necessarily meaningful... what should we make of the object `{meter-unit 2, :name "Sally"}`? `{:height (meter 2), :name "John"}` seems both more informative and more idomatic. Why do `(seq amount)` and `(seq (new-unit amount))` return different sequences, even though the two maps being `seq`'d are supposed to represent the same physical quantity? If you want to explicitly manipulate a sequence of different representations of the same amount it is probably clearer to `((juxt unit1 ... unitN) amount)`.

But suppose that you wanted to make quantities unit-to-value maps anyways... the problem then is that `{meter-unit 2, year-unit 3}` is a well-defined and easily constructible map, but not a well-defined quantity. Nor is, for that matter, `{meter-unit 2, centimeter-unit 1}`. The design decision can be framed as follows: either allow such strange maps as valid quantities and force every user to work out the weird edge cases in their code (since no sensible default behaviour exists), or just expose the protocol. This lets the users who want maps do whatever they like.

Similarly, the non-collection nature of units themselves weighs against the implementation of units as collections. Of course, we can parse a bunch of unit-to-exponent pairs into a unit with `parse-unit`, but only by assuming we `reduce` these pairs with `times` when parsing.

Once we've decided we need a non-map data structure to represent units and dimensionful quantities, it's easy to argue for `deftype` rather than `defrecord`: We're defining new programming constructs, not new domain-specific persistent maps. Particularly, we don't want value-based equality for `amount`s (we want to define their equality *up to conversions* with `==`), and we want to hide the Javaworld `javax.measure.unit.Unit` our implementation relies on (it would be publicly exposed with `defrecord`).

These data structures are not domain-specific because units are -- or should be -- *everywhere*. It's not obvious whether the value `{:name "Alice", :age 14}` describes a child days or years ago. And it's also not obvious that the context will clarify this, especially if the value is something only slightly more involved like `{:channel-owner "Leo", :rate 14}`. Is that bit-per-second, Gigabyte-per-minute, or invalid-queries-per-thousand? It's better to have this information as part of the value itself (the Clojure way) than to leave it in a comment or docstring or (goodness forbid!) add another separate pair `:rate-unit "video-per-week"`.

Long story short: quantities with units aren't data, they're the building blocks of data.

#### Not metadata

We want to create anonymous objects, so being tied to vars is no good. Also, the unit is somehow part of the value we're trying to capture, not a description of the value.


### Rational exponents in dimensional analysis
While trying to justify why rational exponents weren't supported (in favour of just integers), I convinced myself it's probably OK to have them as long as these are only accessible through `(ops/expt ... [rational?])` and explicit construction via the `power` and `root`. Basically, LISP languages are good because LISP == Freedom == Good.

That said, there are usually *conceptual* reasons to try to stick to integer exponents, tied to the details of whatever domain-specific problem one might be trying to solve.

### Unit Parsing

`units2` sticks to the Clojure idiom of using EDN for representing the data you want to parse. You can also parse strings containing EDN, so you should often be able to `pr` or `prn` a data structure containing units or dimensionful quantities to a string or a file and read it back later.

### This smells like a Type System...
A lot of this library, and maybe all of it, could be done with static types. Just look at how units are handled in F#. However, having units as a first-class citizen of the language makes programming a lot more expressive (i.e. easier). Having a bunch of first-class types (rather than one new type for a bunch of first-class units as in `units2`) somehow seems immoral and silly at the same time.

Of course, this means we're not doing dimensional analysis at compile-time. This is somewhat mitigated by good testing practices. Fortunately, we have `clojure.spec`... TODO: DISCUSS
