# frozen_string_literal: true

require 'flipper-ui'
require 'flipper/adapters/redis'

Flipper.register(:early_access) # register up an early_access group in the UI

Flipper::UI.configure do |config|
  config.banner_text = 'Production Environment'
  config.banner_class = 'danger'
  config.fun = true
end

options = {}

options[:url] = ENV['REDIS_URL']
options[:password] = ENV['REDIS_PASSWORD'] if ENV['REDIS_PASSWORD']
client = Redis.new options
adapter = Flipper::Adapters::Redis.new client

flipper = Flipper.new(adapter)
run Flipper::UI.app(flipper) do |builder|
  builder.use Rack::Session::Cookie, secret: 'something long and random'
end
