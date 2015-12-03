A Clojure library designed to log [Timbre](https://github.com/ptaoussanis/timbre) messages to [Amazon SQS](https://aws.amazon.com/sqs/).

## Installation

Boot:

Add the following to `build.boot`:

```clojure
[com.hendrick/timbre.sqs "0.2.0"]
```

Leiningen:

Add the following to `project.clj`:

```clojure
[com.hendrick/timbre.sqs "0.2.0"]
```

## Usage

```clojure
(require '[com.hendrick/timbre-sqs :refer [sqs-appender]])
(timbre/set-config! {:level :debug :appenders {:sqs-appender (sqs-appender {:queue-name "test" :application-name "uber but for dolphins"})}})
(timbre/info "A test message")
```

## License

Copyright © 2015 Hendrick Automotive Group

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
