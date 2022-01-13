(ns weavejester.build.tool
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [weavejester.build.project :as p]
            [weavejester.build.write-pom :as pom]))

(defn- read-deps []
  (edn/read-string (slurp (io/file b/*project-root* "deps.edn"))))

(defn- find-self-version []
  (-> (read-deps) :aliases :build :deps
      (get 'dev.weavejester/build {:local/root "."})))

(defn- replace-template-vars [text vars]
  (reduce-kv (fn [s k v] (str/replace s k v)) text vars))

(defn- write-template [src dest vars]
  (if (.exists (io/file b/*project-root* dest))
    (println "Skipping" dest "as it already exists")
    (let [text (-> src io/resource slurp (replace-template-vars vars))]
      (b/write-file {:path dest, :string text})
      (println "Written" dest))))

(defn- write-bb-edn []
  (write-template "weavejester/build/bb.edn.tmpl" "bb.edn"
                  {"VERSION" (pr-str (find-self-version))}))

(defn- write-project-edn []
  (write-template "weavejester/build/project.edn.tmpl" "project.edn"
                  {"LIBRARY_NAME" "test"}))

(defn- write-tests-edn []
  (if (.exists (io/file b/*project-root* "tests.edn"))
    (println "Skipping tests.edn as it already exists")
    (let [text (slurp (io/resource "weavejester/build/tests.edn"))]
      (b/write-file {:path "tests.edn", :string text})
      (println "Written tests.edn"))))

(defn init [_]
  (write-bb-edn)
  (write-project-edn)
  (write-tests-edn))

(defn- copy-to-target [{:keys [src-dirs resource-dirs class-dir]}]
  (b/copy-dir {:src-dirs   (into src-dirs resource-dirs)
               :target-dir class-dir}))

(defn jar [_]
  (doto (assoc @p/project :basis (b/create-basis))
    (pom/write-pom)
    (copy-to-target)
    (b/jar)))

(defn uberjar [_]
  (doto (assoc @p/project :basis (b/create-basis))
    (pom/write-pom)
    (copy-to-target)
    (b/compile-clj)
    (b/uber)))

(defn evalstr [{:keys [sexp]}]
  (eval sexp))
