(ns weavejester.git
  (:require [clojure.string :as str]
            [clojure.tools.build.api :as b]))

(defn- git [& args]
  (b/process {:command-args (into ["git"] args)
              :out :capture
              :err :ignore}))

(defn default-version []
  (some-> (git "describe" "--exact-match" "--abbrev=0") :out str/trim))
