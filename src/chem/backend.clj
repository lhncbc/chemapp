(ns chem.backend
  (:require [chem.mongodb :as chemdb]))

(defn init []
  (chemdb/init))
