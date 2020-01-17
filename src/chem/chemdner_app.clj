(ns chem.chemdner-app
  (:require [chem.backend :as backend]
            [chem.chemdner-tools :as chemdner-tools]
            [chem.pipeline :as pipeline]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]])
  (:gen-class))

;; Chemdner test application 

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Chemical Annotator")
  (println (str "chemdner input files: " (string/join args)))
  (backend/init)
  (pprint
   (map (fn [arg]
          (let [document-list (chemdner-tools/load-chemdner-abstracts arg)
                annot-result-list (pipeline/annotate-document-set document-list "combine5")]
            
            ))
        args)))

