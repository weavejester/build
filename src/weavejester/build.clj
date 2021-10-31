(ns weavejester.build
  (:require [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [tools-pom.tasks :as pom]))

(defn- git [& args]
  (b/process {:command-args (into ["git"] args)}))

(defn- git-head-tag []
  (some-> (git "describe" "--exact-match" "--abbrev=0") :out str/trim))

(defn- read-project []
  (read-string (slurp "project.edn")))

(def ^:private project
  (delay (read-project)))

(defn pom [_]
  (let [{:keys [lib version] :as opts} @project]
    (pom/pom {:lib          lib
              :version      version
              :pom          (dissoc opts :lib :version)
              :write-pom    true
              :validate-pom true})))
