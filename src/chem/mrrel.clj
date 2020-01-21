(ns chem.mrrel
  )

;; Is this used?  If so, use MRREL indexed with irutils for this.

;; (defn list-objects-for-subject 
;;   ^{:doc "list object cuis for supplied subject cui in MRREL" }
;;   ([subject-cui]      (m/fetch :mrrel :where {:cui2 subject-cui, :rela "isa"}))
;;   ([subject-cui rela] (m/fetch :mrrel :where {:cui2 subject-cui, :rela rela})))

;; (defn display-objects-for-subject
;;   ^{:doc "display object cuis for supplied subject cui in MRREL" }
;;   ([subject-cui] (map #(prn (list (:cui1 %) (:rela %) (:cui2 %)))
;;                       (list-objects-for-subject subject-cui)))
;;   ([subject-cui rela] (map #(prn (list (:cui1 %) (:rela %) (:cui2 %)))
;;                            (list-objects-for-subject subject-cui rela))))



