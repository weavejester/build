(ns weavejester.build
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [weavejester.write-pom :as pom]))

(defn- read-project []
  (read-string (slurp "project.edn")))

(def ^:private default-project
  {:src-dirs   ["src" "resources"]
   :target-dir "target"})

(defn- git [& args]
  (b/process {:command-args (into ["git"] args)
              :out :capture
              :err :ignore}))

(defn- default-version []
  (some-> (git "describe" "--exact-match" "--abbrev=0") :out str/trim))

(defn- default-class-dir [{:keys [target-dir]}]
  (str (io/file target-dir "classes")))

(defn- default-jar-file [{:keys [lib target-dir version]}]
  (str (io/file target-dir (str lib "-" version ".jar"))))

(defn- update-derived-defaults
  [{:keys [class-dir jar-file target-dir version] :as project}]
  (cond-> project
    (nil? version)   (assoc :version (default-version))
    (nil? class-dir) (as-> p (assoc p :class-dir (default-class-dir p)))
    (nil? jar-file)  (as-> p (assoc p :jar-file  (default-jar-file  p)))))

(def ^:private project
  (delay (->> (read-project)
              (merge default-project)
              (update-derived-defaults))))

(defn jar [_]
  (doto (assoc @project :basis (b/create-basis))
    (pom/write-pom)
    (b/copy-dir)
    (b/jar)))
