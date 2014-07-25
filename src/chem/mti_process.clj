(ns chem.mti-process)

(def mti-program-path "/nfsvol/nls/bin/MTI")

(defn run-cmd [cmd]
  (let [process (.exec (java.lang.Runtime/getRuntime) cmd)
        bri (java.io.BufferedReader. (java.io.InputStreamReader. (.getInputStream process)))
        bre (java.io.BufferedReader. (java.io.InputStreamReader. (.getErrorStream process)))]
    (dorun (map println (line-seq bri)))
    (dorun (map println (line-seq bre)))))


;; (def 
;; (run-cmd (format "%s %s %s" chem/mti-process/mti-program-path paths/development-text "results.out"))