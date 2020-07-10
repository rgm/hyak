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
