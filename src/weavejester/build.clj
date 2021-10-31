(ns weavejester.build
  (:require [clojure.string :as str]
            [clojure.tools.build.api :as b]))

(defn- git [& args]
  (b/process {:command-args (into ["git"] args)}))

(defn- git-head-tag []
  (some-> (git "describe" "--exact-match" "--abbrev=0") :out str/trim))

(defn- read-project []
  (read-string (slurp "project.edn")))

(def ^:private default-project
  {:class-dir  "target/classes"
   :src-dirs   ["src" "resources"]
   :target-dir "target"})

(def ^:private project
  (delay (merge default-project (read-project))))

(defn jar [_]
  (doto (assoc @project :basis (b/create-basis))
    (b/write-pom)
    (b/copy-dir)
    (b/jar)))
