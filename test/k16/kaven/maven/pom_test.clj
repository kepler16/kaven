(ns k16.kaven.maven.pom-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]
   [k16.kaven.maven.pom :as maven.pom]))

(deftest pom-read-test
  (let [pom (maven.pom/read-pom (str (io/file (io/resource "fixtures/pom.xml"))))]
    (is (= {:group "com.kepler16"
            :artifact "kaven"
            :version "0.0.1-SNAPSHOT"
            :repositories {"clojars" {:id "clojars"
                                      :url "https://repo.clojars.org/"
                                      :snapshots-enabled false
                                      :releases-enabled false}
                           "kepler16" {:id "kepler16"
                                       :url "https://maven.kepler16.com/"
                                       :snapshots-enabled false
                                       :releases-enabled false}}}
           pom))))
