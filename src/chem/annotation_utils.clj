(ns chem.annotation-utils
  (:gen-class))

(defn adjacent-to-previous
  [token prevtoken]
  (= (+ (-> prevtoken :span :end) 1)
     (-> token :span :start)))

(defn mark-adjacent-to-previous
  [annotation-list]
  (loop [annot-list (rest annotation-list)
         token (first annotation-list)
         prevtoken nil
         new-annot-list []]
    (if (empty? annot-list)
      (conj new-annot-list (assoc token
                                  :adjacent 
                                  (if (adjacent-to-previous token prevtoken)
                                    true
                                    false)))
      (cond
        (nil? prevtoken) (recur (rest annot-list)
                                (first annot-list)
                                token
                                (conj new-annot-list (assoc token :adjacent false)))
        (adjacent-to-previous token prevtoken) (recur (rest annot-list)
                                                      (first annot-list)
                                                      token
                                                      (conj new-annot-list (assoc token
                                                                                  :adjacent true)))
        :else (recur (rest annot-list)
                     (first annot-list)
                     token
                     (conj new-annot-list (assoc token :adjacent false)))))))


(defn adjacent-to-next [token nexttoken]
  (= (+ (-> token :span :end) 1)
     (-> nexttoken :span :start)))

(defn consolidate-adjacent-annotations
  "Consolidate multiple tokens adjacent to each other.  Irrelevant
  tokens must be removed beforehand. "
  [annotation-list]
  (loop [token     (first annotation-list)
         nexttoken (second annotation-list)
         annot-list (rest annotation-list)
         new-annot-list []]
    (if (empty? annot-list)
      (conj new-annot-list token)
      (cond
        (adjacent-to-next token nexttoken) 
        (let [combined-token {:text (str (:text token) " " (:text nexttoken))
                              :output (:output token)
                              :span {:start (-> token :span :start) 
                                     :end   (-> nexttoken :span :end)}
                              :origin [token nexttoken]}] 
          ;; Combine first and next tokens and then
          ;; check for other adjacent tokens.
          (recur combined-token
                 (second annot-list)
                 (rest annot-list)
                 new-annot-list))
        :else (recur (first annot-list)
                     (second annot-list)
                     (rest annot-list)
                     (conj new-annot-list token))))))


;; if (start_1 >= start_n) and (end_1 <= end_n)
(defn is-annotation-subsumed?
  "Is annotation subsumed by another annotation in annotation list?"
  [annotation annot-list]
  (> (count 
      (filter (fn [tannotation]
                (and (>= (-> annotation :span :start) (-> tannotation :span :start ))
                     (<= (-> annotation :span :end) (->  tannotation :span :end))))
              annot-list))
     0))

(defn remove-subsumed-annotations
  "Remove any annotations that are subsumed by a larger spanning annotation."
  [annotation-list]
  (if (empty? annotation-list) 
    annotation-list
    (let [rev-annotation-list (reverse annotation-list)]
      (loop [annotation (first rev-annotation-list)
             restannotation-list (rest rev-annotation-list)
             newannotation-list []]
        (if (empty? restannotation-list) 
          (conj newannotation-list annotation)
          (if (is-annotation-subsumed? annotation restannotation-list)
            (recur (first restannotation-list) (rest restannotation-list) newannotation-list)
            (recur (first restannotation-list) (rest restannotation-list) (conj newannotation-list annotation))))))))

(defn generate-nested-discard-set
  "Generate set of annotations to be discarded because of begin nested
  inside another annotation."
  [annotation-list]
  (let [sorted-annotation-list (vec (sort-by #(-> % :span :start) annotation-list))]
    (reduce (fn [newset [e-annot & targets]]
              (clojure.set/union newset 
                                 (set 
                                  (map #(:span %)
                                       (filter (fn [t-annot]
                                                 (and (>= (-> t-annot :span :start)
                                                          (-> e-annot :span :start))
                                                      (<= (-> t-annot :span :end)
                                                          (-> e-annot :span :end))))
                                               targets)))))
            #{} (concat (partition 4 1 sorted-annotation-list)
                        (partition 2 1 (take-last 4 sorted-annotation-list))))))

(defn remove-nested-annotations
  "Remove annotations nested inside another annotation."
  [annotation-list]
  (let [discardset (generate-nested-discard-set annotation-list)]
    (filter #(and (not (nil? %))
                  (not (contains? discardset (-> % :span))))
            annotation-list)))
