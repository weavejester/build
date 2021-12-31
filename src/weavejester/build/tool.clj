(ns weavejester.build.tool
  (:require [clojure.tools.build.api :as b]
            [weavejester.build.project :as p]
            [weavejester.build.write-pom :as pom]))

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
