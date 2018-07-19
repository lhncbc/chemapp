(ns normchem-ivf
  (:require [clojure.inspector :refer [inspect inspect-table inspect-tree]]
            [clojure.pprint :refer [print-table pprint]]
            [clojure.repl :refer :all]
            [chem.irutils :refer [create-indexes lookup nmslookup] :as civf]))


(def gwa "/net/lhcdevfiler/vol/cgsb5/ind/II_Group_WorkArea")
(def gwah (str gwa "/wjrogers"))
(def ivf (str gwah "/studio/clojure/chem/data/ivf"))
(def tablepath (str ivf "/normchem2017/tables"))
(def indexpath (str ivf "/normchem2017/indices"))
(def indexname "normchem2017")
(def norm-index (create-indexes tablepath indexpath indexname))
