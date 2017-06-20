(ns chem.chemdner-paths)

;; (def biocreative-root "/nfsvol/nlsaux16/II_Group_WorkArea/Lan/projects/BioCreative/2013")
(def biocreative-root "/net/lhcdevfiler/vol/cgsb5/ind/II_Group_WorkArea/Lan/projects/BioCreative/2013")

(def training-dir (str biocreative-root "/CHEMDNER_TRAIN_V01"))
(def training-text (str training-dir "/chemdner_abs_training.txt"))
(def training-entities (str training-dir "/cdi_ann_training_13-07-31.txt"))
(def training-extents (str training-dir "/cem_ann_training_13-07-31.txt"))
(def training-annotations (str training-dir "/chemdner_ann_training_13-07-31.txt"))

(def development-dir (str biocreative-root "/CHEMDNER_DEVELOPMENT_V02"))
(def development-text (str development-dir "/chemdner_abs_development.txt"))
(def development-entities (str development-dir "/cdi_ann_development_13-08-18.txt"))
(def development-extents (str development-dir "/cem_ann_development_13-08-18.txt"))
(def development-annotations (str development-dir "/chemdner_ann_development_13-08-18.txt"))

(def training-dev-dir (str biocreative-root "/train_development_abstract"))
(def small-development-text (str training-dev-dir "/chemdner_abs_development.txt"))
(def small-training-text (str training-dev-dir "/chemdner_abs_training.txt"))

(def test-dir (str biocreative-root "/CHEMDNER_TEST_V01"))
(def test-text (str test-dir "/chemdner_abs_test.txt"))

