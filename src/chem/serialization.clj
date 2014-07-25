(ns chem.serialization
  (:import (java.io FileWriter PushbackReader FileReader)))

(defmacro with-out-writer
  "Opens a writer on f, binds it to *out*, and evalutes body.
  Anything printed within body will be written to f."
  [f & body]
  `(with-open [stream# (FileWriter. ~f)]
     (binding [*out* stream#]
       ~@body)))


(defn serialize
  "Print a data structure to a file so that we may read it in later."
  [data-structure #^String filename]
  (with-out-writer
    (java.io.File. filename)
    (binding [*print-dup* true] (prn data-structure))))


;; This allows us to then read in the structure at a later time, like so:
(defn deserialize [filename]
  (with-open [r (PushbackReader. (FileReader. filename))]
    (read r)))