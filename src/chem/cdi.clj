(ns chem.cdi
  (:require [clojure.java.io :as io]))

(defn write-cdi-result
  "Write result to file in CHEMDNER Document Indexing (CDI) format."
  [filename result]
  (let [docid (:docid result)
        terms (:terms result)]
    (with-open [wtr (io/writer filename)]
      (dorun (map-indexed (fn [i term]
                            (.write wtr (format "%s\t%s\t%s\t0.5\n" docid term (inc i))))
                  terms)))))

(defn pr-str-cdi-result
    "Write result to string in CHEMDNER Document Indexing (CDI) format."
  ([{docid :docid terms :terms}]
   (pr-str-cdi-result docid terms))
  ([docid terms]
   (str 
    (map-indexed (fn [i term]
            (format "%s\t%s\t%s\t0.5\n" docid term (inc i)))
         terms))))
  









        

