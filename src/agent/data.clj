(ns agent.data
  (:require [datomic.api :as d]
            [environ.core :refer [env]]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(s/def ::name string?)
(s/def ::address string?)
(def phone-number-regex #"^[0-9]{10}$")

(def ten-digits (gen/fmap #(str %)
                          (gen/large-integer* {:min 1000000000 :max 9999999999})))

(s/def ::phone-number (s/with-gen (s/and string? #(re-matches phone-number-regex %))
                        (constantly ten-digits)))

(s/def ::agent (s/keys :req [::name ::address ::phone-number]))

(defn init-conn  []
  (def db-base-uri (env :db-base-uri))
  (def db-agents (env :db-agents))
  (def db-source-url (env :db-source-url))
  (def db-uri (str db-base-uri db-agents db-source-url))


  (try
    (d/connect db-uri)
    (catch java.lang.RuntimeException re
      (if (= (.getMessage re) "Could not find agents in catalog")
        (d/create-database db-uri)))
    (finally
      (d/connect db-uri))))

(defn get-user-schema [conn]
  (d/q '[:find ?attr
         :where
         [_ :db.install/attribute ?a]
         [?a :db/ident ?attr]]
       (d/db conn)))

;;(get-user-schema (init-conn))


(defn init-db [conn]
  (let [schema (get-user-schema conn)]
    (if (empty? schema)
      (@(d/transact conn (read-string (slurp "resources/agents.edn")))))))

;;(empty? (get-user-schema (init-conn)))

;;@(d/transact (init-conn) (read-string (slurp "resources/agents.edn")))


(defn register-agent [agent]
  {:pre [(s/valid? ::agent agent)]}
  @(d/transact (init-conn) [agent]))

;;(register-agent {::name "Bugs"
;;                 ::address "Bunny"
;;                 ::phone-number "1234567890"})
