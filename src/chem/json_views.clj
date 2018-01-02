(ns chem.json-views
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [chem.backend :as backend]))

(defn pubmed-output [pmid]
  (json/write-str (backend/process-pubmed-document (edn/read-string pmid))))

