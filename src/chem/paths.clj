(ns chem.paths)

(def biocreative-root "/nfsvol/nlsaux16/II_Group_WorkArea/Lan/projects/BioCreative/2013")
(def training-dev-dir (str biocreative-root "/train_development_abstract"))
(def development-text (str training-dev-dir "/chemdner_abs_development.txt"))
(def training-dev-text (str training-dev-dir "/chemdner_abs_training.txt"))
(def training-dir (str biocreative-root "/CHEMDNER_TRAIN_V01"))
(def training-text (str training-dir "/chemdner_abs_training.txt"))
(def training-entities (str training-dir "/cdi_ann_training_13-07-31.txt"))
(def training-extents (str training-dir "/cem_ann_training_13-07-31.txt"))


;; 
;;
;;
;;