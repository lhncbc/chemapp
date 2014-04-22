(ns chem.mongodb-congomongo
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
  "Initialize connection to mongo db database on specified hostname and port, defaults to using chem database."
  (let [conn (m/make-connection databasename
                                :host hostname
                                :port port)]
    (m/set-connection! conn)))

(defn init
  "Initialize connection to mongo db database, defaults to using chem database."
  ([] (setup "127.0.0.1" 27017 "chem"))
  ([dbname] (setup "127.0.0.1" 27017 dbname)))

(defn load-key-value-db-file [filename tablekeyword]
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

(defn load-table-file [filename tablekeyword recfn]
  "Load database using file and supplied record function.
   Example:
      (load-file \"training.txt\" :training-abstracts abstractfn)"
  (dorun
   (map (fn [lines-partition]
          (m/mass-insert! 
           tablekeyword
           (map recfn 
                lines-partition)))
        (partition-all 100 (line-seq (BufferedReader. (FileReader. filename)))))))

(defn load-db-record-seq 
  "Load database using record sequence and supplied record function.
   Example:
      (load-db-record-seq training-record-seq :training.abstracts abstractfn)"
   ([record-seq tablekeyword recfn]
      (dorun
       (map (fn [lines-partition]
              (m/mass-insert! 
               tablekeyword
               (map recfn 
                    lines-partition)))
            (partition-all 100 record-seq))))
   ([record-seq tablekeyword]
      (dorun
       (map (fn [lines-partition]
              (m/mass-insert! 
               tablekeyword
               lines-partition))
            (partition-all 100 record-seq)))))
   
;; something like: (lookup :normchem "benzene")
(defn lookup [dbkeyword term]
  (m/fetch dbkeyword :where {:key term}))
