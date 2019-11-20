(ns chem.core
  (:require [chem.socketserver :as socketserver])
  (:gen-class))

;; socket server application

(defn -main
  "Start socket server using defaults, hostname, or hostname and port."
  [& args]
  (println (str "args:" args))
  (cond (empty? args) (socketserver/init)
        (> (count args) 1) (socketserver/init (nth args 0) (nth args 1))
        (> (count args) 0) (socketserver/init (nth args 0))))
