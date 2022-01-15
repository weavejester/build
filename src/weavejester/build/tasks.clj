(ns weavejester.build.tasks
  (:refer-clojure :exclude [test])
  (:require [babashka.fs :as fs]
            [babashka.pods :as pods]
            [babashka.tasks :as bb]
            [weavejester.build.project :as p]))

(defn- run-clojure [deps ns args]
  (apply bb/clojure "-Sdeps" (pr-str {:deps deps}) "-M" "-m" (str ns) args))

(defn clean
  "Remove the target folder"
  []
  (fs/delete-tree (:target-dir @p/project)))

(defn jar
  "Create a jar file from the project"
  []
  (bb/clojure "-Tbuild" "jar"))

(defn lint
  "Lint the source files"
  []
  (pods/load-pod "clj-kondo")
  (require 'pod.borkdude.clj-kondo)
  (let [lint-fn  (resolve 'pod.borkdude.clj-kondo/run!)
        print-fn (resolve 'pod.borkdude.clj-kondo/print!)
        results  (let [src (:src-dirs @p/project)]
                  (-> (lint-fn {:lint src})
                      (doto print-fn)))]
    (when (-> results :findings seq)
      (throw (ex-info "Lint warnings found, exiting with status code 1"
                      {:babashka/exit 1})))))

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
  (bb/clojure "-Tbuild" "uberjar"))

(defn deploy
  "Deploy an uberjar to Clojars"
  []
  (bb/clojure "-Tbuild" "deploy"))
