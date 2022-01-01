(ns weavejester.build.tool
  (:require [clojure.tools.build.api :as b]
            [weavejester.build.project :as p]
            [weavejester.build.write-pom :as pom]
            [clojure.java.io :as io]))

(defn init [_]
  (let [src  (io/resource "weavejester/build/bb.edn")
        dest (io/file "bb.edn")]
    (when-not (.exists dest)
      (with-open [in (io/input-stream src)]
        (io/copy in dest)))))

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
