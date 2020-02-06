(ns agent.lambda
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [uswitch.lambada.core :refer [deflambdafn]]
            [agent.data :as d]))

(deflambdafn com.busqandote.agent.RegisterAgent
  [in out ctx]
  (let [in (json/read (io/reader in) :key-fn keyword)
        response (d/register-agent in)]
    (with-open [w (io/writer out)]
      (json/write response w))))

