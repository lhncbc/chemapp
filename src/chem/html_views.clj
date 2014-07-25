(ns chem.html-views
  (:use [compojure.core]
        [hiccup.core]
        [hiccup.page]
        [hiccup.util])
  (:require [clojure.string :as string])
  (:require [chem.metamap-annotation :as mm-annot])
  (:require [chem.annotations :as annot])
  (:require [clojure.pprint])
  (:require [chem.backend :as backend])
  (:import (java.net URLDecoder URLEncoder)))

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
   (view-layout 
    "Start"
    [:h1 "Annotation Server -Start"]
    [:ul
     [:li [:a {:href "/adhoc/"} "Adhoc Queries"]]
     [:li [:a {:href "/chemdner/"} "CHEMDNER Training Documents"]]]
    ))

(defn adhoc-page []
   " Generate view with document submission box. "
   (view-layout 
    "Adhoc"
    [:h1 "Annotation Viewer - Start"]
    [:h2 "Submission"]
    [:form {:method "post" :action "/adhoc/process/"}
     [:textarea {:name "document" :rows 40 :cols 100}]
     [:p
      [:select {:name "engine"}
       [:option {:value "meta-classifier"}   "Meta-Classifier"]
       [:option {:value "metamap"}           "Metamap"]
       [:option {:value "partial"}           "Partial Chemical Match"]
       [:option {:value "partial-enhanced"}  "Partial Chemical Match Enhanced"]
       [:option {:value "fragment"}          "Chemical Fragment Match"]
       [:option {:value "normchem"}          "Normalized Chemical Match"]
       [:option {:value "combine1"}          "Combination 1 (partial+norm)"]]
      [:input.action {:type "submit" :value "process"}]]] ))
   
(defn chemdner-page []
   " Generate view with document submission box. "
   (view-layout 
    "CHEMDNER Training Documents"
    [:h1 "CHEMDNER Training Documents"]
    [:ul 
     (map (fn [docid]
            [:li [:a {:href (str "/chemdner/" docid "/")} docid]])
          (backend/list-docids 3500))]  ))

(defn highlight-text-v1 [text annotations]
  (let [matchedwordset (mm-annot/build-matchedwordset-chemicals annotations)]
    (vec 
     (cons :p
           (map (fn [word]
                  (if (contains? matchedwordset (.toLowerCase word))
                    [:em (str word " ")]
                    (str word " ")))
                (.split text "\\ "))))))

(defn highlight-text [text spans]
  (annot/annotate-text text spans "<em>" "</em>" "<bre>"))

(defn view-result [document engine result]
  [:div
    [:h4 "Document"]
    [:p (highlight-text document (annot/get-spans-from-annotations (result :annotations)))]
    [:h4 "Engine"]
    [:p engine]
    [:h4 "Matched Terms"]
    [:p (string/join " " (map #(str "\"" % "\"") (result :matched-terms)))]
    [:h4 "Spans"]
    [:p (string/join " " (annot/get-spans-from-annotations (result :annotations)))]
    [:h4 "Annotations"]
    [:pre (with-out-str (clojure.pprint/pprint (result :annotations)))]])

(defn view-adhoc-output [document engine result]
  "unify input document with annotations"
   (view-layout 
    "Result"
    [:h1 "Adhoc Annotation Viewer - Result"]
    (view-result document engine result)))
  
(defn view-chemdner-output [docid]
  (let [document (backend/get-document (java.lang.Integer/parseInt docid))
        engine "partial"
        result (backend/process-chemdner-document document engine)]        
  (view-layout 
   (str "Result" docid)
   [:h1 (str "Annotation Viewer " docid " - Result")]
   [:h2 "Document"]
   [:h3 "Title"]
   (view-result (:title document) engine (:title-result result))
   [:h3 "Abstract"]
   (view-result (:abstract document) engine (:abstract-result result))

)))
