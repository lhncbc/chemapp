(ns chem.meta-data-trie)

(defn map-filter [f coll] (map f (filter f coll)))

(defn add-to-trie [trie x meta-data]
  (assoc-in trie x (merge (get-in trie x) {:val x
                                           :meta-data meta-data
                                           :terminal true})))

(defn in-trie? [trie x]
  "Returns true if the value x exists in the specified trie."
  (:terminal (get-in trie x) false))

(defn prefix-matches [trie prefix]
  "Returns a list of matches with the prefix specified in the trie specified."
  (map-filter :val (tree-seq map? vals (get-in trie prefix))))

(defn build-trie [coll]
  "Builds a trie over the values in the specified seq coll."
  (reduce (fn [newtrie [x metadata]]
            (add-to-trie newtrie x metadata)))
          {} coll))
                   
(defn get-meta-data [trie x]
  "Returns metadata if the value x exists in the specified trie."
  (:meta-data (get-in trie x) nil))
