(ns weavejester.build-test
  (:require [clojure.test :refer [deftest is]]
            [weavejester.build :as wb]))

(deftest success-test
  (is (= 1 1)))
