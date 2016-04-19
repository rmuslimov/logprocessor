(defproject logprocessor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[amazonica "0.3.9"]
                 [clj-time "0.9.0"]
                 [clj-yaml "0.4.0"]
                 [cljs-http "0.1.9"]
                 [clojurewerkz/elastisch "2.2.1"]
                 [com.cemerick/piggieback "0.2.1"]
                 [com.climate/claypoole "1.1.2"]
                 [com.github.kyleburton/clj-xpath "1.4.5"]
                 [com.stuartsierra/component "0.3.1"]
                 [compojure "1.5.0"]
                 [figwheel-sidecar "0.5.2-SNAPSHOT"]
                 [fs "1.3.3"]
                 [http-kit "2.1.9"]
                 [joda-time/joda-time "2.9.2"]
                 [liberator "0.14.0"]
                 [manifold "0.1.3-alpha2"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/data.json "0.2.6"]
                 [org.danielsz/system "0.2.0-SNAPSHOT"]
                 [re-com "0.8.0"]
                 [reagent "0.5.1"]
                 [reloaded.repl "0.2.1"]
                 [ring "1.4.0"]
                 [secretary "1.2.3"]
                 [org.clojure/tools.nrepl "0.2.11"]
                 [environ "1.0.2"]]
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :main ^:skip-aot logprocessor.core
  :target-path "target/%s"
  :plugins [[lein-cljsbuild "1.1.3"] [lein-environ "1.0.2"]]
  :cljsbuild {:figwheel false
              :build-ids ["prod"]
              :builds
              [{:id "prod"
                :source-paths ["src/client"]
                :compiler {:optimizations :advanced
                           :output-to "resources/public/main.js"
                           :pretty-print false}}]}
  :profiles {:dev {:source-paths ["dev"]}
             :uberjar {:aot :all}}
  :env {:es-url "http://localhost:9200/"
        :eagle-file "~/.eagle"
        :s3bucket "lboeing_xml"
        :app-port 7800
        :nrepl-port 7801})
