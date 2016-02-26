(ns logprocessor.es
  (:require [clojurewerkz.elastisch.rest :as esr]
            [com.stuartsierra.component :as component]))


(defrecord ES [url]
  component/Lifecycle
  (start [this]
    (assoc this :con (esr/connect url)))
  (stop [this]
    (dissoc this :con)))
