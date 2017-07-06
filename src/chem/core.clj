(ns chem.core
  (:require [chem.chemdner-tools :as chemdner-tools]
            [chem.pipeline :as pipeline]
            [chem.socketserver :as socketserver ]
            [clojure.string :as string])
  (:use [clojure.pprint])
  (:gen-class))

(defn -main
  "Start socket server using defaults"
  [& args]
  (if (empty? args)
    (socketserver/init)
    (socketserver/init (nth args 0))))


(defn previous-main
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


