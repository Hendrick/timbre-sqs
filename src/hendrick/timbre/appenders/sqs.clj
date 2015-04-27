(ns hendrick.timbre.appenders.sqs
  (:require [amazonica.aws.sqs :as sqs]
            [amazonica.core :refer [with-credential]]
            [taoensso.timbre :as timbre]))

(def default-appender-opts
  {:async?        true
   :enabled?      true
   :min-level     :info})

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

(defn make-appender-fn [{:keys [queue-url credential]}]
  "Returns a function that combines runtime and appender config, and logs to SQS so long
   as a queue URL is either specified or can be created. If AWS credentials are specified
   in either config, they will be used to authenticate to SQS."
  (fn [{:keys [ap-config output]}]
    (let [credential (or credential (:credential ap-config))
          queue-url (or queue-url (-> (assoc ap-config :credential credential)
                                      queue-name->url
                                      :queue-url))]
      (when queue-url
        (with-aws-cred credential
          (sqs/send-message :queue-url queue-url
                            :message-body output))))))

(defn make-sqs-appender
  "Returns an appender that will send logs to SQS. If configuration is not set during
   appender creation, it may be set at runtime (using `timbre/set-config!
   [:shared-appender-config :sqs]`). No logs will be sent to SQS until a configuration is
   provided."
  [& [appender-opts config]]
  (let [config (queue-name->url config)]
    (merge default-appender-opts
           appender-opts
           {:fn (make-appender-fn config)})))

