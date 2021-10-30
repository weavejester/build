(ns weavejester.build
  (:require [clojure.string :as str]
            [clojure.tools.build.api :as b]))

(defn- git [& args]
  (b/process {:command-args (into ["git"] args)}))

(defn git-head-tag []
  (some-> (git "describe" "--exact-match" "--abbrev=0") :out str/trim))
