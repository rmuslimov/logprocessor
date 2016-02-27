(defproject logprocessor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [midje "1.8.3"]
                 [com.github.kyleburton/clj-xpath "1.4.5"]
                 [com.stuartsierra/component "0.3.1"]
                 [clojurewerkz/elastisch "2.2.1"]
                 [http-kit "2.1.9"]
                 [amazonica "0.3.9"]
                 [clj-yaml "0.4.0"]
                 [fs "1.3.3"]
                 [clj-time "0.9.0"]]
  :main ^:skip-aot logprocessor.core
  :target-path "target/%s"
  :profiles {:dev {:source-paths ["dev"]}
             :uberjar {:aot :all}})
