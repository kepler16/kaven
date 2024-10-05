(ns build
  (:require
   [clojure.string :as str]
   [clojure.tools.build.api :as b]))

(def lib 'com.kepler16/kaven)

(def version
  (str/replace (or (System/getenv "VERSION")
                   "0.0.0")
               #"v" ""))

(def class-dir "target/classes")
(def jar-file "target/lib.jar")

(defn build [_]
  (let [basis (b/create-basis {})]
    (b/delete {:path "target"})

    (b/copy-dir {:src-dirs (:paths basis)
                 :target-dir class-dir})

    (b/write-pom {:class-dir class-dir
                  :lib lib
                  :version version
                  :basis basis
                  :src-dirs (:paths basis)
                  :pom-data [[:description "A Clojure api for interacting with Maven respositories"]
                             [:url "https://github.com/kepler16/kaven"]
                             [:licenses
                              [:license
                               [:name "MIT"]
                               [:url "https://opensource.org/license/mit"]]]]})

    (b/jar {:class-dir class-dir
            :jar-file jar-file})))

(def ^:private clojars-credentials
  {:username (System/getenv "CLOJARS_USERNAME")
   :password (System/getenv "CLOJARS_PASSWORD")})

(defn release [_]
  (let [deploy (requiring-resolve 'k16.kaven.deploy/deploy)]
    (deploy
     {:jar-path jar-file
      :repository "clojars"
      :repositories {"clojars" {:credentials clojars-credentials}}})))
