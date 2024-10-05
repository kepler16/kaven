(ns k16.kaven.jar-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]
   [k16.kaven.jar :as kaven.jar]
   [k16.kaven.maven.pom :as maven.pom]
   [matcher-combinators.test])
  (:import
   java.io.FileNotFoundException))

(deftest read-pom-from-jar-test
  (let [reader (kaven.jar/extract-pom-from-jar
                (io/file "test/fixtures/lib.jar")
                {:resource-path "META-INF/maven/com.kepler16/kaven/pom.xml"})
        pom (maven.pom/read-pom reader)]
    (is (match? {:group "com.kepler16"
                 :artifact "kaven"}
                pom))))

(deftest find-and-read-pom-from-jar-test
  (let [reader (kaven.jar/extract-pom-from-jar
                (io/file "test/fixtures/lib.jar")
                {})
        pom (maven.pom/read-pom reader)]
    (is (match? {:group "com.kepler16"
                 :artifact "kaven"}
                pom))))

(deftest find-and-read-pom-from-jar-using-lib-test
  (let [reader (kaven.jar/extract-pom-from-jar
                (io/file "test/fixtures/lib.jar")
                {:group "com.kepler16"
                 :artifact "kaven"})
        pom (maven.pom/read-pom reader)]
    (is (match? {:group "com.kepler16"
                 :artifact "kaven"}
                pom))))

(deftest not-found-test
  (is (thrown-match? FileNotFoundException nil
                     (kaven.jar/extract-pom-from-jar
                      (io/file "test/fixtures/lib.jar")
                      {:group "unknown"
                       :artifact "kaven"}))))
