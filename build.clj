(ns build
  (:require [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]
            [tools-pom.tasks :as pom]))

(defn- git [& args]
  (b/process {:command-args (into ["git"] args)}))

(defn git-head-tag []
  (some-> (git "describe" "--exact-match" "--abbrev=0") :out str/trim))

(defn pom [_]
  (pom/pom {:lib 'weavejester/build
            :version (git-head-tag)
            :write-pom true
            :validate-pom true
            :pom {:description "testing"}}))
