(defproject chem "0.1.0-SNAPSHOT"
  :description "Chemical Term Detection"
  :url "http://chemer.nlm.nih.gov/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [instaparse "1.2.2"]
                 [rhizome "0.1.9"]
                 [ring/ring-core "1.1.8"]
                 [ring/ring-devel "1.1.8"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.3"]
                 [se.sics/prologbeans "4.2.1"]
                 [gov.nih.nlm.nls/metamap-api "2.0pre-SNAPSHOT"]
                 [gov.nih.nlm.nls.lexicalsystems/lvg "2013"]
                 [migration/migration "1.0.0-SNAPSHOT"]
                 [clj-http "0.7.7"]
                 [congomongo "0.4.1"]
                 [instaparse "1.2.2"]
                 [rhizome "0.1.9"]
                 [cc.mallet/mallet "2.0.7"]]
  :plugins [[lein-ring "0.8.3"]]
  :ring {:init chem.backend/init
         :handler chem.core/app}
  :profiles {:dev
             {:dependencies
              [[org.clojure/data.xml "0.0.7"]
               [org.clojure/data.zip "0.1.1"]
               [org.clojure/data.json "0.2.3"]
               [org.clojure/java.classpath "0.2.1"]
               [org.clojure/math.numeric-tower "0.0.2"]
               [org.clojure/tools.nrepl "0.2.3"]
               [org.clojure/tools.trace "0.7.5"]
               ]}}
  :marginalia {:javascript ["mathjax/MathJax.js"]}
  :main chem.core)
