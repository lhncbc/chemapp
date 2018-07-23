(ns chem.core
  (:require [chem.socketserver :as socketserver])
  (:gen-class))

;; socket server application

(defn -main
  "Start socket server using defaults"
  [& args]
  (if (empty? args)
    (socketserver/init)
    (socketserver/init (nth args 0))))
