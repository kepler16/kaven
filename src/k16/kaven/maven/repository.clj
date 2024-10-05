(ns k16.kaven.maven.repository
  (:import
   org.eclipse.aether.repository.Authentication
   org.eclipse.aether.repository.RemoteRepository
   org.eclipse.aether.repository.RemoteRepository$Builder
   org.eclipse.aether.util.repository.AuthenticationBuilder))

(defn create-authentication
  ^Authentication [{:keys [username password]}]
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

(defn create-repositories [settings repositories]
  (into {}
        (map (fn [[id repository]]
               [id (create-repository settings repository)]))
        repositories))
