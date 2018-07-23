(ns chem.webapp
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [chem.appserver :refer [app]]))

;; web server application

(defn -main
  "Run webserver on port 3000 "
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (println "Hello, World!")
  (run-jetty app {:port 3000}))

