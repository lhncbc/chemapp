(ns chem.html-views
  (:use [compojure.core]
        [hiccup.core]
        [hiccup.page]
        [hiccup.util])
  (:require [clojure.string :as string])
  (:require [chem.annotations :as annot]))

(defn view-layout [title & content]
  (html
    (doctype :xhtml-strict)
    (xhtml-tag "en"
      [:head
       [:meta {:http-equiv "Content-type"
               :content "text/html; charset=utf-8"}]
       [:title (str "Annotation Viewer - " title)]
       [:style "em { color: red; }"]]
      [:body content])))

(defn frontpage []
   " Generate view with document submission box. "
   (view-layout 
    "Start"
    [:h1 "Annotation Viewer - Start"]
    [:h2 "Submission"]
    [:form {:method "post" :action "/process/"}
     [:textarea {:name "document" :rows 40 :cols 100}]
     [:p]
     [:select {:name "engine"}
      [:option {:value "metamap"}  "Metamap"]
      [:option {:value "partial"}  "Partial Chemical Match"]
      [:option {:value "fragment"} "Chemical Fragment Match"]
      [:option {:value "normchem"} "Normalized Chemical Match"]
      [:option {:value "combine1"} "Combination 1 (partial+norm)"]]
     [:input.action {:type "submit" :value "process"}]] ))

(defn highlight-text-v1 [text annotations]
  (let [matchedwordset (annot/build-matchedwordset-chemicals annotations)]
    (vec 
     (cons :p
           (map (fn [word]
                  (if (contains? matchedwordset (.toLowerCase word))
                    [:em (str word " ")]
                    (str word " ")))
                (.split text "\\ "))))))

(defn highlight-text [text spans]
  (annot/annotate-text text spans "<em>" "</em>" "<bre>"))

(defn view-output [document engine result]
  "unify input document with annotations"
   (view-layout 
    "Result"
    [:h1 "Annotation Viewer - Result"]
    [:h2 "Document"]
    [:p (highlight-text document (result :spans))]
    [:h2 "Engine"]
    [:p engine]
    [:h2 "Matched Terms"]
    [:p (string/join " " (map #(str "\"" % "\"") (result :matched-terms)))]
    [:h2 "Spans"]
    [:p (string/join " " (result :spans))]
    [:h2 "Annotations"]
    [:pre (with-out-str (clojure.pprint/pprint (result :annotations)))]))
  
