(ns user
  (:require [chem.core])
  (:require [chem.setup])
  (:require [chem.mongodb])
  (:require [chem.process])
  (:require [chem.metamap-api])
  (:require [chem.evaluation :as eval])
  (:import [java.io File])
  (:use [chem.utils]))

;; load saved partial match results
(let [fname "/rhome/wjrogers/studio/clojure/chem/partial-match-records.readable"]
  (def partial-match-records 
    (if (.exists (File. fname))
      (read-from-file-with-trusted-contents fname)
      (let [records (map chem.process/add-partial-match-annotations
                         training-records)]
        (pr-object-to-file fname records)
        records))))

;; load saved metamap results
(let [fname "/rhome/wjrogers/studio/clojure/chem/metamap-records.readable"]
  (def metamap-records 
    (if (.exists (File. fname))
      ;; load saved metamap results
      (read-from-file-with-trusted-contents fname)
      ;; generate metamap results
      (let [records (map #(chem.process/add-metamap-annotations mmapi %)
                         training-records)]
        (pr-object-to-file fname records)
        records))))

;; merge the above
(def metamap-partial-records (map #(conj %1 %2) metamap-records partial-match-records))

;; add normchem record filtered by metamap acronyms 
(let [fname "/rhome/wjrogers/studio/clojure/chem/flow1-records.readable"]
  (def flow1-records 
    (if (.exists (File. fname))
      ;; load normchem results
      (read-from-file-with-trusted-contents fname)
      ;; generate normchem results
      (let [records (doall (chem.process/map-flow1 metamap-partial-records))]
        (pr-object-to-file fname records)
        records))))

;; (def flow1-chemdner-resultlist (eval/gen-flow1-chemdner-resultlist flow1-records))
;; (eval/write-chemdner-resultlist-to-file "flow1-chemdner-resultlist.txt" flow1-chemdner-resultlist)
;; (def metamap-chemdner-resultlist (eval/gen-metamap-chemdner-resultlist metamap-records))
;; (eval/write-chemdner-resultlist-to-file "metamap-chemdner-resultlist.txt" metamap-chemdner-resultlist)
;; (def partial-chemdner-resultlist (eval/gen-partial-chemdner-resultlist metamap-records))
;; (eval/write-chemdner-resultlist-to-file "partial-chemdner-resultlist.txt" partial-chemdner-resultlist)
;; (def metamap-partial-chemdner-resultlist (eval/gen-partial-chemdner-resultlist metamap-partial-records))
(eval/write-chemdner-resultlist-to-file "metamap-partial-chemdner-resultlist.txt" metamap-partial-chemdner-resultlist)
;; (def metamap-partial-chemdner-subsume-resultlist (eval/gen-partial-chemdner-subsume-resultlist metamap-partial-subsume-records))
(eval/write-chemdner-resultlist-to-file "metamap-partial-chemdner-subsume-resultlist.txt" metamap-partial-subsume-chemdner-resultlist)

;; remove subsumed terms from termlists
(def subsume-flow-records (map chem.process/subsume-flow flow1-records))

;; convert to chemdner format
;; (def subsume-chemdner-resultlist (apply concat (map #(eval/doc-result-to-chemdner-result % :subsume-matched-terms) subsume-flow-records)))

;; (eval/write-chemdner-resultlist-to-file "subsume-chemdner-resultlist.txt" subsume-chemdner-resultlist)

(def metamap2-records (map chem.process/regenerate-metamap-spans metamap-records))
(def metamap2-partial-records (map #(conj %1 %2) flow1-records metamap2-records))

(def subsume2-records (map-flow metamap2-partial-records subsume-flow))

;; convert to chemdner format
;; (def subsume2-chemdner-resultlist (apply concat (map #(eval/doc-result-to-chemdner-result % :subsume-matched-terms) subsume2-records)))

;; (eval/write-chemdner-resultlist-to-file "subsume2-chemdner-resultlist.txt" subsume2-chemdner-resultlist)

;; 

(def subsume2-eval-records (map #(eval/add-wrong-missing % :subsume-matched-terms) subsume2-records))