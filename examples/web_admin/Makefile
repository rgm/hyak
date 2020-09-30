all: server

server: Gemfile.lock
	bundle exec rackup config.ru -p 9292

Gemfile.lock: Gemfile
	bundle

# vi:ft=make:
