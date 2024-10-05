(ns k16.kaven.maven.system
  (:import
   ; [org.eclipse.aether.generator.gnupg GnupgSignatureArtifactGeneratorFactory]
   ; [org.eclipse.aether.generator.gnupg.loaders GpgAgentPasswordLoader GpgConfLoader GpgEnvLoader]
   ; [org.eclipse.aether.spi.artifact.decorator ArtifactDecorator ArtifactDecoratorFactory]
   [org.eclipse.aether.supplier RepositorySystemSupplier]
   ; [org.eclipse.aether.transport.jdk JdkTransporterFactory]
   ; [org.eclipse.aether.transport.jetty JettyTransporterFactory]
   )
  (:require [clojure.java.io :as io]))

#_(defn- create-gnupg-signature-artifact-generator-factory-loaders []
  ;; order matters
  (doto (java.util.LinkedHashMap.)
    (.put GpgEnvLoader/NAME (GpgEnvLoader.))
    (.put GpgConfLoader/NAME (GpgConfLoader.))
    (.put GpgAgentPasswordLoader/NAME (GpgAgentPasswordLoader.))))

(defn create-repository-system []
  (let [supplier
        (proxy [RepositorySystemSupplier] []
 #_         (createArtifactGeneratorFactories []
            (let [result (proxy-super createArtifactGeneratorFactories)]
              #_(.put result
                      GnupgSignatureArtifactGeneratorFactory/NAME
                      (GnupgSignatureArtifactGeneratorFactory.
                       (.getArtifactPredicateFactory this)
                       (create-gnupg-signature-artifact-generator-factory-loaders)))

              result))

   #_       (createArtifactDecoratorFactories []
            (let [result (proxy-super createArtifactDecoratorFactories)]

              result))

     #_     (createTransporterFactories []
            (let [result (proxy-super createTransporterFactories)]
              result)))]

    (.get supplier)))
