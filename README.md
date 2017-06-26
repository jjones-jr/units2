# units2

A Clojure library for quantities with units.


## Rationale

Few languages have built-in support for numerical quantities in a system of units. This library brings Clojure into the world of unit-aware computing (https://xkcd.com/1643/); in fact, it abstracts away as much of the unit bookkeeping as possible in a functional, lispy way.

## Illustrative Code Snippet

```clojure
;; a function that computes an average, no matter what the units are

(with-unit-arithmetic
    (defn average [a b]
        (/ (+ a b) 2)))

;; assume sec, minute, day are units in the namespace.

(let [f (comp hour average)] ; compute average and return the result in units of hours
    (list
        (+ (f 4 5) (hour 1))
        (f (minute 180) (day 0.5))
        (average 4 5))) ; --> 5.5 hours, 7.5 hours, 4.5

;; Note that dimensional analysis, unit conversions and coercions all happen automatically.
;; Also note that the unit-aware `average` behaves normally on normal clojure numbers.
```

## Features

The aims of `units2` are to be highly expressive, unintrusive, and easy to use. The salient points of `units2` are:

+ A lispy syntax for first-class units that you can `map`, `comp`, etc.
+ New units can be defined
    + in reusable namespaces that collect them by application, and
    + at runtime, anonymously, even within local scope
+ Augmented math ops (`+`,`-`,`*`,`/`,`==`,  etc.), accessible
    + as namespace-qualified symbols (idiomatic clojure), and
    + within the scope of a context-creating macro (idiomatic in any lisp)
+ Dimensional analysis can be extended by the user (even at runtime!)
+ Decorators for adding unit-awareness to users' math ops and numerical calculus schemes

This library also respects the distinction between the algebra on quantities with units and the algebra on units themselves. This is an important prerequisite for dimensional analysis, nonlinear unit conversions (e.g. celsius-fahrenheit), and automatic 'unitification' of numerical differentiation and integration schemes.


## Tutorial / Sample Code / Documentation

There's a **tutorial** to help get started with the library, and some example code in `spice.clj`. There's also a summary of the API (more of a cheatsheet really). Some effort was put into writing readable, well-commented code, so please read the Marginalia (html) output in `docs/`!


## Etymology of *Unit*

No Clojure library is complete without a bit of etymology.

*Unit* is an alteration of *unity*, from Old French *unite* ("uniqueness, oneness", c. 1200), from the Latin *unitas* (cf. below), itself derived from *unus* ("one"). *Unit* was popularized by the English translation of Euclid's Elements (1570), to express the Greek *monos* ("unique, solitary"). Extended sense of "a quantity adopted as a standard of measure" is from 1738.

Originally *unitas*, *unitatis*, (f), "oneness, sameness, agreement". More explicitly:

1. quality of that which has no parts, or forms a whole out of parts
2. quantity considered as an elementary piece for constructing all others
3. quantity chosen as a term of comparison, to evaluate others of the same sort
4. conformity of sentiment, accord, harmony
