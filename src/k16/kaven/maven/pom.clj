(ns k16.kaven.maven.pom
  {:no-doc true}
  (:import
   java.io.FileReader
   java.io.Reader
   org.apache.maven.model.Repository
   org.apache.maven.model.io.xpp3.MavenXpp3Reader))

(set! *warn-on-reflection* true)

(defn- repository-to-map [^Repository repo]
  {:id (.getId repo)
   :url (.getUrl repo)
   :snapshots-enabled (boolean (some-> repo .getSnapshots .isEnabled))
   :releases-enabled (boolean (some-> repo .getReleases .isEnabled))})

(defn read-pom [file-path|reader]
  (let [pom-reader (MavenXpp3Reader.)
        reader (if (instance? Reader file-path|reader)
                 file-path|reader
                 (FileReader. ^String file-path|reader))]

    (with-open [reader ^Reader reader]
      (let [model (.read pom-reader reader)
            repositories (.getRepositories model)]
        {:group (.getGroupId model)
         :artifact (.getArtifactId model)
         :version (.getVersion model)
         :repositories (into {}
                             (comp
                              (map repository-to-map)
                              (map (juxt :id identity)))
                             repositories)}))))
