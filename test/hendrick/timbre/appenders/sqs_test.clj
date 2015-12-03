(ns hendrick.timbre.appenders.sqs-test
  (:require [clojure.test :refer :all]
            [amazonica.core :refer [with-credential]]
            [amazonica.aws.sqs :as sqs]
            [taoensso.timbre :as timbre]
            [hendrick.timbre.appenders.sqs :refer [sqs-appender]])
  (:import [org.elasticmq.rest.sqs SQSRestServerBuilder]))

(defn with-elasticmq
  "Fixture to run tests using an embedded ElasticMQ server"
  [tests]
  (let [server (SQSRestServerBuilder/start)]
    (.waitUntilStarted server)
    (with-credential ["sqs" "" "http://localhost:9324"]
      (tests))
    (.stopAndWait server)))

(defn message-received?
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
    (timbre/set-config! {:level :debug :appenders {:sqs-appender (sqs-appender {:queue-name "test"})}})
    (timbre/error "A test message")
    (message-received? "test" #"A test message"))

  (testing "ensure app name is included in the message"
    (timbre/set-config! {:level :debug :appenders {:sqs-appender (sqs-appender {:queue-name "test" :application-name "acme"})}})
    (timbre/error "")
    (message-received? "test" #"acme"))
)

(deftest test-queue-config
  (testing "no messages are sent if a queue is not configured"
    (timbre/set-config! {:level :debug :appenders {:sqs-appender (sqs-appender {})}})
    (timbre/error "A test message")
    (is (= 0 (-> (sqs/list-queues) :queue-urls count)))))
