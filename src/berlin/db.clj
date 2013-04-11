(ns berlin.db
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :as mq]
            [monger.json]
            [monger.joda-time])
  (:import [org.bson.types ObjectId]))

(def mongo-host "localhost")
(def mongo-port 27017)
(def mongo-db "berlin")

(defn connect!
  []
  (mg/connect! {:host mongo-host :port mongo-port})
  (mg/set-db! (mg/get-db mongo-db)))

(defn insert
  [c doc]
  (let [with-id (if (:_id doc)
                  doc
                  (assoc doc :_id (ObjectId.)))]
    (mc/insert c with-id)
    with-id))

(defn list
  [c]
  (mc/find-maps c))

(defn exists?
  [c k v]
  (boolean (mc/find-one c {k v})))

(defn drop
  [c]
  (mc/drop c))

(defn get
  [c k v]
  (mc/find-one-as-map c {k v}))

(defn update
  [c doc]
  (mc/update-by-id c (:_id doc) doc))
