(ns utsusu.core
  (:require
    [clojure.edn :as edn]
    [clojure.string :as s]
    [utsusu.transfer :as t]))

(def read-info
  {:source-domain "Source instance domain (github.com)"
   :source-user "Source user or org"
   :source-token "Source API token"
   :dest-domain "Destination instance domain (github.com)"
   :dest-user "Destination user or org"
   :dest-token "Destination API token"})

(defn- validate-config [config]
  (into
    {}
    (for [[k _] read-info]
      (let [v (config k)]
        (if (s/blank? v)
          (if (#{:source-domain :dest-domain} k)
            [k "github.com"] ; default domain
            (throw (Exception. (str (name k) " must not be blank."))))
          [k v])))))

(defn- read-config []
  (validate-config
    (try
      (edn/read-string (slurp "config.edn"))
      (catch Exception _
        (into {} (for [[k desc] read-info]
                   (do
                     (print (str desc ": "))
                     (let [v (read-line)]
                       [k v]))))))))

(defn -main []
  (t/transfer (read-config)))
