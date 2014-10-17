(ns chem.annotation-utils)

(defn adjacent-to-previous [token prevtoken]
  (= (+ (-> prevtoken :span :end) 1)
        (-> token :span :start)))

(defn mark-adjacent-to-previous [annotation-list]
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

