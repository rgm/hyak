REDIS_URL ?= redis://localhost:6379/1

# run test suite
test:
	REDIS_URL=$(REDIS_URL) clojure -A:test -m kaocha.runner
.PHONY: test

# start a clojure repl with nrepl and local redis
repl:
	REDIS_URL=$(REDIS_URL) clj -A:test -Sdeps '{:deps {nrepl {:mvn/version "RELEASE"} cider/cider-nrepl {:mvn/version "RELEASE"} cider/piggieback {:mvn/version "RELEASE"}}}' -m nrepl.cmdline --middleware '[cider.nrepl/cider-middleware,cider.piggieback/wrap-cljs-repl]' --interactive --color

# fix formatting
cljfmt:
	clojure -Sdeps '{:deps {cljfmt {:mvn/version "RELEASE"}}}' -m cljfmt.main fix
