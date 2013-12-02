(ns utsusu.transfer
  (:require [tentacles.core]))

(def ^:dynamic *config*)

(defn transfer [config]
  (binding [*config* config]
    (prn "transfer" config)))
