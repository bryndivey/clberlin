(defproject berlin "0.1.0-SNAPSHOT"
            :description "FIXME: write this!"
            :dependencies [[org.clojure/clojure "1.4.0"]
                           [noir "1.3.0-beta3"]
                           [enlive "1.1.0"]
                           [clj-http "0.7.0"]
                           [org.clojure/java.jdbc "0.2.3"]
                           [org.xerial/sqlite-jdbc "3.7.2"]
                           [org.clojars.gmazelier/cron4j "2.2.5"]
                           [org.clojure/tools.logging "0.2.3"]
                           [org.clojure/data.json "0.1.2"]
                           [clj-time "0.4.5"]
                           [com.novemberain/monger "1.4.2"]
                           [cheshire "5.0.1"]]
            :main berlin.server)

