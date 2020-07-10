(ns hyak.core-test
  (:require
   [clojure.string :as string]
   [clojure.test   :as t :refer [deftest is]]
   [hyak.core      :as sut]))

(def fs (sut/make-fstore))

;; prefix all features so we can clean these up in redis
(let [PREFIX "TEST_FEATURE_"]
  (defn make-fkey [s] (str PREFIX s))
  (defn clear-test-features! [fstore]
    (let [test-feature? #(string/starts-with? % PREFIX)]
    (doseq [fkey (filter test-feature? (sut/features fstore))]
      (sut/remove! fstore fkey)))))

(defn fixture [test-fn]
  (clear-test-features! fs)
  (test-fn))

(t/use-fixtures :each fixture)

;; gate tests

(deftest boolean-gate-test
  (let [fkey (make-fkey "my_bool_feature")]
    (sut/add! fs fkey)
    (is (sut/disabled? fs fkey))
    (sut/enable! fs fkey)
    (is (sut/enabled? fs fkey))
    (sut/disable! fs fkey)
    (is (sut/disabled? fs fkey))))

(comment
  (require '[kaocha.repl])
  (clear-test-features! fs)
  (kaocha.repl/run *ns*))
