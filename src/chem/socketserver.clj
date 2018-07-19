(ns chem.socketserver
  (:require [clojure.string :as string]
            [clojure.core.server :refer [start-server stop-server]]
            [chem.backend :as backend]
            [chem.process :as process]
            [medline.medline-format :as medline-format]
            [medline.medline-xml-format :as medline-xml-format])
  (:gen-class))

(defn read-text-body
  "Determine if string is a Old Medline format or New Medline XML
  document and extract the title and abstract."
  [text]
  (cond
    (= (subs text 0 5) "PMID-") 
    (string/join " " (vals (select-keys (medline-format/reader text) [:title :abstract])))
    (= (subs text 0 1) "<") 
    (string/join " " (vals (select-keys (medline-xml-format/reader text) [:title :abstract])))
    :else text))

(defn decode-medline
  "convert message to map containing engine and document"
  [msg]
  (let [fields (string/split msg #"\|")
        engine    (nth fields 0)
        text      (nth fields 1)
        document  (read-text-body text)]
    (hash-map :engine engine
               :document document)))

(defn decode-simple
  "Convert message to map containing engine and document.
  If processing engine is not specified, default to engine
  \"combine5\" (chem.combine-recognizers/combination-5) "
  [msg]
  (let [fields (string/split msg #"\|")
        engine   (if (= (count fields) 1) "combine5" (nth fields 0))
        title    (if (> (count fields) 1) (nth fields 1) msg)
        abstract (if (> (count fields) 2) (nth fields 2) "")]
    {:engine engine
     :document (str title " " abstract)}))

(defn consolidate-terms
  [resultmap]
  (reduce (fn [newmap annotation]
            (let [key (:text annotation)]
              (if (contains? newmap key)
                (assoc newmap key (hash-map :meshid (:meshid annotation) 
                                            :spans (conj (:spans (newmap key))
                                                         (:span annotation))))
                (assoc newmap key (hash-map :meshid (:meshid annotation)
                                            :spans (vector (:span annotation)))))))
          {} (:annotations resultmap)))

(defn piped-output
  [resultmap]
  (if (empty? (:spans resultmap))
    ""
    (let [idmap (reduce (fn [newmap annot]
                          (if (not (empty? (:meshid annot)))
                            (assoc newmap (string/lower-case (:text annot))
                                   (:meshid annot))
                            newmap))
                        {} (:annotations resultmap))]
      (apply str
             (map (fn [[k v]]
                    (if (nil? k)
                      ""
                      (string/join "|" (list k (idmap (string/lower-case k))
                                             (string/join ";"
                                                          (map #(format "%d,%d" 
                                                                        (:start %)
                                                                        (:end %))
                                                               (:spans v)))
                                             "\n"))))
                  (consolidate-terms resultmap))))))


(defn handle-message
  "Given map containing :engine and :document elements, process
  supplied document using specified engine."
  [message]
  (if (empty? (:document message))
    "input ignored?"
    (piped-output (process/process (:engine message) (:document message)))))

(def running? (atom true))

(defn dispatch
  "handle request"
  []
  (while @running?
    (let [cmd (.readLine *in*)]
      (cond (or (= cmd "quit")
                (= cmd "exit")
                (= cmd "bye"))
            (do
              (.write *out* "bye!\n")
              (.flush *out*)
              (swap! running? not) )    ; set running to false
            :else
            (do 
              (.write *out* (handle-message (decode-simple cmd)))
              (.write *out* "EOF\n")
              (.flush *out*)))
        ))
  (swap! running? not) ; set running back to true
  (Object.))
  
(defn init
  "Supply server hostname as argument (port is optional), if no
  argument is supplied then server only accepts connections from
  localhost/127.0.0.1 on port 32000. "
  ([]
   (backend/init)
   (start-server {:port 32000
                  :name "dispatch"
                  :accept 'chem.socketserver/dispatch
                  :server-daemon false}))
  ([hostname]
   (backend/init)
   (start-server {:address hostname
                  :port 32000
                  :name "dispatch"
                  :accept 'chem.socketserver/dispatch
                  :server-daemon false}))
  ([hostname port]
   (backend/init)
   (start-server {:address hostname
                  :port port
                  :name "dispatch"
                  :accept 'chem.socketserver/dispatch
                  :server-daemon false})))
