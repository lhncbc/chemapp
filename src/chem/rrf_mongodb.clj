(ns chem.rrf-mongodb
  (:require [somnium.congomongo :as m])
  (:import (java.io BufferedReader FileReader FileWriter))
  (:gen-class))

;; Set up namespaces
;;
;;     (require '[chem.mrrel-rrf-mongodb :as mrreldb])
;;     (require '[somnium.congomongo :as m])
;;
;; Make a connection
;;
;;     user> (def conn
;;              (m/make-connection "umls2013aa"
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
;;     user> (def mrrel-seq (line-seq (BufferedReader. (FileReader. mrrel-filename))))
;;     user> (chemdb/load-db-record-seq mrrel-seq :mrrel chem.mrrel-rrf-mongodb/mrrel-recfun)
;;     user> (m/add-index! :mrrel [:cui1])
;;     user> (m/add-index! :mrrel [:aui1])
;;     user> (m/add-index! :mrrel [:cui2])
;;     user> (m/add-index! :mrrel [:aui2])
;;     user> (m/add-index! :mrrel [:sab])
;;

(defn setup [hostname port databasename]
  (let [conn (m/make-connection databasename
                                :host hostname
                                :port port)]
    (m/set-connection! conn)))

(defn init 
  ([] (setup "127.0.0.1" 27017 "umls2013ab"))
  ([dbname] (setup "127.0.0.1" 27017 dbname)))

(defn mrrel-recfun [line]
  (let [fields (.split line "\\|" -1)]
    (when (> (count fields) 14)
      {:cui1     (nth fields 0)
       :aui1     (nth fields 1)
       :stype1   (nth fields 2)
       :rel      (nth fields 3)
       :cui2     (nth fields 4)
       :aui2     (nth fields 5)
       :stype2   (nth fields 6)
       :rela     (nth fields 7)
       :rui      (nth fields 8)
       :srui     (nth fields 9)
       :sab      (nth fields 10)
       :sl       (nth fields 11)
       :rg       (nth fields 12)
       :dir      (nth fields 13)
       :suppress (nth fields 14)}
      )))

(defn mrsty-recfun [line]
  (let [fields (.split line "\\|" -1)]
    (when (> (count fields) 4)
      {:cui  (nth fields 0)
       :tui  (nth fields 1)
       :stn  (nth fields 2)
       :sty  (nth fields 3)
       :atui (nth fields 4)})))
       

(defn load-table-file [filename tablekeyword recfunc]
  "Load database using file and supplied record function.
   Example:
      (load-file \"training.txt\" :training-abstracts abstractfn)"
  (dorun
   (map (fn [lines-partition]
          (m/mass-insert! 
           tablekeyword
           (filter #(not (nil? %))      ; remove any nil records
                   (map recfunc 
                        lines-partition))))
        (partition-all 150000 (line-seq (BufferedReader. (FileReader. filename)))))))

(defn load-table-seq [tableseq tablekeyword recfunc]
  "Load database using file and supplied record function.
   Example:
      (load-file \"training.txt\" :training-abstracts abstractfn)"
  (dorun
   (map (fn [lines-partition]
          (m/mass-insert! 
           tablekeyword
           (filter #(not (nil? %))      ; remove any nil records
                   (map recfunc 
                        lines-partition))))
        (partition-all 10000 tableseq))))
