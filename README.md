# timbre-sqs

A Clojure library designed to log Timbre messages to Amazon SQS.

## Usage

```clojure
(timbre/set-config! {:appenders {:sqs-appender (sqs-appender {:queue-name "test"})}})
(timbre/set-level! :debug)
(timbre/error "A test message")
```

## License

Copyright © 2015 Hendrick Automotive Group

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
