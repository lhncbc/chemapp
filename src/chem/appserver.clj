(ns chem.appserver
  (:require [compojure.core :refer :all]
            [hiccup.core :refer :all]
            [hiccup.page :refer :all]
            [hiccup.util :refer :all]
            [clojure.tools.logging :as log]
            [hiccup.middleware :refer [wrap-base-url]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.multipart-params :refer :all]
            [chem.html-views :refer :all]
            [clojure.string :as string]
            [compojure.route :as route :refer [resources]]
            [chem.json-views :as json-views]
            [chem.cdi-views :as cdi-views]
            [chem.piped-views :as piped-views]
            [chem.process :as process]
            [chem.utils :as utils]
            [chem.annotations :as annot]
            [chem.chemdner-tools :as chemdner-tools]
            [chem.pipeline :as pipeline]
                        [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]])
  (:import (java.net URLDecoder URLEncoder))
  (:gen-class))

(defn print-request
  "Print request to standard output."
  [request]
  (.println System/out "============================================")
  (.println System/out (str "request keys: " (keys request)))
  (dorun 
   (map (fn [[key value]]
            (.println System/out (format "(request %s) -> %s" key value)))
        request)))

(defn log-request
  "Print request to log."
  [request]
  (log/info "============================================")
  (log/info (str "request keys: " (keys request)))
  (dorun 
   (map (fn [[key value]]
            (log/info (format "(request %s) -> %s" key value)))
        request)))

(defn wrap-nop
  [handler]
  (fn [request]
    (log-request request)
    (handler request)))

(defroutes viewer-routes

  (GET "/" [] (fn [req] (frontpage req)))

  (GET "/adhoc/" [] (fn [req] (adhoc-page req)))

  (GET "/chemdner/" [] 
    (fn [req]
      (log/debug (str "/chemdner/ " (prn-str (:params req))))
      (cond (empty? (:params req)) (chemdner-page req)
            (contains? (:params req) "docid") (chemdner-document-page req (get (:params req) "docid")))))

  (GET "/altpubmed/" [] 
    (fn [req]
      (log/debug (str "/altpubmed/ " (prn-str (:params req))))
      (cond (empty? (:params req)) (pubmed-page req)
            (contains? (:params req) "pmid")
            (pubmed-document-page req
                                  (get (:params req) "pmid")
                                  (if (contains? (:params req) "engine")
                                    (get (:params req) "engine")
                                    "combine3")))))

  (GET "/pubmed/" [] (pubmed-entry-page))
  (GET "/pubmed/:pmid/" [pmid] 
    (log/debug (str "/pubmed " pmid))
    (json-views/pubmed-output pmid))

  (GET "/pubmedjson/" [] (pubmed-entry-page))
  (GET "/pubmedjson/:pmid/" [pmid] 
    (log/debug (str "/pubmedjson " pmid))
    (json-views/pubmed-output pmid))

  (GET "/pubmedjson/:pmid/:engine/" [pmid engine] 
    (json-views/pubmed-output pmid engine))

  (GET "/pubmedcdi/:pmid/:engine/" [pmid engine] 
    (cdi-views/pubmed-output pmid engine))

  (POST "/adhoc/json/" [document engine]
    (log/debug "document: " document ", engine:" engine)
    (json-views/adhoc-output document (if (nil? engine)
                                        "combine5"
                                        engine)))

  (POST "/adhoc/piped/" [document engine] 
    (log/debug "document: " document ", engine:" engine)
    (piped-views/adhoc-output document (if (nil? engine)
                                         "combine5"
                                         engine)))

  (POST "/adhoc/process/" [document engine]
    (log/debug "document: " document ", engine:" engine)
    (fn [req]
      (view-adhoc-output req document engine (process/process
                                              (if (nil? engine)
                                                "combine5"
                                                engine)
                                              document))))

    (resources "/")
 )

(def app
  (-> viewer-routes
      wrap-nested-params
      wrap-keyword-params
      wrap-params
      ))
