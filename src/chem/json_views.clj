(ns chem.json-views
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [chem.process :as process]
            [chem.backend :as backend]))

(defn pubmed-output
  "Write annotated document map as JSON."
  ([pmid]
   (json/write-str (backend/process-pubmed-document (edn/read-string pmid))))
  ([pmid engine]
   (json/write-str (backend/process-pubmed-document (edn/read-string pmid) engine))))

(defn adhoc-output
  "Write adhoc output as JSON"
  ([document]
   (json/write-str (process/process "combine5" document)))
  ([document engine]
   (json/write-str (process/process engine document))))

