(defproject com.hendrick/timbre-sqs "0.1.0"
  :description "A Timbre appender that sends log messages to Amazon SQS"
  :url "https://github.com/Hendrick/timbre-sqs"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [amazonica "0.3.28"]
                 [com.taoensso/timbre "4.0.2" :exclusions [org.clojure/tools.reader]]]
  :profiles
            {:dev
             {:dependencies [[org.elasticmq/elasticmq-server_2.11 "0.8.8" :exclusions [joda-time]]]}})
