(ns entityrec.find-longest-match
  (:require [clojure.string :refer [join lower-case]]
            [entityrec.string-utils :refer [list-head-proper-sublists
                                            list-tail-proper-sublists
                                            list-all-proper-sublists]]))
(defn tokenlist->str
  "convert tokenlist to string"
  [tokenlist]
  (join " " (mapv #(lower-case (:text %)) tokenlist)))

(defn detokenize
  [tokenlist]
  (join "" (mapv #(:text %) tokenlist)))

(defn find-longest-match
  "Given Example:

    \"Papillary Thyroid Carcinoma is a Unique Clinical Entity.\"
 
Check the following token sublists:

    \"Papillary Thyroid Carcinoma is a Unique Clinical Entity\"
    \"Papillary Thyroid Carcinoma is a Unique Clinical\"
    \"Papillary Thyroid Carcinoma is a Unique\"
    \"Papillary Thyroid Carcinoma is a\"
    \"Papillary Thyroid Carcinoma is\"
    \"Papillary Thyroid Carcinoma\"
    \"Papillary Thyroid\"
    \"Papillary\"
              \"Thyroid Carcinoma is a Unique Clinical Entity\"
              \"Thyroid Carcinoma is a Unique Clinical\"
              \"Thyroid Carcinoma is a Unique\"
              \"Thyroid Carcinoma is a\"
              \"Thyroid Carcinoma is\"
              \"Thyroid Carcinoma\"
              \"Thyroid\"
    ...
 "
  ([subtokenlist dictionary-func]
   (find-longest-match subtokenlist dictionary-func detokenize))
  ([subtokenlist dictionary-func detokenize-func]
   (let [list-of-lists (list-all-proper-sublists subtokenlist)]
     (loop [subsubtokenlist (first list-of-lists)
            rest-tokenlists (rest list-of-lists)
            entitylist []]
       (if (empty? subsubtokenlist)
         entitylist
         (let [term (detokenize-func subsubtokenlist)
               result (vector term (dictionary-func term subsubtokenlist))]
           (if (second result)
             (recur (first rest-tokenlists) ; subsubtokenlist
                    (rest rest-tokenlists)  ; rest-tokenlists
                    (conj entitylist result)) ;entitylist
             (recur (first rest-tokenlists) ; subsubtokenlist
                    (rest rest-tokenlists)  ; rest-tokenlists
                    entitylist)))))))) ;entitylist


;; example of use
(comment
  (def dictionary
    {"papillary thyroid carcinoma" :C0238463
     "unique"   :C1710548
     "clinical" :C0205210
     "entity"   :C1551338})

  (defn lookup
    [term tokenlist]
    (dictionary (lower-case term)))

  (entityrec.find-longest-match/find-longest-match
   (skr.tokenization/analyze-text "Papillary Thyroid Carcinoma is a Unique Clinical Entity.")
   lookup)
  )
