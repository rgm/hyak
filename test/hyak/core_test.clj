(ns hyak.core-test
  (:require
   [clojure.test                  :as t :refer [deftest is]]
   [clojure.test.check.generators :as gen]
   [hyak.core                     :as sut]))

(let [PREFIX "TEST_FEATURE_"]
  ;; prefix all features so we can clean up strays in redis
  (defn make-fkey [s] (str PREFIX s)))

(def fstore (sut/make-fstore {:pool {}
                              :spec {:uri (System/getenv "REDIS_URL")}}
                             "TEST_FEATURES"))

(defn reset-fstore [f]
  (f)
  (sut/reset-fstore! fstore))

(t/use-fixtures :each reset-fstore)

;; gate tests

(deftest boolean-gate-test
  (let [fkey (make-fkey "my_bool_feature")]
    (sut/add! fstore fkey)
    (is (sut/disabled? fstore fkey))
    (sut/enable! fstore fkey)
    (is (sut/enabled? fstore fkey))
    (is (sut/enabled? fstore fkey nil)) ;; should be true with a nil actor
    (sut/disable! fstore fkey)
    (is (sut/disabled? fstore fkey))))

(deftest actor-gate-test
  (let [fkey (make-fkey "my_actor_feature")
        akey :enabled-actor]
    (sut/add! fstore fkey)
    (is (sut/disabled? fstore fkey akey))
    (is (sut/disabled? fstore fkey :some-other-actor))
    (sut/enable-actor! fstore fkey akey)
    (is (sut/enabled? fstore fkey akey))
    (is (sut/disabled? fstore fkey :some-other-actor))
    (sut/disable-actor! fstore fkey akey)
    (is (sut/disabled? fstore fkey akey))
    (is (sut/disabled? fstore fkey :some-other-actor))))

(defn epsilon= [eps a b] (< (abs (- a b)) eps))

(deftest percentage-of-actors-gate-test
  (let [n         1000
        pct       46
        fkey      (make-fkey "percent_actors_feature")
        akeys     (gen/sample gen/string n)
        n-enabled (fn [akeys] (->> akeys
                                   (filter #(sut/enabled? fstore fkey %))
                                   count))]
    (is (= 0 (n-enabled akeys)))
    (sut/enable-percentage-of-actors! fstore fkey pct)
    (let [eps (* n 0.05)]
      (is (epsilon= eps (* (/ pct 100) n) (n-enabled akeys))))
    (sut/disable! fstore fkey)
    (is (= 0 (n-enabled akeys)))))

(deftest percentage-of-time-gate-test
  (let [n     1000
        pct   57
        fkey  (make-fkey "percent_time_feature")
        akey  (gen/generate gen/string)
        n-enabled (fn [akey] (->> (repeat n akey)
                                  (filter #(sut/enabled? fstore fkey %))
                                  count))]
    (is (sut/disabled? fstore fkey))
    (sut/enable-percentage-of-time! fstore fkey pct)
    (let [eps (* n 0.05)]
      (is (epsilon= eps (* (/ pct 100) n) (n-enabled akey))))
    (sut/disable! fstore fkey)
    (is (sut/disabled? fstore fkey))))

(deftest group-test
  (let [admin-akeys    (gen/sample gen/string 5)
        non-admin-akey (gen/sample gen/string 5)
        admin?         (set admin-akeys)
        fkey           (make-fkey "group_feature")]
    (sut/register-group! :admins admin?)
    (doseq [admin-akey admin-akeys] (is (sut/disabled? fstore fkey admin-akey)))
    (is (sut/disabled? fstore fkey non-admin-akey))
    (sut/enable-group! fstore fkey :admins)
    (doseq [admin-akey admin-akeys] (is (sut/enabled? fstore fkey admin-akey)))
    (is (sut/disabled? fstore fkey non-admin-akey))
    (sut/disable-group! fstore fkey :admins)
    (doseq [admin-akey admin-akeys] (is (sut/disabled? fstore fkey admin-akey)))
    (is (sut/disabled? fstore fkey non-admin-akey))
    (sut/unregister-groups!)))
