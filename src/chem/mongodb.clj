(ns chem.mongodb
  (:require [somnium.congomongo :as m])
  (:import (java.io BufferedReader FileReader FileWriter))
  (:gen-class))

;; Set up namespaces
;;
;;     (use 'astserver.mongodb)
;;     (require '[somnium.congomongo :as m])
;;
;; Make a connection
;;
;;     user> (def conn
;;              (m/make-connection "chem"
;;                                 :host "127.0.0.1"
;;                                 :port 27017))
;; #'mongo-db-loader/conn
;;
;; Set the connection globally
;;
;;     user> (m/set-connection! conn)
;;     {:mongo #<MongoClient Mongo: /127.0.0.1:27017>,
;;             :db #<DBApiLayer ast>}
;;

(defn setup [hostname port databasename]
  (let [conn (m/make-connection databasename
                                :host hostname
                                :port port)]
    (m/set-connection! conn)))

(defn init []
  (setup "127.0.0.1" 27017 "chem"))

(defn load-db-file [filename tablekeyword]
  (dorun
   (map (fn [lines-partition]
          (m/mass-insert!
           tablekeyword
           (map (fn [record]
                  (let [fields (.split record "\\|")]
                    {:key (nth fields 0)
                     :value (nth fields 1)}))
                lines-partition)))
        (partition-all 100 (line-seq (BufferedReader. (FileReader. filename)))))))

;; something like: (lookup :normchem "benzene")
(defn lookup [dbkeyword term]
  (m/fetch dbkeyword :where {:key term}))