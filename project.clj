(defproject logprocessor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[amazonica "0.3.9"]
                 [clj-time "0.9.0"]
                 [clj-yaml "0.4.0"]
                 [clojurewerkz/elastisch "2.2.1"]
                 [com.climate/claypoole "1.1.2"]
                 [com.github.kyleburton/clj-xpath "1.4.5"]
                 [com.stuartsierra/component "0.3.1"]
                 [fs "1.3.3"]
                 [http-kit "2.1.9"]
                 [joda-time/joda-time "2.9.2"]
                 [manifold "0.1.3-alpha2"]
                 [midje "1.8.3"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/data.json "0.2.6"]
                 [com.cemerick/piggieback "0.2.1"]
                 [figwheel-sidecar "0.5.2-SNAPSHOT"]
                 [reagent "0.5.1"]]
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :main ^:skip-aot logprocessor.core
  :target-path "target/%s"
  :profiles {:dev {:source-paths ["dev"]}
             :uberjar {:aot :all}})
