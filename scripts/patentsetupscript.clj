(ns patentsetupscript
  (:require [chem.chemdner-data :as chemdner-data]
            [chem.chemdner-tools :as chemdner-tools]
            [chem.chemdner-paths :as paths]
            [chem.utils :refer :all]))

(def development-records
  (chem.chemdner-tools/load-chemdner-abstracts paths/patent-development-text))
(def training-records
  (chem.chemdner-tools/load-chemdner-abstracts paths/patent-training-text))
(def development-annotations
  (chemdner-tools/load-chemdner-annotations paths/patent-development-annotations))
(def training-annotations
  (chemdner-tools/load-chemdner-annotations paths/patent-training-annotations))
(def development-docid-annotation-list-map
  (chem.chemdner-tools/gen-docid-annotation-list-map development-annotations))
(def training-docid-annotation-list-map
  (chem.chemdner-tools/gen-docid-annotation-list-map training-annotations))
