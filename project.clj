(defproject chem "0.1.1-SNAPSHOT"
  :description "Chemical Term Detection"
  :url "https://ii.nlm.nih.gov/"
  :license {:name "Public Domain"
            :url "https://metamap.nlm.nih.gov/MMTnCs.shtml"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.3"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.cli "0.4.2"]
                 [org.clojure/tools.logging "0.5.0"]
                 [org.clojure/math.numeric-tower "0.0.2"]
                 [cc.mallet/mallet "2.0.7"]
                 [clojure-opennlp "0.3.2"] ;; uses Opennlp 1.5.3
                 ;; [se.sics/prologbeans "4.3.2"]
                 [org.apache.lucene/lucene-core "4.10.0"]
                 [org.apache.lucene/lucene-queryparser "4.10.0"]
                 [org.apache.lucene/lucene-queries "4.10.0"]
                 [org.apache.lucene/lucene-analyzers-common "4.10.0"]
                 [org.apache.lucene/lucene-highlighter "4.10.0"]
                 [instaparse "1.4.10"]
                 [log4j/log4j "1.2.17"]
                 [rhizome "0.1.9"]
                 [reduce-fsm "0.1.4"]
                 [javax.servlet/servlet-api "2.5"]
                 [compojure "1.5.1" :exclusions [instaparse]]
                 [hiccup "1.0.5"]
                 [clj-http "2.3.0" :exclusions [commons-logging
                                                org.clojure/tools.reader
                                                com.fasterxml.jackson.core/jackson-core]]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [uk.ac.cam.ch.opsin/opsin-core "2.4.0"]
                 [me.raynes/fs "1.4.6"]
                 [clj-glob "1.0.0"]
                 ;; deeplearning library using Math Kernel Library (MKL)
                 ;; be sure that The Intel Math Kernel Library is in LD_LIBRARY_PATH
                 ;; [uncomplicate/neanderthal "0.24.0"]
                 [gov.nih.nlm.ctx/ctx "1.0-SNAPSHOT"]
                 [gov.nih.nlm.nlp/meta-stacking "1.0-SNAPSHOT"]
                 [gov.nih.nlm.nls/stanford-ner-extensions "1.0-SNAPSHOT"]
                 [hashtrie "0.1.0-SNAPSHOT"]
                 [skr "0.1.2-SNAPSHOT"]
                 [ii/utils "0.1.1-SNAPSHOT"]
                 [bioc "1.0.1"]
                 ;; [metamap-api "2.0"]
                 ;; [gov.nih.nlm.nls/mallet-example "1.0-SNAPSHOT"]
                 [gov.nih.nlm.nls.lexaccess/lexaccess-dist "2013"]
                 [javax.servlet/servlet-api "2.5"]
                 [clj-diff "1.0.0-SNAPSHOT"]
                 [irutils "2.0"]
                 [standoff "0.1.0-SNAPSHOT"]
                 
                 ]
  :plugins [[lein-ring "0.8.12" :exclusions [org.clojure/clojure]]]
  :repositories [["metamap" "https://metamap.nlm.nih.gov/maven2"]
                 ;; ["java.net" "https://download.java.net/maven/2"]
                 ;; ["ucc-repo" "https://maven.ch.cam.ac.uk/m2repo"]
                 ]
  :ring {:init chem.backend/init
         :handler chem.appserver/app
         :web-xml "web.xml"}
  :profiles {:dev
             {:dependencies
              [[org.clojure/java.classpath "0.2.2"]]}}
  :marginalia {:javascript ["mathjax/MathJax.js"]}
  :uberjar {:aot :all}
  :aot [chem.core
        chem.backend
        chem.applistener
        chem.appservlet]
  :main chem.core)

