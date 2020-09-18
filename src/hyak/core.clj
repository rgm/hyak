(ns hyak.core
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string     :as string]
   [taoensso.carmine   :as car :refer [wcar]]))

;; DRAMATIS PERSONAE:
;; fstore .................... Feature Store
;; fkey ...................... Feature Key, a string
;; akey ...................... Actor Key, a string
;; gkey ...................... Group Key, a string

(s/def ::fstore (s/keys :req [::root-key ::conn]))
(s/def ::root-key string?)
(s/def ::conn (s/keys :req-un [::pool ::spec])) ;; carmine spec

(defn make-fstore
  "Make a flipper store. See `taoensso.carmine/wcar` for documentation of
   `conn-opts`."
  ([]
   (let [redis-url    (or (clojure.core/get (System/getenv) "REDIS_URL")
                          "redis://127.0.0.1:6379/0")
         conn-opts    {:pool {} :spec {:uri redis-url}}
         features-key "flipper_features"]
     (make-fstore conn-opts features-key)))
  ([conn-opts features-key]
   {::conn conn-opts
    ::root-key features-key}))

(defn features [{::keys [root-key conn]}]
  (set (wcar conn (car/smembers root-key))))

(defn exists? [fstore fkey]
  (contains? (features fstore) fkey))

(defn add! [{::keys [root-key conn]} fkey]
  (wcar conn (car/sadd root-key fkey)))

(defn clear! [{::keys [conn]} fkey]
  (wcar conn (car/del fkey)))

(defn remove! [{::keys [root-key conn] :as fstore} fkey]
  (clear! fstore fkey)
  (wcar conn (car/srem root-key fkey)))

;; * boolean gates

(defn enable! [{::keys [conn] :as fstore} fkey]
  (clear! fstore fkey)
  (wcar conn (car/hset fkey "boolean" "true")))

(def disable! clear!)

(defn boolean-gate-open? [gate-values _akey]
  (= (get gate-values "boolean") "true"))

;; actor gates

(defn akey->str [akey] (str "actors/" akey))

(defn str->akey [s] (string/replace s #"^actors/" ""))

(defn enable-actor! [{::keys [conn]} fkey akey]
  (wcar conn (car/hset fkey (akey->str akey) "1")))

(defn disable-actor! [{::keys [conn]} fkey akey]
  (wcar conn (car/hdel fkey (akey->str akey))))

(defn actor-gate-open? [gate-values akey]
  (= (get gate-values (akey->str akey)) "1"))

;; group gates

(defonce *group-registry (atom {}))

(defn gkey->str [gkey] (str "groups/" gkey))

(defn str->gkey [s] (string/replace s #"^groups/" ""))

(defn register-group [gkey pred]
  [{:pre [(ifn? pred)]}]
  (swap! *group-registry assoc (gkey->str gkey) pred))

(defn unregister-groups []
  (reset! *group-registry {}))

(defn group-names []
  (into #{} (map str->gkey (keys @*group-registry))))

(defn group-preds []
  (vals @*group-registry))

(defn group-exists? [gkey]
  (contains? @*group-registry (gkey->str gkey)))

(defn enable-group! [{::keys [conn]} fkey gkey]
  (wcar conn (car/hset fkey (gkey->str gkey) 1)))

(defn disable-group! [{::keys [conn]} fkey gkey]
  (wcar conn (car/hdel fkey (gkey->str gkey))))

(defn group-gate-open? [gate-values akey]
  (let [active? (fn [[k v]] (and (string/starts-with? k "groups/")
                                 (= v "1")))
        gkeys   (into [] (comp (filter active?) (map key)) gate-values)
        preds   (vals (select-keys @*group-registry gkeys))
        test-fn (apply some-fn preds)]
    (test-fn akey)))

;; percentage of actors gates

(defn enable-percentage-of-actors! [{::keys [conn]} fkey n]
  {:pre [(integer? n) (<= 0 n 100)]}
  (wcar conn (car/hset fkey "percentage_of_actors" (str n))))

(defn disable-percentage-of-actors! [{::keys [conn]} fkey]
  (wcar conn (car/hdel fkey "percentage_of_actors")))

(defn akey->n [akey]
  (let [crc (doto (new java.util.zip.CRC32)
              (.update (.getBytes akey)))]
    (.getValue crc)))

(defn percentage-of-actors-gate-open? [gate-values akey]
  (when-let [percent (get gate-values "percentage_of_actors")]
    (let [scaling-factor 1000 ;; gives us a few more decimal places
          n (mod (akey->n akey) (* 100 scaling-factor))]
      (< n (* (Integer/parseInt percent) scaling-factor)))))

;; percentage of time gate

(defn enable-percentage-of-time! [{::keys [conn]} fkey n]
  {:pre [(integer? n) (<= 0 n 100)]}
  (wcar conn (car/hset fkey "percentage_of_time" (str n))))

(defn disable-percentage-of-time! [{::keys [conn]} fkey]
  (wcar conn (car/hdel fkey "percentage_of_time")))

(defn percentage-of-time-gate-open? [gate-values _akey]
  (when-let [percent (get gate-values "percentage_of_time")]
    (< (rand) (/ (Integer/parseInt percent) 100.0))))

;; enabled? / disabled?

(defn get-feature [conn fkey]
  (apply hash-map (wcar conn (car/hgetall fkey))))

;; some-fn short-circuits so leave expensive checks for last
(def active-gates [boolean-gate-open?
                   actor-gate-open?
                   percentage-of-actors-gate-open?
                   percentage-of-time-gate-open?
                   #_group-gate-open?])

(defn enabled?
  ([fstore fkey] (enabled? fstore fkey nil))
  ([{::keys [conn]} fkey akey]
   (let [gate-values (get-feature conn fkey)
         preds       (map #(partial % gate-values) active-gates)
         any?        (apply some-fn preds)]
     (any? akey))))

(def disabled? (complement enabled?))
