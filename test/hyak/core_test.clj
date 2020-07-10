(ns hyak.core-test
  (:require
   [clojure.string :as string]
   [clojure.test   :refer :all]
   [hyak.core      :as sut]))

;; prefix all features so we can clean these up in redis
(let [PREFIX "TEST_FEATURE_"]
  (defn test-feature? [s] (string/starts-with? s PREFIX))
  (defn make-fkey [s] (str PREFIX s)))

;; gate tests

(deftest boolean-gate-test
  (let [fs   (sut/make-fstore)
        fkey (make-fkey "my_bool_feature")]
    (sut/add! fs fkey)
    (is (sut/disabled? fs fkey))
    (sut/enable! fs fkey)
    (is (sut/enabled? fs fkey))
    (sut/disable! fs fkey)
    (is (sut/disabled? fs fkey))))
