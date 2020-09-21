(ns hyak.core-test
  (:require
   [clojure.spec.alpha     :as s]
   [clojure.spec.gen.alpha :as gen]
   [clojure.string         :as string]
   [clojure.test           :as t :refer [deftest is]]
   [hyak.core              :as sut]))

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

(deftest percentage-of-actors-gate-test
  (let [fkey       (make-fkey "my_percent_actors_test")
        percentage 25
        low-end    20 ;; uses generator, so allow a bit of slop
        high-end   30]
    (sut/add! fs fkey)
    (sut/enable-percentage-of-actors! fs fkey percentage)
    (let [num-actors         1000
          akeys              (gen/sample (s/gen ::sut/akey) num-actors)
          test-fn            #(sut/enabled? fs fkey %)
          all-runs           (doall (map test-fn akeys))
          successful-runs    (filter true? all-runs)
          success-proportion (* 100 (/ (count successful-runs) num-actors))]
      (is (< low-end success-proportion high-end)))))

(deftest percentage-of-time-gate-test
  (let [fkey       (make-fkey "my_percent_time_test")
        percentage 25
        low-end    20 ;; uses (rand), so allow a bit of slop
        high-end   30]
    (sut/add! fs fkey)
    (sut/enable-percentage-of-time! fs fkey percentage)
    (let [run-count          1000
          test-fn            #(sut/enabled? fs fkey)
          all-runs           (take run-count (repeatedly test-fn))
          successful-runs    (filter true? all-runs)
          success-proportion (* 100 (/ (count successful-runs) run-count))]
      (is (< low-end success-proportion high-end)))))

(comment
  (require '[kaocha.repl])
  (clear-test-features! fs)
  (kaocha.repl/run)
  (kaocha.repl/run-all))
