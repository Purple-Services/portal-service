# portal-service

The Clojure Portal Service Server

## Testing

There are two kinds of tests, unit and functional. Unit tests use the standard
clojure.test library to test individual functions. Functional tests make use of
the Selenium WebDriver for the Chrome Browser (chromedriver) in order to test
proper functionality of the webapp frontend.

To run lein tests:

1. Edit profiles.clj to include:
```clojure
   {:dev {:env {:base-url "http://localhost:5744/"}}}
```
**Note**: You must use port 5744 or change the corresponding test-port in
test/functional/test/portal.clj

**Note**: Make sure you are not running another test server at the repl
on port 5744

## License

Copyright © 2016 Purple Services, Inc.

All Rights Reserved.
