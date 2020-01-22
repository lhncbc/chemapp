(ns chem.html-views
  (:require [compojure.core :refer :all]
            [hiccup.core :refer :all]
            [hiccup.page :refer :all]
            [hiccup.util :refer :all]
            [hiccup.element :refer :all]
            [clojure.string :as string]
            [clojure.edn :as edn]
            [clojure.pprint]
            [chem.annotations :as annot]
            [chem.backend :as backend]
            [chem.metamap-annotation :as mm-annot])
  (:import (java.net URLDecoder URLEncoder)))

(defonce annotation-format (atom [:text :cid :meshid :ncstring :pos :output :class :location :span]))

(def front-page-title (atom "Home"))

(defn view-layout
  "Base view layout."
  [req title & content]
  (html
   (doctype :xhtml-strict)
   (xhtml-tag "en"
              [:head
               [:meta {:http-equiv "Content-type"
                       :content "text/html; charset=utf-8"}]
               [:title (str "Annotation Viewer - " title)]
               (include-css (str (:servlet-context-path req) "/css/style.css"))
               [:style "em { color: red; }"]]
              [:body 
               [:div {:id "content"} 
                content
                [:div {:id "footer"}
                 
                 [:address
                  (if (= title @front-page-title)
                    (str "Annotation Viewer - " @front-page-title)
                    [:a {:href (str (:servlet-context-path req) "/")} (str "Annotation Viewer - " @front-page-title)]) " | "
                  [:a {:href "http://ii.nlm.nih.gov"} "Indexing Initiative"]]]
                
                ]])))

(defn frontpage
  "The Front Page."
  [req]
  (view-layout req
               @front-page-title
               [:h1 "Annotation Viewer - " @front-page-title]
               [:ul
                [:li [:a {:href (str (:servlet-context-path req) "/adhoc/")} "Adhoc Queries"]]
                [:li [:a {:href (str (:servlet-context-path req) "/pubmed/")} "PubMed Documents (javascript client)"]]
                [:li [:a {:href (str (:servlet-context-path req) "/altpubmed/")} "PubMed Documents (static html)"]]
                [:li [:a {:href (str (:servlet-context-path req) "/chemdner/")} "CHEMDNER Training Documents"]]]
               ))

(defn adhoc-page
   " Generate view with document submission box. "
  [req]
  (view-layout req
               "Adhoc"
               [:h1 "Annotation Viewer - Start"]
               [:h2 "Submission"]
               [:form {:method "post" :action (str (:servlet-context-path req) "/adhoc/process/")
                       ;; :enctype "multipart/form-data"
                       }
                [:textarea {:name "document" :rows 40 :cols 100}]
                [:p (backend/gen-engine-option-list)
                 [:input.action {:type "submit" :value "process"}]]] ))

(defn pubmed-link
  "Generate a pubmed link from pmid"
  ([req pmid]
   [:a {:href (format "%s?pmid=%d&engine=combine6" (:uri req) pmid)}
    (format "%d" pmid )])
  ([req pmid engine]
   [:a {:href (format "%s?pmid=%d&engine=%s" (:uri req) pmid engine)}
    (format "%d (%s)" pmid engine)]))

(defn pubmed-page 
  " Generate view with document submission box. "
  [req]
  (view-layout req
               "PubMed"
               [:h1 "PubMed Annotation Viewer"]
               [:h2 "Submission"]
               [:form {:method "get" :action "/altpubmed/"}
                [:input {:name "pmid" :size 20}]
                [:p (backend/gen-engine-option-list)
                 [:input.action {:type "submit" :value "process"}]]]
               [:h2 "Sample queries"]
               [:p
                [:ul
                 [:li (pubmed-link req 23104419)]
                 [:li (pubmed-link req 23550066)]
                 [:li (pubmed-link req 23530020)]
                 [:li (pubmed-link req 23122105)]
                 [:li (pubmed-link req 23494810)]
                 [:li (pubmed-link req 23567486)]
                 [:li (pubmed-link req 23639096)]
                 [:li (pubmed-link req 23198831)]
                 [:li (pubmed-link req 23376090)]
                 [:li (pubmed-link req 23223708)]
                 [:li (pubmed-link req 23429043)]
                 [:li (pubmed-link req 23371488)]
                 [:li (pubmed-link req 23537597)]
                 [:li (pubmed-link req 23511311)]
                 [:li (pubmed-link req 23568512)]
                 [:li (pubmed-link req 23632158)]
                 [:li (pubmed-link req 23462380)]
                 [:li (pubmed-link req 23463336)]
                 [:li (pubmed-link req 23497898)]
                 [:li (pubmed-link req 23122085)]
                 [:li (pubmed-link req 23250357)]
                 ;;      [:li [:a {:href "/altpubmed/?pmid=253161229&engine=combine6"} "25316122 (combine5)"]]
                 ]]
               ))

(defn chemdner-page
   " Generate view with document submission box. "
   [req]
   (view-layout req
    "CHEMDNER Training Documents"
    [:h1 "CHEMDNER Training Documents"]
    [:h2 "Submission"]
    [:form {:method "get" :action "/chemdner/"}
     [:input {:name "docid" :size 20 :value "23104419"}]
     [:input.action {:type "submit" :value "process"}]] ))

(defn highlight-text-v1 [text annotations]
  (let [matchedwordset (mm-annot/build-matchedwordset-chemicals annotations)]
    (vec 
     (cons :p
           (map (fn [word]
                  (if (contains? matchedwordset (string/lower-case word))
                    [:em (str word " ")]
                    (str word " ")))
                (string/split text "\\ "))))))

(defn highlight-text-using-spans
  [text spans]
     (annot/annotate-text-using-spans text spans "<span class=\"mesh\">" "</span>" "<bre>"))

(defn highlight-text-using-annotations
  "Add highlight HTML tags to text based on span in annotations,
  setting color based on present of keyword in annotation."
  [text annotations]
  (annot/annotate-text-using-annotations text annotations
                                         :meshid
                                         "<span class=\"mesh\">"   "</span>" 
                                         "<span class=\"mallet\">" "</span>" 
                                         "<bre>"))

(defn pubmed-document-page
  "Display annotated PubMed document."
  [req pmid engine]
  (view-layout req
               "PubMed"
               [:h1 (str "PubMed Annotation Viewer - " pmid)]
               (let [document (backend/process-pubmed-document engine (edn/read-string pmid))
                     title-annotations  (:title_annotations document)
                     abstract-annotations (:abstract_annotations document)]
                 [:div
                  [:h2 (highlight-text-using-annotations (:title document) title-annotations)]
                  [:p (highlight-text-using-annotations (:abstract document) abstract-annotations)]
                  [:h3 "Engine"]
                  [:p engine]
                  [:h3 "Title Annotations"]
                  [:pre (with-out-str (clojure.pprint/print-table
                                       @annotation-format
                                       title-annotations))]
                  [:h3 "Abstract Annotations"]
                  [:pre (with-out-str (clojure.pprint/print-table
                                       @annotation-format
                                       abstract-annotations))]]
                 )))

(defn highlight-text-sample
  [text annotations]
  [:p "Transient gestational and neonatal hypothyroidism-induced specific changes in " 
  [:em {:class "mesh" :title "{:mallet \"lc NN androgen\",
  :pos \"NN\",
  :output \"FAMILY\",
  :class \"lc\",
  :location \"A\",
  :text \"androgen\",
  :span {:start 80, :end 88}}"} "androgen"]
  " receptor expression in skeletal and cardiac muscles of adult rat."])

(defn view-result
  "View annotated result from document"
  [document engine result]
  [:div
    [:h4 "Document"]
    [:p (highlight-text-using-spans document (:spans result))]
    [:h4 "Engine"]
    [:p (str engine ": " (backend/get-engine-label engine))]
    [:h4 "Matched Terms"]
    [:p (string/join " " (map #(str "\"" % "\",") (map #(:text %) (result :annotations))))]
    [:h4 "Spans"]
    [:p (string/join " " (result :spans))]
    [:h4 "Annotations"]
    [:pre (with-out-str (clojure.pprint/print-table
                         @annotation-format
                         (result :annotations)))]])

(defn view-adhoc-output
  "unify input document with annotations"
   [req document engine result]
   (view-layout req
    "Result"
    [:h1 "Adhoc Annotation Viewer - Result"]
    (view-result document engine result)))
  
(defn chemdner-document-page
  "View annotated version of ChemDNER Training Document."
  [req docid]
  (let [document (backend/get-document (java.lang.Integer/parseInt docid))]
    (if (nil? document)
      (view-layout req
       (str "Document " docid " not found.")
       [:h1 "Document Not found."]
        [:p (str "Document " docid " is not a Chemdner Training Document")]
       [:h2 "Submission"]
       [:form {:method "get" :action "/chemdner/"}
        [:input {:name "docid" :size 20}]
        [:input.action {:type "submit" :value "process"}]])
      (let [engine :combine6 ;; "irutils-normchem"
            result (backend/process-chemdner-document document engine)
            title-annotations  (-> result :title-result :annotations)
            abstract-annotations (-> result :abstract-result :annotations)]        
        (view-layout 
         (str "Result" docid)
         [:h1 (str "ChemDNER Annotation Viewer - " docid)]
         [:h2 (highlight-text-using-annotations (:title document) title-annotations)]
         [:h3 "Abstract"]
         [:p (highlight-text-using-annotations (:abstract document) abstract-annotations)]
         [:h3 "Engine"]
         [:p engine]
         [:h4 "Title Annotations"]
         [:pre (with-out-str
                 (clojure.pprint/print-table
                  @annotation-format title-annotations))]
         [:h4 "Abstract Annotations"]
         [:pre (with-out-str 
                 (clojure.pprint/print-table
                  @annotation-format abstract-annotations))]
         
         )))))

(defn view-pubmed-output 
  [pmid]
  (view-layout 
   (str pmid)
   [:h1 (str "Annotation Viewer " pmid " - Result")]
   (str "Result" pmid)))

(def annotation-display-js "
function insertdocument(httpRequest){
    if (httpRequest.readyState == 4){
        // everything is good, the response is received
        if ((httpRequest.status == 200) || (httpRequest.status == 0)){
            var article = JSON.parse(httpRequest.responseText);
             title_ = article.title;

            // title annotations
            boffset = 0
            var annotations = article.title_annotations
            for (i = 0; i < annotations.length; i++) {
                 var annotation = annotations[i];
	         var s = annotation.text
	         details = JSON.stringify(annotation, undefined, 4)
	         details = details.replace(/\\\"/g, \"&quot;\").replace(/-/g, \"&ndash;\")
	         details = details.replace(/\\ /g, \"&nbsp;\")

                 var replacement = \"<span class='unknown' title='\" + details + \"'>\" +  s + \"</span>\"

                 // use span information instead of regular expressions	     
                 start = boffset + annotation.span.start
                 end = boffset + annotation.span.end
                 title_ = [title_.slice(0,start),replacement,title_.slice(end)].join('')
                 boffset = boffset + (replacement.length - (end - start))
	    }
            document.getElementById(\"title\").innerHTML = title_

            // abstract annotations
            document.getElementById(\"title\").innerHTML = article.title;
	    abstract_ = article.abstract;
            boffset = 0
            var annotations = article.abstract_annotations
            for (i = 0; i < annotations.length; i++) {
                 var annotation = annotations[i];
	         var s = annotation.text
	         details = JSON.stringify(annotation, undefined, 4)
	         details = details.replace(/\\\"/g, \"&quot;\").replace(/-/g, \"&ndash;\")
	         details = details.replace(/\\ /g, \"&nbsp;\")
	    
                 if (\"meshid\" in annotation) {
                    var replacement = \"<span class='known' title='\" + details + \"'>\" +  s + \"</span>\"
                 } else {
                    var replacement = \"<span class='unknown' title='\" + details + \"'>\" +  s + \"</span>\"
                 }
                 // use span information instead of regular expressions	     
                 start = boffset + annotation.span.start
                 end = boffset + annotation.span.end
                 abstract_ = [abstract_.slice(0,start),replacement,abstract_.slice(end)].join('')
                 boffset = boffset + (replacement.length - (end - start))
	    }
            document.getElementById(\"abstract\").innerHTML = abstract_
        } else {
            document.getElementById(\"abstract\").innerHTML = '<p>There was a problem with the request: status ' + httpRequest.status;
        }
    }
}

function process( ){
  var pmid = document.getElementById(\"pmid\")
  var the_url =  \"/pubmed/\" + pmid.value + \"/\"
  var request = new XMLHttpRequest();
  request.onreadystatechange = function() { insertdocument(request); };
  request.open(\"GET\", the_url, true);
  request.setRequestHeader(\"Content-Type\", \"text/plain\");
  request.setRequestHeader('X-Requested-With', 'XMLHttpRequest'); // Tells server that this call is made for ajax purposes
                                                                  // Most libraries like jQuery/Prototype/Dojo do this
  request.send(null); // No data needs to be sent along with the request.
}

")

(defn pubmed-entry-page []
  (html
   (doctype :xhtml-strict)
   (xhtml-tag 
    "en"
    [:head
     [:meta {:http-equiv "Content-type"
             :content "text/html; charset=utf-8"}]
     [:title (str "PubMed Annotation Viewer")]
     (include-css "/css/style.css")
     (javascript-tag annotation-display-js)]
    [:body 
     [:h1 "PubMed Annotation Viewer"]
     [:p
      [:div {:id "content"}
       [:div {:id "left"}
        [:div {:id "input"}
         [:label "PMID: "]
         [:input {:id "pmid", :type "number" :value "23104419"}]
         [:button {:onclick "process();"} "Submit"]]]
       [:div {:id "right"}
        [:h2 {:id "title"} "Title"]
        [:br]
        [:p {:id "abstract"} "ABSTRACT"]]]
      [:div {:id "footer"}
       [:address
        [:a {:href "http://ii.nlm.nih.gov"} "Indexing Initiative"]]]]]
    )))
