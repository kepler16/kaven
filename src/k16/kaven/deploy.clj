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

(defn deploy
  "Deploy a jar found at `:jar-path` to a maven `:repository`.

  Accepts the following opts:

  + `:jar-path` - A path on disk to the jar file to be deployed. Required.
  + `:pom-path` - An optional path on disk to the POM definition of the maven
  module. This is extracted from the jar at `:jar-path` if not provided.
  + `:repository` - Either a repository id as a string or a full repository
  configuration map. Required.
  + `:repositories` - A map of repository id (string) to a repository
  configuration map.
  + `:lib` - An optional fully-qualified symbol describing the lib given in the
  form <group-id>/<artifact-id>. This is inferred from the POM file if not
  provided.
  + `version` - An option artifact version. This is inferred from the POM file
  if not provided.

  Requires, at minimum, a `:jar-path` and a `:repository` to be set.

  ### POM File Discovery

  If no `:pom-path` is configured, or the `:pom-path` does not point to a valid
  file on disk, then this fn will search inside the jar at `:jar-path` for a POM
  file. This works by using the first result returned by the following search:

  1) If `:pom-path` is provided but is not a file on disk then we try find a
  resource within the jar at this path.
  2) If `:lib` is provided then we try find a POM file at
  'zip://<jar-path>/META-INF/maven/<group-id>/<artifact-id>/pom.xml'
  3) The jar resources are scanned for the first pom file in
  'zip://<jar-path>/META-INF/maven/.*/pom.xml'

  ### Repository Configuration

  A set of repository configurations are constructed by analysing the POM file
  and extracting the <repositories> section.

  Credentials from `~/.m2/settings.xml` are then loaded and merged with
  repositories found in the POM file. This is done by referencing the
  repositories `:id` field.

  Repositories can configured/overriden through the `:repositories` opt.
  Anything specified here will be merged with automatically discovered
  repositories.

  The target repository to deploy to is specified by setting the `:repository`
  opt to the desired repository ID or by providing the full repository config.

  ### Examples

  If you have a `username/password` for clojars configured in `settings.xml`
  then you only need to specify the repository id.
  ```clojure
  (deploy {:jar-path \"target/lib.jar\"
           :repository \"clojars\"})
  ```

  Explicitly specifying the repository credentials
  ```clojure
  (deploy {:jar-path \"target/lib.jar\"
           :repository \"clojars\"
           ;; The clojars repository URL is typically always available
           :repositories {\"clojars\" {:credentials {:username \"username\"
                                                     :password \"password\"}}}})
  ```

  Deploy to a github repository, specifying the repository config directly.
  ```clojure
  (deploy {:jar-path \"target/lib.jar\"
           :repository {:id \"github\"
                        :url \"https://maven.pkg.github.com/<github-org>/<github-repo>\"
                        :credentials {:username (System/getenv \"GITHUB_USERNAME\")
                                      :password (System/getenv \"GITHUB_TOKEN\")}}})
  ```

  Override the `:lib` and `:version` used.
  ```clojure
  (deploy {:jar-path \"target/lib.jar\"
           :repository \"clojars\"
           :lib 'com.kepler16/kaven-2
           :version \"0.0.1-SNAPSHOT\"})
  ```

  Specify the path to the pom file explicitly
  ```clojure
  (deploy {:jar-path \"target/lib.jar\"
           :pom-path \"target/META-INF/maven/com.kepler16/kaven/pom.xml\"
           :repository \"clojars\"})
  ```"
  [{:keys [jar-path pom-path
           repository repositories
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

        repository (if (string? repository)
                     (get repositories repository)
                     (if (:id repository)
                       (meta-merge/meta-merge (get repositories (:id repository))
                                              repository)
                       repository))

        repository (maven.repository/create-repository
                    settings repository)

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
