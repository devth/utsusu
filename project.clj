(defproject utsusu "0.1.2-SNAPSHOT"
  :description "A tool to transfer git repositories between GitHub and GitHub Enterprise"
  :url "https://github.com/devth/utsusu"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main utsusu.core
  :lein-release {:deploy-via :clojars}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [me.raynes/conch "0.6.0"]
                 [tentacles "0.2.6"]])
