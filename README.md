# portal-service

The Clojure Portal Service Server

[![Build Status](https://travis-ci.com/Purple-Services/portal-service.svg?token=qtYcDv5JYzqmyunRnB93&branch=dev)](https://travis-ci.com/Purple-Services/portal-service)

## Testing

There are two kinds of tests, unit and functional. Unit tests use the standard
clojure.test library to test individual functions. Functional tests make use of
the Selenium WebDriver for the Chrome Browser (chromedriver) in order to test
proper functionality of the webapp frontend.

To run 'lein tests':

1. profiles.clj will need to be present for common/config.clj. Obtain
one from the development team.

**Notes**:

1. When running 'lein ring server' change the :base-url to
http://192.168.1.100:3002/ Be sure to change it BACK before running tests!

## License

Copyright © 2016 Purple Services, Inc.

All Rights Reserved.
