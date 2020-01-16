(ns entityrec.string-utils)

(defn list-indices [srcstring str]
  "list indices of all occurrences of str in srcstring."
  (loop [i 0
         r (.indexOf srcstring str i)
         ilist []]
    (if (< r 0)
      ilist
      (recur r (.indexOf srcstring str (inc r)) (conj ilist r)))))

(defn list-head-proper-sublists
  "Generate a list of all proper sublists of supplied list creating
  successively smaller lists, each list beginning at the head of the
  supplied list, starting with the supplied list itself and excluding
  the empty list."
  [alist]
  (map (fn [n]
         (take n alist))
       (reverse
        (drop 1
              (range (inc (count alist)))))))

(defn list-tail-proper-sublists
  "Generate a list of all proper sublists of supplied list creating
  successively smaller lists, each list ending at the tail of the
  supplied list, starting with the supplied list itself and excluding
  the empty list."
  [alist]
  (map (fn [n]
         (drop n alist))
       (range (count alist))))

(defn list-all-proper-sublists
  [alist]
  (mapcat list-tail-proper-sublists
          (list-head-proper-sublists alist)))
