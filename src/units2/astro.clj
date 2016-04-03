(ns units2.astro
  (:require [units2.core :refer :all]
            [units2.IFnUnit :refer :all])
  (:import (javax.measure.unit Unit SI NonSI)))

(set! *warn-on-reflection* true)

;; ## Length [L]


(defunit-with-SI-prefixes pc (->IFnUnit NonSI/PARSEC))
(defunit-with-SI-prefixes m (->IFnUnit SI/METER))

(defunit AU (->IFnUnit NonSI/ASTRONOMICAL_UNIT))

;; ## Time [T]


;; Seconds are not shortened to `s` as in the SI because `ns` is the Clojure namspace macro. The astrophsyical sec is `arcsec`.

(defunit-with-SI-prefixes sec (->IFnUnit SI/SECOND))
(defunit-with-SI-prefixes yr (->IFnUnit NonSI/YEAR_SIDEREAL))

;; ## Mass [M]


(defunit-with-SI-prefixes g (->IFnUnit SI/GRAM))
(defunit Msol (AsUnit (kg 1.98855E30)))

;; ## Charge [Q]

(defunit coulomb (->IFnUnit SI/COULOMB))
(defunit positroncharge (AsUnit (coulomb 1.6022e-19)))

;; ## Temperature [K]

(defunit-with-SI-prefixes K (->IFnUnit SI/KELVIN))

;; ## (Solid) Angles


(defunit rad (->IFnUnit SI/RADIAN))
(defunit deg (->IFnUnit NonSI/DEGREE_ANGLE))
(defunit arcsec (->IFnUnit NonSI/SECOND_ANGLE))
(defunit as (->IFnUnit NonSI/SECOND_ANGLE))

(defunit sr (->IFnUnit SI/STERADIAN))
(defunit sky (rescale sr (* 4 Math/PI)))


;; ## Velocity [L/T]

(defunit lightspeed (rescale (divide m sec) 2.9979246e8))


;; ## Energy


(defunit-with-SI-prefixes eV (->IFnUnit NonSI/ELECTRON_VOLT))
(defunit-with-SI-prefixes J (->IFnUnit SI/JOULE))
(defunit erg (->IFnUnit NonSI/ERG))
;; ## Luminosity (radiant flux)


(defunit Lsol (AsUnit (->amount 3.846E26 (->IFnUnit SI/WATT))))

;; The next few units use cm and seconds because they are instrumentalists' units.

;; ## Flux

(defunit Flux (unit-from-powers {cm -2 sec -1}))
(defunit Intensity (unit-from-powers {cm -2 sec -1 sr -1}))

;; ## Spectral Flux

(defunit SpectralFlux (unit-from-powers {cm -2 sec -1 GeV -1}))
(defunit SpectralIntensity (unit-from-powers {cm -2 sec -1 sr -1 GeV -1}))

;; ## Spectral Irradiance (spectral flux density)


(defunit-with-SI-prefixes Jansky (rescale (unit-from-powers {(->IFnUnit SI/WATT) 1  m -2 (->IFnUnit SI/HERTZ) -1}) 1E-26))

;; From this point onwards, units are dimension-free. However, angles aren't redshifts or probabilities! We should try to guarantee we're not allowing silly conversions.

;; ## Redshifts can be given as z or as 1+z, both are useful in cosmology.


(defunit zee (->IFnUnit (.alternate (.divide SI/METER SI/METER) "zee")))
(defunit onepluszee (->IFnUnit (.plus ^Unit (implementation-hook zee) -1)))

;; ## No units

(defunit dimensionless (divide pc pc)) ;; define this last, otherwise "dimensionless" will be `sr` or `zee`... :-D