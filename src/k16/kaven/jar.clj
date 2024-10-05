(ns k16.kaven.jar
  (:require
   [clojure.java.io :as io]) 
  (:import
   java.io.FileNotFoundException
   java.io.InputStreamReader
   java.util.jar.JarFile
   java.util.zip.ZipEntry))

(set! *warn-on-reflection* true)

(defn- find-pom-entry
  [^JarFile jar]
  (let [entries (iterator-seq (.entries jar))]
    (some
     (fn [^ZipEntry entry]
       (when (re-find #"META-INF/maven/.*/pom.xml" (.getName entry))
         entry))
     entries)))

(defn- make-resource-path [group artifact]
  (str "META-INF/maven/" group "/" artifact "/pom.xml"))

(defn extract-pom-from-jar
  [jar-path {:keys [resource-path group artifact]}]
  (let [jar-file (JarFile. (io/file jar-path))

        resource-path (or resource-path
                          (when (and group artifact)
                            (make-resource-path group artifact)))

        pom-entry (if resource-path
                    (.getEntry jar-file resource-path)
                    (find-pom-entry jar-file))]

    (when-not pom-entry
      (throw (FileNotFoundException.
              (str "Could not find POM file inside JAR"))))

    (InputStreamReader. (.getInputStream jar-file pom-entry))))
