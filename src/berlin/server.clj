(ns berlin.server
  (:require [noir.server :as server]
            [berlin.db :as db]
            [berlin.crawling :as crawling]
            [berlin.views.listings :as listings])
  (:import [it.sauronsoftware.cron4j Scheduler]))

(server/load-views-ns 'berlin.views)

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8088"))]

    ;; set up database connections
    (db/connect!)

    ;; start the crawler
    (crawling/run)

    ;; schedule scraping every 15 minutes
    (crawling/add-url crawling/root)
    (doto (Scheduler.)
      (.schedule "*/15 * * * *" (fn []
                                  (crawling/add-url crawling/root))))
    
    ;; exchange rate update every hour
    (listings/update-exchange-rate)
    (doto (Scheduler.)
      (.schedule "*/15 * * * *" listings/update-exchange-rate))
    
    (server/start port {:mode mode
                        :ns 'berlin})))

