(ns weavejester.build.project
  (:require [clojure.java.io :as io]
            [weavejester.build.git :as git]))

(defn- read-project []
  (read-string (slurp "project.edn")))

(def ^:private default-project
  {:src-dirs      ["src"]
   :resource-dirs ["resources"]
   :target-dir    "target"})

(defn- default-class-dir [{:keys [target-dir]}]
  (str (io/file target-dir "classes")))

(defn- default-jar-file [{:keys [lib target-dir version]}]
  (str (io/file target-dir (str lib "-" version ".jar"))))

(defn- default-uberjar-file [{:keys [lib target-dir version]}]
  (str (io/file target-dir (str lib "-" version "-uber.jar"))))

(defn- update-derived-defaults
  [{:keys [class-dir jar-file uber-file version scm] :as project}]
  (cond-> project
    (nil? version)   (assoc :version (git/default-version))
    (nil? scm)       (assoc :scm     (git/github-scm-map))
    (nil? class-dir) (as-> p (assoc p :class-dir (default-class-dir p)))
    (nil? jar-file)  (as-> p (assoc p :jar-file  (default-jar-file  p)))
    (nil? uber-file) (as-> p (assoc p :uber-file (default-uberjar-file p)))))

(def project
  (delay (->> (read-project)
              (merge default-project)
              (update-derived-defaults))))
