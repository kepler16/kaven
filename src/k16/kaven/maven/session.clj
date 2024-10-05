(ns k16.kaven.maven.session
  (:import
   org.eclipse.aether.RepositorySystem
   org.eclipse.aether.RepositorySystemSession
   )
  (:require
   [clojure.java.io :as io]))

(defn create-system-session
  (^RepositorySystemSession [^RepositorySystem system]
   (create-system-session system {}))

  (^RepositorySystemSession [^RepositorySystem system opts]
   (let [local-repo-path (or (:local-repo-path opts)
                             (str (System/getProperty "user.home")
                                  "/.m2/repository"))
         session-builder (.createSessionBuilder system)]

     (-> session-builder
         (.withLocalRepositoryBaseDirectories [(.toPath (io/file local-repo-path))])

         #_(.setConfigProperty "aether.generator.gpg.enabled" (.toString Boolean/FALSE))
         #_(.setConfigProperty "aether.generator.gpg.keyFilePath"
                               (-> (Paths/get "src" (into-array ["main" "resources" "alice.key"]))
                                   (.toAbsolutePath)
                                   (.toString)))
         #_(.setConfigProperty "aether.syncContext.named.factory" "noop"))

     (.build session-builder))))
