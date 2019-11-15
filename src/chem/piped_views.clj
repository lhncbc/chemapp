(ns chem.piped-views
  (:require [clojure.edn :as edn]
            [chem.process :as process]
            [chem.backend :as backend]
            [chem.piped-output :refer [piped-output]]))

(defn pubmed-output
  "Write annotated document map as JSON."
  ([pmid]
   (piped-output (backend/process-pubmed-document (edn/read-string pmid))))
  ([pmid engine]
   (piped-output (backend/process-pubmed-document (edn/read-string pmid) engine))))

(defn adhoc-output
  "Write adhoc output as JSON"
  ([document]
   (piped-output (process/process "combine5" document)))
  ([document engine]
   (piped-output (process/process engine document))))

