# Hyak

```
    ~                           ~              ~
       ~~~~     ~          ~~ ~        ~      ~    ~~
  ~~             ,-''-.     ~~        .-.       ~  ~~~
            ,---':::::::\`.            \_::`.,...__    ~
     ~     |::`.:::::::::::`.       ~    )::::::.--'
           |:_:::`.::::::::::`-.__~____,'::::(
 ~~~~       \```-:::`-.o:::::::::\:::::::::~::\       ~~~
             )` `` `.::::::::::::|:~~:::::::::|      ~   ~~
 ~~        ,',' ` `` \::::::::,-/:_:::::::~~:/
         ,','/` ,' ` `\::::::|,'   `::~~::::/  ~~        ~
~       ( (  \_ __,.-' \:-:,-'.__.-':::::::'  ~    ~
    ~    \`---''   __..--' `:::~::::::_:-'
          `------''      ~~  \::~~:::'
       ~~   `--..__  ~   ~   |::_:-'                    ~~~
   ~ ~~     /:,'   `''---.,--':::\          ~~       ~~
  ~         ``           (:::::::|  ~~~            ~~    ~
~~      ~~             ~  \:~~~:::             ~       ~~~
             ~     ~~~     \:::~::          ~~~     ~
    ~~           ~~    ~~~  ::::::                     ~~
          ~~~                \::::   ~~
                       ~   ~~ `--'
```

A drop-in Clojure adaptation of [Flipper][flipper] for feature flagging.

![CI](https://github.com/rgm/hyak/workflows/CI/badge.svg)

Respects Flipper's Redis implementation, so that you can still use the existing
Ruby admin web UI and API to manage the Redis store.

[flipper]:https://github.com/jnunemaker/flipper

## Usage

```clojure
(require '[hyak.core :as hyak])

;; the Redis feature store allows the flags to be shared across multiple
;; appservers, etc.
(def fstore (hyak/make-fstore {:pool {} :spec {:url "redis://localhost:6739/1"}}
                              "feature_flippers")) ;; to match Flipper's defaults

;; fake users aka. actors for the gates below
(def enabled-actor  {:id 1000 :username "willy"})
(def disabled-actor {:id 1001 :username "shamu"})

(def MY-FEATURE-FLAG "my_great_new_feature")
(hyak/add! fstore MY-FEATURE-FLAG)

(hyak/enabled? fstore MY-FEATURE-FLAG) ;; => false

;; boolean gate
;; Feature is either on or off for all users, but checked at runtime.
(hyak/enable! fstore MY-FEATURE-FLAG)
(hyak/enabled? fstore MY-FEATURE-FLAG) ;; => true
(hyak/disable! fstore MY-FEATURE-FLAG)

;; actor gate
;; Is a actor's identifier in a set of users who get the feature? Good for
;; previewing features to known users.
(hyak/enable-actor! fstore MY-FEATURE-FLAG 40)
(hyak/enabled? fstore MY-FEATURE-FLAG (:id enabled-actor))  ;; => true
(hyak/enabled? fstore MY-FEATURE-FLAG (:id disabled-actor)) ;; => false
(hyak/disable! fstore MY-FEATURE-FLAG)

;; percentage of actors gate
;; Is the actor in a (deterministic) partition of n % of the users?
;; (we use `mod 100` on a numeric checksum of the supplied actor identifier, so an
;; actor in the enabled set will stay in the set over successive calls). Good
;; for gradually rolling out UI features.
(hyak/enable-percentage-of-actors! fstore MY-FEATURE-FLAG 50)
(hyak/enabled? fstore MY-FEATURE-FLAG (:username enabled-actor))  ;; => true
(hyak/enabled? fstore MY-FEATURE-FLAG (:username disabled-actor)) ;; => false
(hyak/disable! fstore MY-FEATURE-FLAG)

;; percentage of time gate
;; Randomly make some percentage of all requests enable the feature, without
;; regard for the actor. Good for gradually bringing up load on backend
;; features that tax the system, but where we otherwise don't care who uses it.
(hyak/enable-percentage-of-time! fstore MY-FEATURE-FLAG 10)
(let [tries 1000]
  (/ (count (filter true? (repeatedly tries #(hyak/enabled? fstore MY-FEATURE-FLAG))))
     tries)) ;; => should expect somewhere near 0.1
(hyak/disable! fstore MY-FEATURE-FLAG)
```
