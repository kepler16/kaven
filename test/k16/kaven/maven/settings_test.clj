(ns k16.kaven.maven.settings-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]
   [k16.kaven.maven.settings :as maven.settings]))

(deftest settings-read-test
  (let [settings (maven.settings/read-settings 
                   (io/file (io/resource "fixtures/settings.xml")))]
    (is (= {:servers {"kepler16" {:id "kepler16"
                                  :username "username"
                                  :password "password"}}}
           settings))))
