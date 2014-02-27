(ns chem.utils
  (:use [clojure.pprint])
  (:import (java.io BufferedReader FileReader))
  (:gen-class))

(defn line-seq-from-file [file-name]
  " return a lazy seq of lines from supplied filename"
  (line-seq (BufferedReader. (FileReader. file-name))))

(defn print-elements-delimited [coll  delimiter]      
  (count                            
   (doall                         
    (map                          
     #(prn (format "%s" (apply str (interpose delimiter %))))
     coll))))

(defn write-elements-delimited [outfilename coll delimiter]    
  (count                            
   (with-open [w (java.io.FileWriter. outfilename)]   
     (doall                         
      (map                          
       #(.write w (format "%s\n" (apply str (interpose delimiter %))))
       coll)))))

(defn print-elements-piped [coll]   
  (count       
   (doall    
    (map     
     #(prn (format "%s" (apply str (interpose \| %))))       
     coll))))

(defn write-elements-piped [outfilename coll]         
  (count       
   (with-open [w (java.io.FileWriter. outfilename)]   
     (doall    
      (map     
       #(.write w (format "%s\n" (apply str (interpose \| %))))
       coll)))))                    

(defn print-interns [package]       
  (count (map prn (ns-interns package))))             

(defn print-elements [coll]         
  (count (map prn coll)))           

(defn write-elements [outfilename coll]               
  (count       
   (with-open [w (java.io.FileWriter. outfilename)]   
     (doall                  
      (map #(.write w (format "%s\n" %)) coll)))))

(defn write-to-file [outfilename astr]
  (with-open [w (java.io.FileWriter. outfilename)]
    (.write w astr)))

(defn pprint-object-to-file [filename object]
  "pretty print object to file."
  (with-open [w (java.io.FileWriter. filename)]
    (pprint object w)))

(defn pr-object-to-file [filename object]
  "print object readably to file."
  (with-open [w (java.io.FileWriter. filename)]
    (.write w (pr-str object))))

;; read wants its reader arg (or *in*) to be a java.io.PushbackReader.
;; with-open closes r after the with-open body is done.  *read-eval*
;; specifies whether to allow #=() forms when reading, and evaluate
;; them as a side effect while reading.
(defn read-from-file-with-trusted-contents [filename]
  (with-open [r (java.io.PushbackReader.
                 (clojure.java.io/reader filename))]
    (binding [*read-eval* false]
      (read r))))

;; Winnowing a Sequence
;; Problem

;; How do I separate the elements of a sequence into those which
;; satisfy a given predicate and those which fail the test?

;; Solution

;; Adrian Cuthbertson provided this version:

(defn winnow [pred coll]
  (reduce (fn [[a b] x]
            (if (pred x)
              [(conj a x) b]
              [a (conj b x)]))
          [[] []]
          coll))

;; The function makes use of the three-argument version of reduce. We
;; provide reduce a function and a sequence to which to apply it, and
;; here we also specify an initial value for reduce to start with. In
;; this case it is a vector of two empty vectors [[] []] in which to
;; store the results of the winnowing. The first subvector will
;; contain all of the elements which pass the test. The second will
;; contain those which fail the test. The function used by reduce here
;; takes two arguments. The first is the result of winnowing up to
;; this point. This is destructured, so that the "true" values are
;; bound to a and the "false" values bound to b. The second argument
;; is the next element to be processed, and is bound to x. If x passes
;; the test it is conjoined to the sequence a. Otherwise it is added
;; to b.

;; (winnow even? (range 10)) => [(0 2 4 6 8) (1 3 5 7 9)]
;; (winnow #{\a \e \i \o \u} "see and be scene") =>
;;     [(\e \e \a \e \e \e) (\s \space \n \d \space \b \space \s \c \n)]

;; And here is Rich Hickey's own version (although he calls it unzip-with):

(defn unzip-with [pred coll]
  (let [pvs (map #(vector (pred %) %) coll)]
    [(for [[p v] pvs :when p] v)
     (for [[p v] pvs :when (not p)] v)]))

;;fin
