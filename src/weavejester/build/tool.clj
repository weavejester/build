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

(defn- replace-bbedn-version [s version]
  (str/replace s "VERSION" (pr-str version)))

(defn- write-bbedn []
  (if (.exists (io/file b/*project-root* "bb.edn"))
    (println "Skipping bb.edn as it already exists")
    (let [bbedn (-> (io/resource "weavejester/build/bb.edn.tmpl")
                    (slurp)
                    (replace-bbedn-version (find-self-version)))]
      (b/write-file {:path "bb.edn" :string bbedn})
      (println "Written bb.edn"))))

(defn init [_]
  (write-bbedn))

(defn jar [_]
  (doto (assoc @p/project :basis (b/create-basis))
    (pom/write-pom)
    (b/copy-dir)
    (b/jar)))

(defn uberjar [_]
  (doto (assoc @p/project :basis (b/create-basis))
    (pom/write-pom)
    (b/copy-dir)
    (b/compile-clj)
    (b/uber)))
