(ns chem.mongodb
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [clojure.string :as string])
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
;; Terse Authenticating:
;;
;;    (mg/authenticate (mg/get-db "chem") "chemuser" (.toCharArray "password"))
;;
;; Or use:
;;
;;    (let [conn (mg/connect)
;;        db   (mg/get-db "chem")
;;        u    "chemuser"
;;        p    (.toCharArray "UserChem!@#$%")]
;;    (mg/authenticate db u p))
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

(defn remote-init
  "Initialize connection to mongo db database, defaults to using chem database.

   This needs some unexpected error handling code..."
  ([] (remote-init "chem"))
  ([dbname] 
     (let [netrc-record
           (first 
            (filter #(and (> (count (nth % 1)) 10) (= (subs (nth % 1) 0 10) "mongodb://"))
                    (map #(string/split % #" ")
                         (string/split (slurp (str (System/getProperty "user.home") "/.netrc")) #"\n"))))
           dbhost (subs (nth netrc-record 1) 10)
           dbuser (nth netrc-record 3)
           dbpass (nth netrc-record 5)]
       (setup dbhost 27017 dbname)
       (mg/authenticate (mg/get-db dbname) dbuser (.toCharArray dbpass)) )))

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
(defn lookup
  ([collname term]
     (seq
      (mc/find collname {:key term})))
  ([collname field-keyword target-string]
     (seq
      (mc/find collname {field-keyword target-string}))))

(defn list-documents
  [dbname]
  (mc/find-maps dbname))

(defn list-docids 
  [dbname]
  (map #(:docid %)
       (mc/find-maps dbname)))

(defn get-document [docid]
  (mc/find-one-as-map "training.abstracts" {:docid docid}))
