(ns chem.utils
  (:use [clojure.pprint])
  (:import (java.io BufferedReader FileReader FileWriter))
  (:require [clojure.data.json :as json])
  (:gen-class))

;; ## Various utility functions

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
  "print object in edn format to file."
  (spit filename (pr-str object)))

;; read wants its reader arg (or *in*) to be a java.io.PushbackReader.
;; with-open closes r after the with-open body is done.  *read-eval*
;; specifies whether to allow #=() forms when reading, and evaluate
;; them as a side effect while reading.
(defn read-from-file-with-trusted-contents [filename]
  (with-open [r (java.io.PushbackReader.
                 (clojure.java.io/reader filename))]
    (binding [*read-eval* false]
      (read r))))

;; ## Winnowing a Sequence
;;
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


(defn nth-vals* [a i m]
  (if (and (map? m) (> i 0))
    (reduce into a (map (fn [v] (nth-vals* a (dec i) v)) (vals m)))
    (conj a m)))

(defn nth-vals [i m]
  (if (nil? m)
    {}
    (nth-vals* [] i m)))

;; One way to convert an UTF-8 string to ascii
;;    String utf = "Some UTF-8 String";
;;    byte[] data = utf.getBytes("ASCII");
;;    String ascii = new String(data);

(defn utf8-to-ascii [text]
  (String. (.getBytes text "ASCII")))

;; (defn utf8-to-metamap-ascii [text]
;;   (String. (.getBytes text "ASCII")))

(defn numkeyword [num]
  "Convert number into a keyword"
  (keyword (format "%d" num)))

(defn maybe-string-to-seq [element]
  "if element is a string convert it to a sequence else return it."
  (case (type element)
    java.lang.String              (re-seq #"[^:]+" element)
    clojure.lang.PersistentVector element
    clojure.lang.PersistentList   element
    element))

(defn maybe-string-to-primitive [element]
  (case (type element)
   java.lang.String    (read-string element)
   java.lang.Long      element
   java.lang.Integer   element
   clojure.lang.BigInt element
   java.lang.Double    element
   clojure.lang.Ratio  element
   element))

(defn maybe-seq-to-piped-string
 ([element] (maybe-seq-to-piped-string element " "))
 ([element delimiter]
    (case (type element)
      java.lang.String              element
      (clojure.string/join delimiter element))))

(defn list-matching-ns [parent]
  (filter #(re-matches (re-pattern (format "%s.*" parent)) (str %)) (all-ns)))

(defn write-edn-to-file
  "write object to file in edn format."
  [outfilename obj]
  (spit outfilename (pr-str obj)))

(defn write-json-to-file
  "write object to file in json format."
  [outfilename obj]
  (spit outfilename (json/write-str obj)))
  

;;fin
