(ns chem.chemdner-paths)

(def workarea "/net/lhcdevfiler/vol/cgsb5/ind/II_Group_WorkArea")

(def biocreative-root (str workarea "/Lan/projects/BioCreative/2013"))

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

;; CHEMDNER Patents Collection
;; /net/lhcdevfiler/vol/cgsb5/ind/II_Group_WorkArea/wjrogers/Collections/BioCreativeV/CHEMDNER
(def patent-biocreative-root (str workarea "/wjrogers/Collections/BioCreativeV/CHEMDNER"))

(def patent-training-dir (str patent-biocreative-root "/cemp_training_set"))
(def patent-training-text (str patent-training-dir "/chemdner_patents_train_text.txt"))
(def patent-training-eval (str patent-training-dir "/chemdner_cemp_gold_standard_train.tsv"))
(def patent-training-extents (str patent-training-dir "/chemdner_cemp_gold_standard_train_eval.tsv"))
(def patent-training-annotations (str patent-training-dir "/chemdner_cemp_gold_standard_train.tsv"))

(def patent-development-dir (str patent-biocreative-root "/cemp_development_set_v02"))
(def patent-development-text (str patent-development-dir "/chemdner_patents_development_text.txt"))
(def patent-development-eval (str patent-development-dir "/chemdner_cemp_gold_standard_development_eval_v02.tsv"))
(def patent-development-extents (str patent-development-dir "/chemdner_cemp_gold_standard_development_eval_v02.tsv"))
(def patent-development-annotations (str patent-development-dir "/chemdner_cemp_gold_standard_development_v02.tsv"))
