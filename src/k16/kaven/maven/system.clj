(ns k16.kaven.maven.system
  {:no-doc true}
  (:import
   org.apache.maven.repository.internal.MavenRepositorySystemUtils
   org.eclipse.aether.RepositorySystem
   org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
   org.eclipse.aether.spi.connector.RepositoryConnectorFactory
   org.eclipse.aether.spi.connector.transport.TransporterFactory
   org.eclipse.aether.transport.file.FileTransporterFactory
   org.eclipse.aether.transport.http.HttpTransporterFactory))

(set! *warn-on-reflection* true)

(defn create-repository-system ^RepositorySystem []
  (let [locator (MavenRepositorySystemUtils/newServiceLocator)]

    (.addService locator RepositoryConnectorFactory BasicRepositoryConnectorFactory)
    (.addService locator TransporterFactory FileTransporterFactory)
    (.addService locator TransporterFactory HttpTransporterFactory)

    (.getService locator RepositorySystem)))
