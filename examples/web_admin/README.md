# Web admin example

Show how to use jnunemaker/flipper infrastructure to manage the flippers.

## To use

1. Make sure you have Ruby + bundler installed.

2. Set REDIS_URL (and REDIS_PASSWORD, which you really should be using but I
   guess it's OK if you're just trying this out on your own laptop) in your
   shell environment.

3. Issue `make` at the shell to stand up a Rack-based web UI.

4. Go to `http://127.0.0.1:9292` to see the web UI.
