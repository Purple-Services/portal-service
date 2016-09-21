# portal-service

The Clojure Portal Service Server

## Testing

There are two kinds of tests, unit and functional. Unit tests use the standard
clojure.test library to test individual functions. Functional tests make use of
the Selenium WebDriver for the Chrome Browser (chromedriver) in order to test
proper functionality of the webapp frontend.

To run 'lein tests':

1. Edit profiles.clj to include:
```clojure
   {:dev {:env {:base-url "http://localhost:5744/"}}}
```
**Notes**:

1. You must use port 5744 or change the corresponding test-port in
test/functional/test/portal.

2. Make sure you are not running another test server at the repl
on port 5744

3. When running 'lein ring server' change the :base-url to
http://192.168.1.100:3002/ Be sure to change it BACK before running tests!

### Continuous local testing

see: https://github.com/jakemcc/lein-test-refresh

**Remember**: You must change your profile.clj to use
:base-url http://localhost:5744/ If Selenium functional tests start failing,
check the host they are using. You may have switched the :base-url to run
'lein ring server'!

It is possible to continuously run tests in a terminal. To do this, simply run

```bash
$ lein test-refresh
```

in another terminal. The tests are run everytime you change .clj source files.
You will be notified via Growl notification any time a test fails.

## License

Copyright © 2016 Purple Services, Inc.

All Rights Reserved.
