(ns chem.umls-semantic-groups
  (:require [chem.utils :as utils])
  (:require [clojure.string :as string]))

(defn load-semantic-group-file [file-name]
  (reduce (fn [sgmap line]
            (let [fields (string/split line #"\|")
                  group-abbrev (nth fields 0)
                  recmap (hash-map :group-abbrev group-abbrev
                                   :group-name (nth fields 1)
                                   :type-id    (nth fields 2)
                                   :type-name  (nth fields 3))]
              (if (contains? sgmap group-abbrev)
                (assoc sgmap group-abbrev (conj (sgmap group-abbrev) recmap))
                (assoc sgmap group-abbrev #{ recmap }))))
          {} (utils/line-seq-from-file file-name)))

;; (def sgmap (chem.umls-semantic-groups/load-semantic-group-file "/usr/local/pub/nlp/umls/SemGroups.txt"))