(ns k16.kaven.deploy
  (:require
   [clojure.java.io :as io]
   [k16.kaven.jar :as kaven.jar]
   [k16.kaven.maven.pom :as maven.pom]
   [k16.kaven.maven.repository :as maven.repository]
   [k16.kaven.maven.session :as maven.session]
   [k16.kaven.maven.settings :as maven.settings]
   [k16.kaven.maven.system :as maven.system]
   [meta-merge.core :as meta-merge])
  (:import
   java.nio.file.Files
   java.nio.file.attribute.FileAttribute
   org.eclipse.aether.artifact.Artifact
   org.eclipse.aether.artifact.DefaultArtifact
   org.eclipse.aether.deployment.DeployRequest
   org.eclipse.aether.deployment.DeployResult
   org.eclipse.aether.util.artifact.SubArtifact))

(set! *warn-on-reflection* true)

(defn- make-artifact [{:keys [group artifact extension version path]}]
  (let [artifact (DefaultArtifact. group artifact "" extension version)]
    (.setFile artifact (io/file path))))

(defn- make-sub-artifact [{:keys [parent extension path]}]
  (let [artifact (SubArtifact. parent "" extension)]
    (.setFile artifact (io/file path))))

(defn- deploy-result->map [^DeployResult result]
  (let [artifacts
        (into []
              (map (fn [^Artifact artifact]
                     {:group (.getGroupId artifact)
                      :artifact (.getArtifactId artifact)
                      :extension (.getExtension artifact)
                      :version (.getVersion artifact)}))
              (.getArtifacts result))]
    {:artifacts artifacts}))

(defn- write-reader-to-temp-file [reader]
  (let [temp-file (Files/createTempFile "temp-file" ".txt" (into-array FileAttribute []))
        file-path (.toFile temp-file)]
    (spit file-path (slurp reader))
    (str file-path)))

(defn deploy [{:keys [pom-path jar-path
                      repositories repository
                      lib version]}]
  (let [settings (maven.settings/read-settings)

        group (when lib (namespace lib))
        artifact (when lib (name lib))

        pom-file-exists? (and pom-path
                              (.exists (io/file pom-path)))

        pom-path (if pom-file-exists?
                   pom-path
                   (write-reader-to-temp-file
                    (kaven.jar/extract-pom-from-jar
                     jar-path {:group group
                               :artifact artifact
                               :resource-path pom-path})))

        pom (maven.pom/read-pom pom-path)
        repositories (meta-merge/meta-merge (:repositories pom)
                                            repositories)

        repositories (maven.repository/create-repositories
                      settings repositories)

        repository (get repositories repository)

        group (or group (:group pom))
        artifact (or artifact (:artifact pom))
        version (or version (:version pom))

        jar-artifact
        (make-artifact {:group group
                        :artifact artifact
                        :extension "jar"
                        :version version
                        :path jar-path})

        pom-artifact
        (make-sub-artifact {:parent jar-artifact
                            :extension "pom"
                            :path pom-path})

        deploy-request (DeployRequest.)]

    (-> deploy-request
        (.addArtifact jar-artifact)
        (.addArtifact pom-artifact)
        (.setRepository repository))

    (let [system (maven.system/create-repository-system)
          session (maven.session/create-system-session system)
          result (.deploy system session deploy-request)]

      (when (not pom-file-exists?)
        (io/delete-file pom-path))

      (deploy-result->map result))))
