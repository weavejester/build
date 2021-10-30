(ns build
  (:require [org.corfield.build :as bb]
            [tools-pom.tasks :as pom]
            [weavejester.build :as wb]))

(defn pom [_]
  (pom/pom {:lib 'weavejester/build
            :version (wb/git-head-tag)
            :write-pom true
            :validate-pom true
            :pom {:description "testing"}}))
