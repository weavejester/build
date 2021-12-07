(ns weavejester.build
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [weavejester.build.git :as git]
            [weavejester.build.write-pom :as pom]))

(defn- read-project []
  (read-string (slurp "project.edn")))

(def ^:private default-project
  {:src-dirs   ["src" "resources"]
   :target-dir "target"})

(defn- default-class-dir [{:keys [target-dir]}]
  (str (io/file target-dir "classes")))

(defn- default-jar-file [{:keys [lib target-dir version]}]
  (str (io/file target-dir (str lib "-" version ".jar"))))

(defn- default-uberjar-file [{:keys [lib target-dir version]}]
  (str (io/file target-dir (str lib "-" version "-uber.jar"))))

(defn- update-derived-defaults
  [{:keys [class-dir jar-file uber-file target-dir version scm] :as project}]
  (cond-> project
    (nil? version)   (assoc :version (git/default-version))
    (nil? scm)       (assoc :scm     (git/github-scm-map))
    (nil? class-dir) (as-> p (assoc p :class-dir (default-class-dir p)))
    (nil? jar-file)  (as-> p (assoc p :jar-file  (default-jar-file  p)))
    (nil? uber-file) (as-> p (assoc p :uber-file (default-uberjar-file p)))))

(def ^:private project
  (delay (->> (read-project)
              (merge default-project)
              (update-derived-defaults))))

(defn jar [_]
  (doto (assoc @project :basis (b/create-basis))
    (pom/write-pom)
    (b/copy-dir)
    (b/jar)))

(defn uberjar [_]
  (doto (assoc @project :basis (b/create-basis))
    (pom/write-pom)
    (b/copy-dir)
    (b/compile-clj)
    (b/uber)))
