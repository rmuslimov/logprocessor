(ns user
  (:require [reloaded.repl :refer [system init start stop go reset]]
            [figwheel-sidecar.repl-api :as ra]
            [logprocessor.core :refer [main-system]]))

(reloaded.repl/set-init! main-system)

;; clj configuration management
;; Tip for Emacsers!
;; (setq cider-refresh-before-fn "user/stop-system"
;;       cider-refresh-after-fn "user/start-system")

(defn start-system []
  (reloaded.repl/resume))

(defn stop-system []
  (reloaded.repl/suspend))

;; (init) (go) (stop)

;; cljs configuration management
;; Tip for Emacsers!
;; (setq
;;  cider-cljs-lein-repl
;;  "(do (require 'user) (user/start-cljs) (user/repl-cljs))")

(defn start-cljs []
  (ra/start-figwheel!
    {:figwheel-options {} ;; <-- figwheel server config goes here
     :build-ids ["dev"]   ;; <-- a vector of build ids to start autobuilding
     :all-builds          ;; <-- supply your build configs here
     [{:id "dev"
       :figwheel true
       :source-paths ["src"]
       :compiler {:main "client.web"
                  :asset-path "out"
                  :output-to "dev-resources/public/main.js"
                  :output-dir "dev-resources/public/out"
                  :verbose true}}]}))

(defn stop-cljs []
  (ra/stop-figwheel!))

(defn repl-cljs []
  (ra/cljs-repl))
