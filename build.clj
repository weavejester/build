(ns build
  (:require [weavejester.build :as wb]))

(defn pom [_]
  (wb/pom {:lib 'weavejester/build
           :version (wb/git-head-tag)
           :description "testing"}))
