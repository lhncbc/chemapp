(ns chem.pubchem
  (:import (java.net URLDecoder URLEncoder))
  (:require [clojure.string :as string])
  (:require [clj-http.client :as client]))

;; For more information on the PubChem User Gateway (PUG) ReST protocol see the following web pages:
;; PUG REST specification
;; http://pubchem.ncbi.nlm.nih.gov/pug_rest/PUG_REST.html
;; PUG REST Tutorial
;; http://pubchem.ncbi.nlm.nih.gov/pug_rest/PUG_REST_Tutorial.html

(defn gen-name-to-cidlist-url [name fmt]
  "gen url cids for name"
  (format "http://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/%s/cids/%s" (URLEncoder/encode name) fmt))

(defn gen-name-to-nearest-match-url [name]
  (format "http://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/%s/PNG" (URLEncoder/encode name)))

(defn gen-sdf-name-url [name]
  (format "http://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/%s/SDF" (URLEncoder/encode name)))

(defn gen-synonym-name-url 
  ([name] (gen-synonym-name-url name "JSON"))
  ([name fmt]
     (format "http://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/%s/synonyms/%s"
             (URLEncoder/encode name) fmt)))

(defn gen-compound-property-url 
  ([cid]               
     (gen-compound-property-url cid "CanonicalSMILES" "JSON"))
  ([cid properties]    
     (gen-compound-property-url cid properties "JSON"))
  ([cid properties fmt]
     (format "http://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/%s/property/%s/%s" 
             cid (string/join properties) fmt)))


(defn lookup [query]  
  (client/get (gen-name-to-cidlist-url query "JSON")))

(defn get-synonymlist [cid]
  (client/get (gen-synonym-name-url cid "JSON")))

(defn get-isomeric-smiles [cid]
  (client/get (gen-compound-property-url cid "IsomericSMILES" "JSON")))

(defn get-canonical-smiles [cid]
  (client/get (gen-compound-property-url cid "CanonicalSMILES" "JSON")))