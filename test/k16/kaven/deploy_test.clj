(ns k16.kaven.deploy-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]
   [k16.kaven.deploy :as kaven.deploy]
   [matcher-combinators.test]))

(def ^:private snapshot-version1-pattern
  #"0\.0\.1-(.*)\.(.*)-(.*)$")

(def ^:private snapshot-version2-pattern
  #"0\.0\.2-(.*)\.(.*)-(.*)$")

(deftest deploy-test
  (let [result (kaven.deploy/deploy
                {:pom-path (str (io/file (io/resource "fixtures/pom.xml")))
                 :jar-path (str (io/file (io/resource "fixtures/lib.jar")))

                 :repositories {"test" {:url "http://localhost:7765/releases"
                                        :credentials {:username "admin"
                                                      :password "secret"}}}

                 :repository "test"})]

    (is (match? {:artifacts [{:group "com.kepler16"
                              :artifact "kaven"
                              :extension "jar"
                              :version snapshot-version1-pattern}
                             {:group "com.kepler16"
                              :artifact "kaven"
                              :extension "pom"
                              :version snapshot-version1-pattern}]}
                result))))

(deftest deploy-with-pom-self-discovery-test
  (let [result (kaven.deploy/deploy
                {:jar-path (str (io/file (io/resource "fixtures/lib.jar")))

                 :repositories {"test" {:url "http://localhost:7765/releases"
                                        :credentials {:username "admin"
                                                      :password "secret"}}}

                 :repository "test"})]

    (is (match? {:artifacts [{:group "com.kepler16"
                              :artifact "kaven"
                              :extension "jar"
                              :version snapshot-version1-pattern}
                             {:group "com.kepler16"
                              :artifact "kaven"
                              :extension "pom"
                              :version snapshot-version1-pattern}]}
                result))))

(deftest deploy-with-overrides-test
  (let [result (kaven.deploy/deploy
                {:pom-path (str (io/file (io/resource "fixtures/pom.xml")))
                 :jar-path (str (io/file (io/resource "fixtures/lib.jar")))

                 :lib 'com.kepler16.override/kaven2
                 :version "0.0.2-SNAPSHOT"

                 :repositories {"test" {:url "http://localhost:7765/releases"
                                        :credentials {:username "admin"
                                                      :password "secret"}}}

                 :repository "test"})]

    (is (match? {:artifacts [{:group "com.kepler16.override"
                              :artifact "kaven2"
                              :extension "jar"
                              :version snapshot-version2-pattern}
                             {:group "com.kepler16.override"
                              :artifact "kaven2"
                              :extension "pom"
                              :version snapshot-version2-pattern}]}
                result))))

(deftest deploy-parallel-test
  (let [results (->> (range 0 20)
                     (map (fn [i]
                            (future
                              (kaven.deploy/deploy
                               {:jar-path (str (io/file (io/resource "fixtures/lib.jar")))
                                :version (str "0.0." i "-SNAPSHOT")
                                :repositories {"test" {:url "http://localhost:7765/releases"
                                                       :credentials {:username "admin"
                                                                     :password "secret"}}}

                                :repository "test"}))))
                     (map deref)
                     doall)]

    (is (match? (->> (range 0 20)
                     (map (fn [i]
                            (let [version-pattern (re-pattern (str "0\\.0\\." i "-(.*)\\.(.*)-(.*)$"))]
                              {:artifacts [{:group "com.kepler16"
                                            :artifact "kaven"
                                            :extension "jar"
                                            :version version-pattern}
                                           {:group "com.kepler16"
                                            :artifact "kaven"
                                            :extension "pom"
                                            :version version-pattern}]}))))
                results))))
