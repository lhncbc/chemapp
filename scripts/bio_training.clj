(ns bio-training
  (:require [clojure.java.io :as io]
            [chem.bio-feature-generation :as bfg]))

;;
;; generate annotated trainging sets for various machine learning systems
;; 

(def training-features-dir "data/features/chemdner1-bio-simple-filtered/training")
(def development-features-dir "data/features/chemdner1-bio-simple-filtered/development")

(load-file "scripts/setupscript.clj")

(bfg/render-cem-feature-files training-features-dir
                              user/training-records
                              user/chemdner-training-cem-gold-map)

(bfg/render-cem-feature-files development-features-dir
                              user/development-records
                              user/chemdner-development-cem-gold-map)

;; BIO-format training sets

;; CHEMDNER 1 training and developement sets

(def training-annot-features-dir "data/features/chemdner1-bio-multiclass/training")
(def development-annot-features-dir "data/features/chemdner1-bio-multiclass/development")

(bfg/render-annotation-feature-files training-annot-features-dir
                                     user/training-records
                                     user/training-docid-annotation-list-map)

(bfg/render-annotation-feature-files development-annot-features-dir
                                     user/development-records
                                     user/development-docid-annotation-list-map)

;; CHEMDNER Patent

(load-file "scripts/patentsetupscript.clj")

(def patent-training-annot-features-dir "data/features/chemdner-patent-bio-multiclass/training")
(def patent-development-annot-features-dir "data/features/chemdner-patent-bio-multiclass/development")

(bfg/render-annotation-feature-files patent-training-annot-features-dir
                                     patentsetupscript/training-records
                                     patentsetupscript/training-docid-annotation-list-map)

(bfg/render-annotation-feature-files patent-development-annot-features-dir
                                     patentsetupscript/development-records
                                     patentsetupscript/development-docid-annotation-list-map)


;; 
;; Mallet Training 
;;
;; Feature generation at the repl:
;;
;;    user> (load-file "scripts/mallet_features.clj")
;; 
;; Train the CRF at the command line:
;;
;;    $ cd data/features/mallet/chemdner-patent-bio-multiclass/training
;;    $ cat 2*.fv > data/features/all.fv
;;    $ cd  ../../../../../..
;;    $ ~/scripts/SimpleTagger.sh --model-file seqtagcrf.model --train true \
;;        data/features/all.fv
;;
;;
;; Testing on the command line:
;;
;;    $ ~/scripts/SimpleTagger.sh --include-input true --model-file seqtagcrf.model \
;;        data/features/mallet/chemdner-patent-bio-multiclass/development/22564442.fv
;;
;; 
 
