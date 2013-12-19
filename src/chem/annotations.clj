(ns chem.annotations
  (:require [clojure.string :as string]))

(defn annotate-text [text spans begintag endtag eoltag]
  "Given document and spans annotate document for display.
   variable 'spans' is of the form [[:begin0 end0] [begin1 end1] ...]"
  (let [startset (set (map #(:start %) spans))
        endset (set (map #(:end %) spans))]
    (loop [i 0 
           text text
           result (vec "")] 
      (if (< i (count text))
        (let [ch (.charAt text i)
              rch (cond 
                 (contains? startset i) (str begintag ch)
                 (contains? endset i) (str endtag ch)
                 (= ch \newline) (str ch eoltag)
                 :else ch)]
          (recur (inc i) text (conj result rch)))
        (string/join result)))))
