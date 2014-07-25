(defproject chem "0.1.0-SNAPSHOT"
  :description "Chemical Term Detection"
  :url "http://ii.nlm.nih.gov/"
  :license {:name "Public Domain"
            :url "http://metamap.nlm.nih.gov/MMTnCs.shtml"}
  :dependencies [[cc.mallet/mallet "2.0.7"]
                 [com.novemberain/monger "1.7.0"]
                 [compojure "1.1.5"]
                 [congomongo "0.4.1" :exclusions [org.clojure/core.incubator]]
                 [clj-http "0.7.7" :exclusions [commons-logging]]
                 [edu.stanford.nlp/stanford-corenlp "3.3.1"]
                 [gov.nih.nlm.ctx/ctx "1.0-SNAPSHOT"]
                 [gov.nih.nlm.nlp/meta-stacking "1.0-SNAPSHOT"]
                 [gov.nih.nlm.nls/metamap-api "2.0"]
                 [gov.nih.nlm.nls/stanford-ner-extensions "1.0-SNAPSHOT"]
                 [hashtrie "0.1.0-SNAPSHOT"]
                 [hiccup "1.0.3"]
                 [se.sics/prologbeans "4.2.1"]
                 [gov.nih.nlm.nls/metamap-api "2.0pre-SNAPSHOT"]
                 [gov.nih.nlm.nls.lexicalsystems/lvg "2013"]
                 [migration/migration "1.0.0-SNAPSHOT"]
                 [clj-http "0.7.7"]
                 [congomongo "0.4.1"]
                 [com.novemberain/monger "1.7.0"]
                 [instaparse "1.2.2"]
                 [log4j/log4j "1.2.17"]
                 [migration "1.0.0-SNAPSHOT" :exclusions [org.clojure/core.incubator]]
                 [rhizome "0.1.9"]
                 [ring/ring-core "1.3.0" :exclusions [[joda-time]
                                                      [org.clojure/tools.reader]]]
                 [ring/ring-devel "1.3.0" :exclusions [[org.clojure/tools.namespace]
                                                       [org.clojure/tools.reader]
                                                       [joda-time]
                                                       [hiccup]
                                                       [org.clojure/java.classpath]]]
                 [se.sics/prologbeans "4.2.1"]
                 [uk.ac.cam.ch.opsin/opsin-core "1.5.0"]]
  :plugins [[lein-ring "0.8.11" :exclusions [org.clojure/clojure]]]
  :repositories [["java.net" "http://download.java.net/maven/2"]
                 ["ucc-repo" "http://maven.ch.cam.ac.uk/m2repo"]]
  :ring {:init chem.backend/init
         :handler chem.core/app}
  :profiles {:dev
             {:dependencies
              [[org.clojure/clojure "1.5.1"]
               [org.clojure/data.xml "0.0.7"]
               [org.clojure/data.zip "0.1.1"]
               [org.clojure/data.json "0.2.3"]
               [org.clojure/java.classpath "0.2.1"]
               [org.clojure/math.numeric-tower "0.0.2"]
               [org.clojure/tools.nrepl "0.2.3"]
               [org.clojure/tools.trace "0.7.5"]
               [ring-mock "0.1.5"]
               ]}}
  :marginalia {:javascript ["mathjax/MathJax.js"]}
  :uberjar {:aot :all}
  :main chem.core)
