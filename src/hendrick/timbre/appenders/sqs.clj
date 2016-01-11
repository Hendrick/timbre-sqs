(ns hendrick.timbre.appenders.sqs
  (:require [amazonica.aws.sqs :as sqs]
            [amazonica.core :refer [with-credential]]
            [cheshire.core :as json]
            [clojure.stacktrace :as trace]
            [taoensso.timbre :as timbre]))

(defmacro with-aws-cred
  "If AWS credentials are supplied, then call the block using Amazonica's
   `with-credential`, otherwise just execute the block directly."
  [credential & body]
  `(if ~credential
     (with-credential ((juxt :access-key :secret-key :endpoint) ~credential)
       ~@body)
     ~@body))

(defn queue-name->url
  "If the `:queue-url` key doesn't already exist in config, look for a `:queue-name` value
   and retrieve the corresponding url. Uses credentials from the config, if present."
  [{:keys [queue-url queue-name credential] :as config}]
  (assoc
   config :queue-url
   (or queue-url
       (and queue-name
            (:queue-url
             (with-aws-cred credential
               (sqs/create-queue queue-name)))))))

(defn sqs-appender
  "Returns an SQS appender.

  (sqs-appender {:queue-name \"test\" :application-name \"my-app\"}
  (sqs-appender {:queue-url \"http://sqs.us-east-1.amazonaws.com/123456789012/testQueue\" :application-name \"my-app\"})
  (sqs-appender {:queue-name \"test\" :credential {:access-key \"foo\" :secret-key \"bar\" :endpoint \"baz\" :application-name \"my-app\"})"
  [sqs-config]
  (let [queue-url (:queue-url (queue-name->url sqs-config))
        app (:application-name sqs-config)]
    {:async?    false
     :enabled?  true
     :min-level nil
     :output-fn (fn [data]
                  (let [{:keys [level error-level? vargs_ ?file ?line ?err_ ?ns-str msg_]} data]
                    (json/generate-string (merge
                                            {:app app
                                             :level level
                                             :message @vargs_
                                             :file ?file
                                             :line ?line
                                             :ns ?ns-str}
                                            (when-let [e @?err_]
                                              {:error {:stacktrace (timbre/stacktrace e {:stacktrace-fonts {}})
                                                       :exception-message (.getMessage e)
                                                       :error-level error-level?}})))))
     :fn        (fn [data]
                  (let [{:keys [output-fn]} data]
                    (when queue-url
                      (sqs/send-message :queue-url queue-url
                                        :message-body (output-fn data)))))}))
