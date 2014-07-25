(ns chem.mongodb
  (:require [monger.core :as mg])
  (:require [monger.collection :as mc])
  (:import (java.io BufferedReader FileReader FileWriter))
  (:gen-class))

;; Set up namespaces
;;
;;     (use 'astserver.mongodb)
;;     (require '[somnium.congomongo :as m])
;;
;; Make a connection and set connection globally
;;
;;     user> (mg/connect!)
;;
;; Set the database globally
;;
;;     user> (mg/set-db! (mg/get-db "chem"))
;;
;; To disconnect, use monger.core/disconnect!

(defn setup [hostname port databasename]
  "Initialize connection to mongo db database on specified hostname
   and port, defaults to using chem database."
  (mg/connect! { :host hostname :port port })
  (mg/set-db! (mg/get-db databasename)))

(defn init
  "Initialize connection to mongo db database, defaults to using chem database."
  ([] (setup "127.0.0.1" 27017 "chem"))
  ([dbname] (setup "127.0.0.1" 27017 dbname)))

(defn load-key-value-db-file [filename tablename]
  (dorun
   (map (fn [lines-partition]
          (mc/insert-batch
           tablename
           (vec 
            (map (fn [record]
                   (let [fields (.split record "\\|")]
                     {:key (nth fields 0)
                      :value (nth fields 1)}))
                 lines-partition))))
        (partition-all 100 (line-seq (BufferedReader. (FileReader. filename)))))))

(defn load-table-file [filename tablename recfn]
  "Load database using file and supplied record function.
   Example:
      (load-file \"training.txt\" \"training-abstracts\" abstractfn)"
  (dorun
   (map (fn [lines-partition]
          (mc/insert-batch 
           tablename
           (vec
            (map recfn 
                 lines-partition))))
        (partition-all 100 (line-seq (BufferedReader. (FileReader. filename)))))))

(defn load-db-record-seq 
  "Load database using record sequence and supplied record function.
   Example:
      (load-db-record-seq training-record-seq \"training.abstracts\" abstractfn)"
   ([record-seq tablename recfn]
      (dorun
       (map (fn [lines-partition]
              (mc/insert-batch 
               tablename
               (vec 
                (map recfn 
                     lines-partition))))
            (partition-all 100 record-seq))))
   ([record-seq tablename]
      (dorun
       (map (fn [lines-partition]
              (mc/insert-batch
               tablename
               lines-partition))
            (partition-all 100 record-seq)))))
   
;; something like: (lookup "normchem" "benzene")
(defn lookup [dbname term]
  (seq
   (mc/find dbname {:key term})))
