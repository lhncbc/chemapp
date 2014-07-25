(ns chem.core
  (:use [compojure.core]
        [hiccup.core]
        [hiccup.page]
        [hiccup.util]
        [ring.middleware.params :only [wrap-params]]
        [clojure.pprint]
        [chem.html-views])
  (:require [chem.process :as process]
            [chem.utils :as utils]
            [chem.annotations :as annot]
            [chem.chemdner-tools :as chemdner-tools]
            [chem.pipeline :as pipeline]
            [clojure.string :as string])
  (:import (java.net URLDecoder URLEncoder))
  (:gen-class))

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

(defroutes 
  viewer-routes
  (GET "/" []
       (frontpage))

  (GET "/adhoc/" []
       (adhoc-page))

  (GET "/chemdner/" []
       (chemdner-page))

  (GET "/chemdner/:docid/" [docid]
       (view-chemdner-output docid))

  (POST "/adhoc/process/" [document engine]
        (view-adhoc-output document engine (process/process engine document))))

(def app (wrap-params viewer-routes))


