(ns app.client
  (:require [datomic.client.api :as d]))

(def db-name "todos")

(def cfg {:server-type        :peer-server
          :access-key         "testtodos"
          :secret             "testtodos"
          :endpoint           "localhost:8998"
          :validate-hostnames false})

(defn get-client
  "Creates a datomic peer server client based on default configuration."
  []
  (try
    (d/client cfg)
    (catch Exception e (println (ex-data e)))))

(defn get-conn
  "Return a new connection to datomic db."
  []
  (d/connect (get-client) {:db-name db-name}))

(defn db
  "Returns the most recent database from the connection."
  []
  (d/db (get-conn)))
