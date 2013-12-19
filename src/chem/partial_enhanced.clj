(ns chem.partial-enhanced
  (:require [chem.partial :as partial])
  (:require [chem.metamap-api :as metamap-api]))


(defn enhance-annotations [mmapi annotationlist]
  "Query MetaMap using term processing to determine concepts for
   annotation texts for document."
  (map (fn [annotation]
         (let [mm-annotationlist 
               (chem.metamap-api/handle-result-list
                (chem.metamap-api/process-string mmapi (:text annotation) "-z"))]
           (if (empty? mm-annotationlist)
             annotation
             (conj annotation {:conceptlist (map #(:conceptid %) mm-annotationlist)}))))
       annotationlist))

(defn get-concepts-for-annotations [mmapi document]
  "enhance annotations with conceptids if possible."
  (let [result (partial/partial-match document)]
    (conj result {:annotations (enhance-annotations mmapi (:annotations result))})))
  