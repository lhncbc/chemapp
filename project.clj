(defproject chem "0.1.0-SNAPSHOT"
  :description "Chemical Term Detection"
  :url "http://ii.nlm.nih.gov/"
  :license {:name "Public Domain"
            :url "http://metamap.nlm.nih.gov/MMTnCs.shtml"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.2"]
                 [org.clojure/data.json "0.2.6"]
                 [cc.mallet/mallet "2.0.7"]
                 [clojure-opennlp "0.3.2"] ;; uses Opennlp 1.5.3
                 [se.sics/prologbeans "4.2.1"]
                 [ns-tracker "0.3.1"]
                 [congomongo "0.4.1"]
                 [com.novemberain/monger "1.7.0"]
                 [org.apache.lucene/lucene-core "4.10.0"]
                 [org.apache.lucene/lucene-queryparser "4.10.0"]
                 [org.apache.lucene/lucene-queries "4.10.0"]
                 [org.apache.lucene/lucene-analyzers-common "4.10.0"]
                 [org.apache.lucene/lucene-highlighter "4.10.0"]
                 [instaparse "1.4.3"]
                 [log4j/log4j "1.2.17"]
                 [rhizome "0.1.9"]
                 [javax.servlet/servlet-api "2.5"]
                 [compojure "1.6.0" :exclusions [instaparse]]
                 [hiccup "1.0.5"]
                 [clj-http "2.3.0" :exclusions [commons-logging
                                                org.clojure/tools.reader
                                                com.fasterxml.jackson.core/jackson-core]]
                 [clj-sockets "0.1.0"]
                 [com.gearswithingears/async-sockets "0.1.0"]
                 [ring/ring-headers "0.3.0"]
                 [ring/ring-servlet "1.6.1"]
                 [ring/ring-jetty-adapter "1.6.1"]
                 [ring/ring-json "0.4.0"]
                 [ring.middleware.logger "0.5.0"]
                 [uk.ac.cam.ch.opsin/opsin-core "1.5.0"]
                 [bioc "1.0.1"]
                 [gov.nih.nlm.ctx/ctx "1.0-SNAPSHOT"]
                 [gov.nih.nlm.nlp/meta-stacking "1.0-SNAPSHOT"]
                 [gov.nih.nlm.nls/stanford-ner-extensions "1.0-SNAPSHOT"]
                 [hashtrie "0.1.0-SNAPSHOT"]
                 [gov.nih.nlm.nls/metamap-api "2.0"]
                 [metamap-api "1.0-SNAPSHOT"]
                 [skr "0.1.0-SNAPSHOT"]
                 [utils "0.1.0-SNAPSHOT"]
                 [gov.nih.nlm.nls/mallet-example "1.0-SNAPSHOT"]
                 [lexicalsystems/lex-access "2012"]
                 [javax.servlet/servlet-api "2.5"]
                 [clj-diff "1.0.0-SNAPSHOT"]
                 [irutils "2.0"]
                 [reduce-fsm "0.1.4"]]
  :plugins [[lein-ring "0.12.0" :exclusions [org.clojure/clojure]]]
  :repositories [;; ["java.net" "http://download.java.net/maven/2"]
                 ;; ["ucc-repo" "http://maven.ch.cam.ac.uk/m2repo"]
                 ["ii-repo" "https://metamap.nlm.nih.gov/maven2"]]
  :ring {:init chem.backend/init
         :handler chem.appserver/app}
  :profiles {:dev
             {:dependencies
              [[org.clojure/java.classpath "0.2.2"]
               [org.clojure/math.numeric-tower "0.0.2"]]}}
  :marginalia {:javascript ["mathjax/MathJax.js"]}
  :uberjar {:aot :all}
  :aot [chem.core
        chem.socketserver
        chem.irutils
        chem.irutils-normchem
        chem.process
        chem.span-utils]
  :main chem.core
  :pedantic? true)

