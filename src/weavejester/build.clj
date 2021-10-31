(ns weavejester.build
  (:require [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [tools-pom.tasks :as pom]))

(defn- git [& args]
  (b/process {:command-args (into ["git"] args)}))

(defn git-head-tag []
  (some-> (git "describe" "--exact-match" "--abbrev=0") :out str/trim))

(defn pom [{:keys [lib version] :as opts}]
  (pom/pom {:lib          lib
            :version      version
            :pom          (dissoc opts :lib :version)
            :write-pom    true
            :validate-pom true}))
