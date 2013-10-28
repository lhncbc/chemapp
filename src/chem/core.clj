(ns chem.core
  (:use [compojure.core]
        [hiccup.core]
        [hiccup.page]
        [hiccup.util]
        [clojure.pprint]
        [ring.middleware.params :only [wrap-params]]
        [chem.html-views])
  (:require [chem.process :as process])
  (:require [chem.utils :as utils])
  (:require [chem.annotations :as annot])
  (:require [clojure.string :as string])
  (:import (java.net URLDecoder URLEncoder))
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Chemical Annotator")
  (println (str "input files: " (string/join args)))
  (dorun 
   (map (fn [arg]
          (let [document (string/join "\n " (utils/line-seq-from-file arg))
                result (process/process "metamap" document)
                annotationlist {:annotations result}
                spans {:spans result}]
            (println (annot/annotate-text document spans "{" "}" ""))
            (pprint  [:annotations annotationlist])  ))
        args)))

(defroutes 
  viewer-routes
  (GET "/" []
       (frontpage))
  (POST "/process/" [document engine]
        (view-output document engine (process/process engine document))) )
(def app (wrap-params viewer-routes))
