(ns chem.chemdner-app
  (:require [chem.chemdner-tools :as chemdner-tools]
            [chem.pipeline :as pipeline]
            [clojure.pprint :refer [pprint]])
  (:gen-class))

;; Chemdner test application 

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Chemical Annotator")
  (println (str "chemdner input files: " (string/join args)))
  (pprint
   (map (fn [arg]
          (let [document-list (chemdner-tools/load-chemdner-abstracts arg)
                annot-result-list (pipeline/annotate-document-set document-list "metamap")]
            
            ))
        args)))

