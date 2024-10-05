(ns k16.kaven.maven.session
  (:import
   org.apache.maven.repository.internal.MavenRepositorySystemUtils
   org.eclipse.aether.repository.LocalRepository
   org.eclipse.aether.RepositorySystem
   org.eclipse.aether.RepositorySystemSession))

(set! *warn-on-reflection* true)

(defn create-system-session
  (^RepositorySystemSession [^RepositorySystem system]
   (create-system-session system {}))

  (^RepositorySystemSession [^RepositorySystem system opts]
   (let [local-repo-path (or (:local-repo-path opts)
                             (str (System/getProperty "user.home")
                                  "/.m2/repository"))

         local-repo (LocalRepository. ^String local-repo-path)

         session (MavenRepositorySystemUtils/newSession)]

     (.setLocalRepositoryManager session (.newLocalRepositoryManager system session local-repo))
     session)))
