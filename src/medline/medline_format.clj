(ns medline.medline-format
  (:require [clojure.string]))

;; 123456
;; headercontent
;; headercontent
;;       content
;;       content
;;       content
;; headercontent
;;       content
;;
;; tokenization:
;;  
;;
;;
;; grammer:
;; document -> header content
;;
(defn reader
  "A Medline Format (non-XML) document parser.
   Really? This is a parser?"
  [doc-str]
  (let [doclines (clojure.string/split doc-str #"\n")]
    (loop [linelist (rest doclines)
           line (first doclines)
           current-id ""
           pmid ""
           titlelinelist []
           abstractlinelist []]
      (let [id      (clojure.string/trim (subs line 0 4))
            content (subs line 6)]
        (cond
          (empty? linelist) (hash-map :pmid pmid 
                                      :title (clojure.string/join "\n" titlelinelist)
                                      :abstract (clojure.string/join "\n" abstractlinelist))
          (= id "PMID") (recur (rest linelist) (first linelist) id 
                               content titlelinelist abstractlinelist)
          (= id "TI")   (recur (rest linelist) (first linelist) "TI" 
                               pmid (conj titlelinelist content) abstractlinelist)
          (= id "AB")   (recur (rest linelist) (first linelist) "AB" 
                               pmid titlelinelist (conj abstractlinelist content))
          (and (= id "") (= current-id "TI"))
          (recur (rest linelist) (first linelist) "TI" 
                 pmid (conj titlelinelist content) abstractlinelist)
          (and (= id "") (= current-id "AB"))
          (recur (rest linelist) (first linelist) "AB" 
                 pmid titlelinelist (conj abstractlinelist content))
          :else (recur (rest linelist) (first linelist) id 
                       pmid titlelinelist abstractlinelist))))))
    


(defn reader2
  "An alternate implementation of the Medline Format (non-XML) reader"
  [doc-str]
  (let [docrecords (mapv (fn [docline]
                         {:header  (clojure.string/trim (subs docline 0 4))
                          :content (subs docline 6)})
                       (clojure.string/split doc-str #"\n"))]
    
    (loop [recordlist (rest docrecords)
           record (first docrecords)
           current-id ""
           newdoc {:pmid ""
                   :title ""
                   :abstract ""}]
      (let [id (:header record)
            content (:content record)]
        (cond
          (empty? recordlist) newdoc
          (= id "PMID") (recur (rest recordlist) (first recordlist) id
                               (assoc newdoc :pmid content))
          (= id "TI")   (recur (rest recordlist) (first recordlist) "TI"
                               (assoc newdoc :title content))
          (= id "AB")   (recur (rest recordlist) (first recordlist) "AB"
                               (assoc newdoc :abstract content))
          (and (= id "") (= current-id "TI"))
          (recur (rest recordlist) (first recordlist) "TI"
                 (assoc newdoc :title (str (:title newdoc) "\n" content)))
          (and (= id "") (= current-id "AB"))
          (recur (rest recordlist) (first recordlist) "AB" 
                 (assoc newdoc :abstract (str (:abstract newdoc) "\n" content)))
          :else (recur (rest recordlist) (first recordlist) id
                       newdoc))))))
