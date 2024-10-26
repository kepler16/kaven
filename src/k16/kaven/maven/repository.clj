(ns k16.kaven.maven.repository
  {:no-doc true}
  (:import
   org.eclipse.aether.repository.Authentication
   org.eclipse.aether.repository.RemoteRepository
   org.eclipse.aether.repository.RemoteRepository$Builder
   org.eclipse.aether.util.repository.AuthenticationBuilder))

(set! *warn-on-reflection* true)

(defn create-authentication
  ^Authentication [{:keys [^String username ^String password]}]
  (-> (AuthenticationBuilder.)
      (.addUsername username)
      (.addPassword password)
      .build))

(defn create-repository
  ^RemoteRepository [settings repository]
  (let [{:keys [id url credentials]} repository

        credentials (or credentials
                        (get-in settings [:servers id]))

        repo (RemoteRepository$Builder. id "default" url)]

    (when credentials
      (.setAuthentication
       repo (create-authentication credentials)))

    (.build repo)))
