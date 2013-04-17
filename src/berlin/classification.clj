(ns berlin.classification)

(def area-names ["neukoelln"
                 "neukolln"
                 "mitte"
                 "kreuzberg"
                 "prenzlauer"
                 "prenzlauerberg"
                 "friedrichshain"])

(defn normalize-text
  "Normalize accented characters"
  [t]
  (.replaceAll
   (java.text.Normalizer/normalize t java.text.Normalizer$Form/NFD)
   "[^\\p{ASCII}]" ""))

(defn find-prices
  [t]
  (map #(Integer. %)
       (concat (map second (re-seq #"EUR\s*(\d+)" t))
               (map second (re-seq #"(\d+)\s*EUR" t)))))

(defn find-dates
  [t]
  (map second (re-seq #"\d{2}\.\d{2}\.?\s*-\s*\d{2}\.\d{2}\.?" t)))

(defn find-sizes
  [t]
  (map #(Integer. %)
       (map second (re-seq #"(\d+)\s*ft" t))))

(defn find-rooms
  [t]
  (map #(Integer. %)
       (map second (re-seq #"(\d+)\s*br" t))))

(defn find-areas
  [t]
  (let [areas (map #(java.util.regex.Pattern/compile %) area-names)
        t (.toLowerCase t)]
    (filter identity (reduce #(conj %1 (re-find %2 t)) [] areas))))

(defn normalize-area
  [area]
  (cond
   (= area "neukolln") "neukoelln"
   (= area "prenzlauer") "prenzlauerberg"
   :else area))

(defn classify-listing
  [listing]
  (let [t (normalize-text (:title listing))
        c (normalize-text (:content listing))
        get-using (fn [f]
                    (distinct (concat (f t) (f c))))
        price (first (filter #(not (#{0 1} %)) (find-prices t)))
        price (if (nil? price)
                nil
                ;; day rates
                (if (< price 100)
                  (* price 30)
                  price))
        ]
    (assoc listing
      :price price
      :area (normalize-area
             (let [a (find-areas t)]
               (if-let [a (first a)]
                 a
                 (let [a (find-areas c)]
                   (if-let [a (first a)]
                     a
                     nil)))))
      :size (first (get-using find-sizes))
      :rooms (first (get-using find-rooms))
      :dates (get-using find-dates)
      :requires-edit  (nil? price)
      :comments (get listing :comments nil)
      :tags (get listing :tags [])      
      )))

  ;; aggregation

(defn get-options2
  [listings]
  (let [opts (apply merge-with #(sort (distinct (into %1 %2)))
                    (map #(list (select-keys % [:area :rooms :prices])) listings))
        [min-price max-price] (apply (juxt min max) (:price opts))]
    (assoc opts
      :min-price min-price
      :max-price max-price)))

(defn get-options
  ;; god there must be a better way
  [listings]
  (let [opts (reduce (fn [m l] (merge-with into
                                          m
                                          {:prices [(:price l)]
                                           :rooms [(:rooms l)]
                                           :areas [(:area l)]
                                           :tags (:tags l)}))
                    {}
                    listings)
        opts (apply hash-map (apply concat (map #(list (first %) (filter identity (sort (distinct (second %))))) opts)))
        [min-price max-price] (if (not (empty? (:prices opts)))
                                (apply (juxt min max) (:prices opts))
                                [0 10000000])
        opts (assoc opts
               :min-price min-price
               :max-price max-price
               )]
    (println (:prices opts))
    opts))

