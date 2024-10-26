(ns k16.kaven.maven.settings
  {:no-doc true}
  (:require
   [clojure.java.io :as io])
  (:import
   org.apache.maven.settings.Server
   org.apache.maven.settings.Settings
   org.apache.maven.settings.io.xpp3.SettingsXpp3Reader))

(set! *warn-on-reflection* true)

(defn- server-credentials
  [^Server server]
  (let [id (.getId server)
        username (.getUsername server)]
    {id {:id id
         :username username
         :password (.getPassword server)}}))

(defn- servers-with-passwords
  [^Settings settings]
  (let [servers (.getServers settings)]
    (into {} (map server-credentials) servers)))

(defn read-settings
  (^Settings [] (read-settings nil))
  (^Settings [settings-path]
   (let [default-settings-path (str (System/getProperty "user.home")
                                    "/.m2/settings.xml")
         settings-path (or settings-path default-settings-path)
         sr (SettingsXpp3Reader.)]
     (with-open [rdr (io/reader (io/as-file settings-path))]
       (let [settings (.read sr rdr)]
         {:servers (servers-with-passwords settings)})))))
