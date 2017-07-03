# units2 -- design

## Why units2?

Quantitative domain-specific problems typically involve domain-specific units of measurement. My scientific research had me commenting and converting and juggling units manually for a while. My code was getting ugly. I decided there must be a better way: I should be writing *regular Clojure code* that deals with units automatically and behind-the-scenes (except when I'm explicitly working with them as first-class citizens of the language).

There were already unit libraries on offer: `frinj`, `minderbinder`, `meajure`, and what I guess are a few abandonned personal projects. However, they didn't do what I wanted. `frinj` and `minderbinder` can do unit conversions but can't do maths (and their DSLs are very unLISPy). `meajure` can do maths, but not conversions (and doesn't do dimensional analysis, either). None of these treat units of measurement as first-class citizens. And although the problem of units has obviously already been solved many times over the decades in LISP-family languages, it didn't occur to me to check.

Fortunately, there was a well-designed implementation of units in the Java standard library: JSR-363 (revised JSR-275) had been accepted a few months prior as `javax.measure`. So, I wrote some Java interop, cleaned up my code, and did my science. My wrapper code coevolved along with my science code for a while -- the science code got cleaner and cleaner as the `units` code got uglier and uglier. Eventually, a complete rewrite was necessary. To avoid breaking my science code, I copied `units` into a folder called `units2`.

I tell this story as a way to introduce the use case for this library: my science code needed (i) to crunch lots of numbers with units and perform lots of conversions, all while (ii) talking to external libraries that don't know about units. That said, I've used it at the REPL for short calculations, too.

## Design Choices

### Exceptions
Clojure is infamous for its cryptic error messages. Ideally Clojure would have a condition system based on Common Lisp's and designed for concurrency/parallelism/awesomeness. Instead, `units2` throws Exceptions.

We chose not to use the exceptions defined in JSR-363, nor to define our own, since predefined `java.lang` exceptions are easier to `try`/`catch`. `units2` relies on a combination of IllegalArgument and UnsupportedOperation exceptions, and always tries to explain which data are causing the error to be thrown.

Note that ArithmeticExceptions and ArityExceptions may occur when using the decorated versions of some clojure math ops. This behaviour is not prescribed by the `units2` API, but an accident of the behaviour of the undecorated functions they are built from (e.g. `clojure.core//` and `clojure.core/min`).

### IFnUnits with `deftype` instead of Clojure's builtin data structures.
There are two obvious way to add units to clojure: some builtin data structure (representing units as data) or metadata. TODO: DISCUSS


### Printing Quantities with Units
The units in `units2` implement `IFn`. Even though the s-expression `(m 7)` is not self-evaluating and not atomic, it's basically a literal with the value "seven meters". Indeed, printing quantities with units as `(<unit> <value>)` means that it's easy to read them back into clojure after they're printed into character streams, even as parts of a nested data structure or an autogenerated source file.

So, why not print `#=(m 7)`? Firstly, this doesn't work, it'd have to be `#=(->amount 7 m)`. Secondly, `#=` isn't an officially supported feature of the Clojure reader, so it'd have to be `#units2.core.amount[7 m]`. But even that doesn't really work, because fooling with the reader always seems to break easy things related to referential transparency that are trivially solved with `(<unit> <value>)`.

### Rational exponents in dimensional analysis
While trying to justify why rational exponents weren't supported (in favour of just integers), I convinced myself it's probably OK to have them as long as these are only accessible through `(ops/expt ... [rational?])` and explicit construction via the `power` and `root`. Basically, LISP languages are good because

<center>
LISP == Freedom == Good
</center>

That said, there are usually *conceptual* reasons to try to stick to integer exponents, tied to the details of whatever domain-specific problem one might be trying to solve.

### This smells like a Type System...
A lot of this library, and maybe all of it, could be done with static types. Just look at how units are handled in F#. However, having units as a first-class citizen of the language makes programming a lot more expressive (i.e. easier). Having a bunch of first-class types (rather than one new type for a bunch of first-class units as in `units2`) somehow seems immoral and silly at the same time.

Of course, this means we're not doing dimensional analysis at compile-time. Fortunately, we have `clojure.spec`... TODO: DISCUSS