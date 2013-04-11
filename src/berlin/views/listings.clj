(ns berlin.views.listings
  (:require [berlin.views.common :as common]
            [berlin.models.listing :as listing]
            [berlin.classification :as classification]
            [monger.gridfs :as gfs]
            [noir.request :as request]
            [noir.response :as response])
  (:use [noir.core :only [defpage defpartial]]
        [noir.response :only [content-type json redirect]]
        [hiccup.form :only [form-to]]
        [clojure.data.json :only [read-json]]))


(def ^:dynamic exchange-rate (atom nil))

(defn get-exchange-rate
  []
  (:rate (read-json (slurp "http://rate-exchange.appspot.com/currency?from=EUR&to=ZAR"))))

(defn update-exchange-rate
  []
  (swap! exchange-rate (fn [old] (get-exchange-rate))))

(defpage "/raw" []
  (common/page-layout
   (for [l (listing/list)]
     (do
       (println l)
       [:dl.rawlisting (apply concat
                              (for [k (keys l)]
                                [[:dt k]
                                 [:dd (str (k l))]]))]))))

(defpartial image-view
  [id]
  [:img {:src (str "/image/" id)}])


(defpartial listing-sidebar
  [options selected]
  (let [min-price (str (get selected :min-price (apply min (:prices options))))
        max-price (str (get selected :max-price (apply max (:prices options))))
        areas (:areas selected)
        text (:text selected)
        sel? (fn [a b] (if (= (str a) b) "selected"))
        msel? (fn [a b] (if (some #{a} b) "selected"))]
    (println "s111" selected max-price)
    (println  (apply min (:prices options)))
    [:div
     [:div.well.sidebar-nav
      [:ul.nav.nav-list
       [:li.nav-header "Listings"]
       [:li.active [:a "Active link"]]]]

     [:form {:method "GET"}
      [:fieldset
       [:legend "Filters"]
       [:label "Text"]
       [:input {:name "text" :value text}]
       
       [:label "Areas"]
       [:select {:multiple "multiple" :name "areas[]"}
        (for [area (:areas options)]
          [:option {:value area :selected (msel? area areas)} area])]
       [:br]
       
       [:label "Minimum"]
       [:select {:name "min-price"}
        (for [price (:prices options)]
          [:option {:value price :selected (sel? price min-price)} price])]
       [:br]
       
       [:label "Maximum"]
       [:select {:name "max-price"}
        (for [price (:prices options)]
          [:option {:value price :selected (sel? price max-price)} price])]
       [:br]
       [:div.form-actions
        [:button {:type "submit" :class "btn btn-primary"} "Filter"]]]
      ]]))

(defpartial listing-view
  [listing min-price max-price areas]
  [:div.span9.listing
   
   ;; labels
   
   ;; header
   [:div 
    [:h4
     (if (not(:seen listing))
       [:span.label.label-warning "New!"])
     " "
     (if (:images listing)       
       [:span.label.label-info "Pics"])
     [:a {:href (:url listing)} " " (:title listing)]]
    ]

   ;; row     
   [:div.row-fluid
    [:div.span3.attributes
     
     ;; attributes
     (if (:price listing)
       (do
         (if (nil? @exchange-rate)
           (update-exchange-rate))
         [:span
          [:b "Price"]
          ": €" (str (:price listing))
          (format " R%.2f" (* @exchange-rate (:price listing)))
          ]))

     (if (:rooms listing)
       [:span
        [:b "Rooms"]
        ": " (str (:rooms listing))])

     (if (:area listing)
       [:span
        [:b "Area"]
        ": " (clojure.string/capitalize (:area listing))])

     (if (:size listing)
       [:span
        [:b "Size"]
        ": " (str (:size listing))])

     [:form.form-inline {:method "POST" :action (str "/listings/" (:id listing) "/action")}
      [:input {:type "hidden" :name "min-price" :value min-price }]
      [:input {:type "hidden" :name "max-price" :value max-price }]
      (for [area areas]
        [:input {:type "hidden" :name "areas[]" :value area }])
      [:button {:type "submit" :class "btn" :name "action" :value "seen" :data-id (:id listing)} "Seen"]
      [:button {:type "submit" :class "btn" :name "action" :value "hidden" :data-id (:id listing)} "Hide"]]       
     ]

    ;; images
    [:div.span9
     (for [image (take 4 (:images listing))]
       [:div.span3 (image-view image)])]]

   ])

(defpage [:post "/listings/:id/action"] {id :id :keys [action inline min-price max-price areas]}
  (if (not (some #{action} ["hidden" "seen"]))
    ;; error
    (response/status 400 (str "Action " action " is invalid"))

    ;; continue
    (do 
      (when-let [l (listing/get id)]
        (listing/update (assoc l (keyword action) true)))
      (println "DOING AN ACTION" id action inline)
      (if (not inline)
        ;; no js response (sucks)
        (let [astr (apply str (map #(str "&areas[]=" %) areas))]
          (response/redirect (str ((:headers (request/ring-request)) "origin") "?min-price=" min-price "&max-price=" max-price astr)))
        ;; ajax response
        (response/json {})))))


(defn filter-listings
  [listings min-price max-price areas text]
  (filter #(do
             (if (and
                  (not (:hidden %))
                  (or (nil? (:price %))
                      (and (or (nil? min-price)
                               (>= (:price %) min-price))
                           (or (nil? max-price)
                               (<= (:price %) max-price))))
                  (or
                   (nil? areas)
                   (some (set [(:area %)]) areas))
                  (or
                   (nil? text)
                   (re-find (java.util.regex.Pattern/compile (str "(?i)" text)) (:content %))))
               true
               false))
          listings)
  
  )

(defn parse-int [i]
  (if (nil? i)
    nil
    (Integer/parseInt i)))

(defpage "/" {:keys [min-price max-price areas text] :as selected}
  (println "SEL" selected)
  (let [listings (map classification/classify-listing (listing/list))
        options (classification/get-options listings)
        min-price (parse-int min-price)
        max-price (parse-int max-price)
        filtered-listings (filter-listings listings min-price max-price areas text)]
    (println (count listings))
    (common/page-layout
     (common/navbar)
     (common/fluid-layout
      (listing-sidebar options selected)
      [:div.row-fluid
       [:div.span9 "1 - " (str (count filtered-listings))]
       (for [l (partition-all 1 filtered-listings)]
         (do
           [:div.row-fluid
            (map #(listing-view % min-price max-price areas) l)]))]))))

(defpage "/image/:id" {id :id}
  (if-let [file (listing/get-image id)]
    (response/content-type (.getContentType file)
                           (.getInputStream file))))
