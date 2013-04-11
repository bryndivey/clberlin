(ns berlin.crawling
  (:require [net.cgrand.enlive-html :as enlive]
            [clojure.tools.logging :as log]
            [berlin.models.listing :as listing])
  (:use [clojure.string :only [split]]
        [clj-time.format :only [formatter parse]])
  (:import (java.net URL MalformedURLException)))

(defn- links-from-main
  [base-url html]
  (remove nil? (for [link (enlive/select html [:p.row :a])]
                 (when-let [href (-> link :attrs :href)]
                   (try
                     (URL. base-url href)
                     (catch MalformedURLException e))))))


(def url-queue (java.util.concurrent.LinkedBlockingQueue.))
(def img-queue (java.util.concurrent.LinkedBlockingQueue.))
(def root "http://berlin.en.craigslist.de/sub/")

(declare run get-url get-img process-main handle-main-results process-listing)

(defn- make-agents
  [n-urls n-imgs]
  (set (concat (repeatedly n-urls #(agent {::t #'get-url :queue url-queue}))
               (repeatedly n-imgs #(agent {::t #'get-img :queue img-queue})))))

(def agents (make-agents 25 25))

(defn- as-url
  [url]
  (if (= java.net.URL (class url))
    url
    (java.net.URL. url)))

(defn- -get-id [url]
  (-> (split url #"/")
      last
      (split #"\.")
      first))

(defn- -get-content [html selector]
  (apply str (filter string? (enlive/unwrap (first (enlive/select html selector))))))


;; actions

(defn ^::blocking get-url
  [{:keys [^java.util.concurrent.BlockingQueue queue] :as state}]
  (let [item (.take queue)
        url (as-url (:url item))
        next (:next item)]
    (log/info "Getting URL" url)
    (try (if (listing/exists? (-get-id (str url)))
           (do
             (log/info "Already crawled" url)
             state)
           {:url url
            :content (slurp url :encoding "iso-8859-1")
            ::t next})
         (catch Exception e
           (do
             (log/error "Error crawling " url (str e))
             state))
         (finally
          (run *agent*)))))

(defn ^::blocking get-img
  [{:keys [^java.util.concurrent.BlockingQueue queue] :as state}]
  (let [item (.take queue)
        url (as-url (:url item))
        listing-id (:listing-id item)
        image-id (-get-id (str url))]
    (log/info "Getting image" url)
    (try (if (listing/has-image? listing-id image-id)
           (do
             (log/info "Already got image" url)
             state)
           (do
             ;; this actually gets the image
             (listing/add-image listing-id image-id (str url))
             {::t #'get-img :queue img-queue}))
         (catch Exception e
           (do
             (log/error "Error fetching image" url (str e))
             state))
         (finally
          (run *agent*)))))

(defn process-main
  [{:keys [url content]}]
  (try
    (let [html (enlive/html-resource (java.io.StringReader. content))]
      {::t #'handle-main-results
       :url url
       :links (links-from-main url html)})
    (finally (run *agent*))))

(defn ^::blocking handle-main-results
  [{:keys [url links]}]
  (try
    (do 
      (doseq [url links]
        (.put url-queue {:url url :next #'process-listing}))
      {::t #'get-url :queue url-queue})
    (finally (run *agent*))))


(defn- -get-images
  [html]
  (map #(-> % :attrs :href) (enlive/select html [:div#thumbs :a])))

(def cl-format (formatter "yyyy-MM-dd, hh:mmaa"))
(defn- parse-cl-date [dt]
  (parse cl-format (clojure.string/join " " (butlast (split dt #"\s+")))))

(defn ^::blocking process-listing
  [{:keys [url content]}]
  (try
    (let [html (enlive/html-resource (java.io.StringReader. content))
          id (-get-id (str url))
          images (-get-images html)]
      (doseq [url images]
        (.put img-queue {:url url :listing-id id}))
      (listing/add-new {:id id
                       :url (str url)
                       :title (-get-content html [:h2.postingtitle])
                       :content (-get-content html [:section#postingbody])
                       :posted (parse-cl-date (-get-content html [:p.postinginfo :date]))
                       :reply (-get-content html [:.dateReplyBar :a])})
      {::t #'get-url :queue url-queue})
    (catch Exception e
      (log/error "Error processing listing" url (str e)))
    (finally (run *agent*))))

















(defn paused? [agent] (::paused (meta agent)))

(defn run
  ([] (doseq [a agents] (run a)))
  ([a]
     (when (agents a)
       (send a (fn [{transition ::t :as state}]                 
                 (when-not (paused? *agent*)
                   (let [dispatch-fn (if (-> transition meta ::blocking)
                                       send-off
                                       send)]
                     (comment log/info "Running transition" transition)
                     (dispatch-fn *agent* transition)))
                 state)))))

(defn pause
  ([] (doseq [a agents] (pause a)))
  ([a] (alter-meta! a assoc ::paused true)))

(defn restart
  ([] (doseq [a agents] (restart a)))
  ([a]
     (alter-meta! a dissoc ::paused)
     (run a)))

(defn add-url
  [url]
  (.add url-queue {:url url :next #'process-main}))

(defn test-crawler
  [agent-count time starting-urls]
  (def agents (make-agents agent-count agent-count))
  (.clear url-queue)
  (doseq [url starting-urls]
    (add-url url))
  (println "Running")
  (run)
  (comment Thread/sleep time)
  (comment pause)
  [(count url-queue)])

(defn go []
  (test-crawler 30 120000 [root (str root "index100.html") (str root "index200.html")]))

