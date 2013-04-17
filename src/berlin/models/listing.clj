(ns berlin.models.listing
  (:require [berlin.db :as db]
            [monger.collection :as mc]
            [monger.gridfs :as gfs]
            [monger.query :as mq]
            [berlin.classification :as classification])
  (:use [monger.operators :only [$addToSet $gte $lte]]))

(def collection "listings")

(defn insert
  [l]
  (db/insert collection l))

(defn add-new
  [{:keys [id url title content posted reply] :as listing}]
  (insert (classification/classify-listing (merge 
                                            listing 
                                            {:seen false
                                             :hidden false
                                             :tags []
                                             :comments nil}))))

(defn list
  []
  (mq/with-collection collection
    (mq/find {})
    (mq/sort (sorted-map :posted -1))))

(defn exists?
  [id]
  (db/exists? collection :id id))

(defn drop
  []
  (db/drop collection)
  (gfs/remove-all))

(defn update
  [doc]
  (db/update collection doc))

(defn get
  [id]
  (db/get collection :id id))

(defn has-image?
  [id image-id]
  (some #{image-id} (:images (get id))))

(defn add-image
  [id image-id image-url]
  (gfs/store-file (gfs/make-input-file image-url)
                  (gfs/filename image-id)
                  (gfs/metadata {:format "jpg"
                                 :listing id})
                  (gfs/content-type "image/jpg"))
  (mc/update collection {:id id} {$addToSet {:images image-id}}))

(defn get-image
  [id]
  (gfs/find-one {:filename id}))

(defn add-comment
  [id comment]
  (when-let [l (get id)]
    (update (assoc l :comments (str (:comments l) "\n" comment)))))

(defn add-tag
  [id tag]
  (when-let [l (get id)]
    (update (assoc l :tags (set (conj (:tags l) tag))))))

(defn remove-tag
  [id tag]
  (when-let [l (get id)]
    (update (assoc l :tags (remove #{tag} (:tags l))))))

(defn reclassify
  []
  (for [listing (list)]
    (try
      (update (classification/classify-listing listing))
      (catch Exception e
        (do
          (println "Exception with id " (:id listing) e)) ))))

