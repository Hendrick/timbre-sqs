(ns hendrick.timbre.appenders.sqs-test
  (:require [clojure.test :refer :all]
            [amazonica.core :refer [with-credential]]
            [amazonica.aws.sqs :as sqs]
            [taoensso.timbre :as timbre]
            [hendrick.timbre.appenders.sqs :refer [make-sqs-appender]])
  (:import [org.elasticmq.rest.sqs SQSRestServerBuilder]))

(defn with-elasticmq
  "Fixture to run tests using an embedded ElasticMQ server"
  [tests]
  (let [server (SQSRestServerBuilder/start)]
    (.waitUntilStarted server)
    (with-credential ["sqs" "" "http://localhost:9324"]
      (tests))
    (.stopAndWait server)))

(defn is-message-received
  "Helper to assert that a message was received and matches the given pattern"
  [queue-name test-pat]
  (let [url (str "http://localhost:9324/queue/" queue-name)
        {:keys [messages]} (sqs/receive-message :queue-url url
                                                :wait-time-seconds 1)]
    (is (= 1 (count messages)))
    (is (re-find test-pat (-> messages first :body)))))

(use-fixtures :each with-elasticmq)

(deftest test-sqs
  (testing "ensure that logs get sent to SQS"
    (timbre/set-config! [:appenders :sqs]
                          (make-sqs-appender nil
                                            {:queue-name "test"
                                              :credential
                                              {:access-key "sqs"
                                               :secret-key ""
                                               :endpoint "http://localhost:9324"}}))
    (timbre/error "A test message")
    (is-message-received "test" #"A test message")))

(deftest test-config
  (testing "no messages are sent if a queue is not configured"
    (timbre/set-config! [:appenders :sqs] (make-sqs-appender))
    (timbre/error "A test message")
    (is (= 0 (-> (sqs/list-queues) :queue-urls count))))
  (comment "Not quite working yet..."
           (testing "queue name can be configured dynamically"
             (timbre/set-config! [:appenders :sqs] (make-sqs-appender))
             (timbre/set-config! [:shared-appender-config :queue-name] "test")
             (timbre/set-config! [:shared-appender-config :credential] {:access-key "sqs"
                                                                        :secret-key ""
                                                                        :endpoint "http://localhost:9324"})
             (timbre/error "A test message")
             (is-message-received "test" #"A test message"))))
