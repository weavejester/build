(ns weavejester.build.tasks
  (:refer-clojure :exclude [test])
  (:require [babashka.fs :as fs]
            [babashka.pods :as pods]
            [babashka.tasks :as bb]
            [clojure.edn :as edn]))

(def ^:private project-config
  (delay (when (fs/exists? "project.edn")
           (edn/read-string (slurp "project.edn")))))

(defn- run-clojure [deps ns args]
  (apply bb/clojure "-Sdeps" (pr-str {:deps deps}) "-M" "-m" (str ns) args))

(defn clean
  "Remove the target folder"
  []
  (fs/delete-tree (:target-dir @project-config "target")))

(defn jar
  "Create a jar file from the project"
  []
  (bb/clojure "-T:build" "jar"))

(defn lint
  "Lint the source files"
  []
  (pods/load-pod "clj-kondo")
  (require 'pod.borkdude.clj-kondo)
  (let [lint-fn  (resolve 'pod.borkdude.clj-kondo/run!)
        print-fn (resolve 'pod.borkdude.clj-kondo/print!)
        results  (let [src (:src-dirs @project-config ["src"])]
                  (-> (lint-fn {:lint src})
                      (doto print-fn)))]
    (when (-> results :findings seq)
      (throw (ex-info "Lint warnings found, exiting with status code 1" {:babashka/exit 1})))))

(defn outdated
  "Find outdated dependencies"
  [& args]
  (run-clojure '{com.github.liquidz/antq {:mvn/version "1.3.1"}}
               'antq.core args))

(defn repl
  "Start a REPL for the project"
  [& args]
  (run-clojure '{com.bhauman/rebel-readline {:mvn/version "0.1.4"}}
               'rebel-readline.main args))

(defn test
  "Run tests for the project"
  [& args]
  (run-clojure '{lambdaisland/kaocha {:mvn/version "1.60.945"}}
               'kaocha.runner args))

(defn uberjar
  "Create an uberjar with the project and dependencies"
  []
  (bb/clojure "-T:build" "uberjar"))
