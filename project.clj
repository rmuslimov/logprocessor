(defproject logprocessor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.1"]
                 [criterium "0.4.4"]
                 [midje "1.8.3"]
                 [com.github.kyleburton/clj-xpath "1.4.5"]]
  :main ^:skip-aot logprocessor.core
  :target-path "target/%s"
  :profiles {:dev {:source-paths ["dev"]}
             :uberjar {:aot :all}})
