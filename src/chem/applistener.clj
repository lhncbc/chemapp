(ns chem.applistener
  (:require [clojure.tools.logging :as log]
            [ring.util.servlet]
            [chem.backend]
            [chem.appservlet]
            [chem.appserver])
  (:import [javax.servlet ServletContext ServletContextEvent])
  (:gen-class :implements [javax.servlet.ServletContextListener]))


;; This is a modified (cleaned-up) implementation of the
;; ServletContextListener generated by "lein ring" in which the
;; contextInitialized method calls (init ^ServletContext context) with
;; the ServletContext

(defn -contextInitialized
  "Initialize application and request service handler when servlet
  context is initialized."
  [this ^ServletContextEvent contextEvent]
  ;; run init passing servlet context instance
  (chem.backend/init (.getServletContext contextEvent))
  ;; rest of this stolen from code generated by lein ring  
  (let [service-handler
        (let [app-handler (resolve (quote chem.appserver/app))]
          (fn [request]
            (let [context-path
                  (.getContextPath ^{:column 27,
                                     :line 126,
                                     :tag javax.servlet.http.HttpServletRequest}
                                   (:servlet-request request))]
              (app-handler (assoc request
                                  :context context-path
                                  :path-info (-> (:uri request)
                                                 (subs (.length context-path))
                                                 not-empty 
                                                 (or "/")))))))
        make-service-method (resolve (quote ring.util.servlet/make-service-method))
        method (make-service-method service-handler)]
    (alter-var-root (do 
                      (resolve (quote chem.appservlet/service-method)))
                    (constantly method))))

(defn -contextDestroyed
  [this ^ServletContextEvent contextEvent] nil)
