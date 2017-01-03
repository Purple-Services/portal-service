[![Build Status](https://travis-ci.com/Purple-Services/portal-service.svg?token=qtYcDv5JYzqmyunRnB93&branch=dev)](https://travis-ci.com/Purple-Services/portal-service)

# Portal Service

Portal Service Server for Business Clients

# Getting Started

## Download the code

There a few git repos that make up the Purple ecosystem. Create a "Purple-Services"
dir somewhere in your home dir or other appropriate place to keep all of the repos.

These repos make up the service:

Git repo: Purple-Services/common
Clojure project name: common
Description: library containing majority of logic (used by web/dashboard-service) + database calls + misc

Git repo: Purple-Services/portal-service
Clojure project name: dashboard
Description: dashboard api + logic specific to dashboard

Git repo: Purple-Services/portal-cljs
Clojure project name: dashboard
Description: ClojureScript for portal-service

## common library installation and configuration

Clone all of these repos to your Purple-Services dir. The common and opt libraries must be installed into your local repository with the following commands:

```bash
~/Purple-Services/common$ lein install
```

There should be little, if any, editing of the common library during development. However, sometimes common
must be developed in parallel to portal-service. Use the checkouts/ dir (https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md#checkout-dependencies) to facilitate this by linking to your local repo with ln.

```bash
~/Purple-Services/dashboard-service/checkouts$ ln -s ~/Purple-Services/common common
```

You'll end up with a dir structure like this in dashboard-service:
```
    .
    |-- project.clj
    |-- README.md
    |-- checkouts
    |   `-- common [link to ~/Purple-Services/common]
    `-- src
        `-- app
            `-- handler.clj
```

## Request addition of your IP address to RDS

Navigate to https://www.whatismyip.com/ and send your IP address to Chris in order to be added to the AWS RDS. You will have to update your IP address whenever it changes. This step must be completed in order to access the test database so that you will be able to develop locally. This must be done before continuing further if you plan to connect to the development database on AWS as opposed to using your local MySQL database.

## Local MySQL Database Configuration

Run the following command to create the local MySQL databases if you haven't already followed along
with the 'Using a local MySQL Database for Development' section of app-service.

```bash
$ mysql -u root -p<password> < database/ebdb.sql
```

Update the database/ebdb.sql file with the script
```bash
$ scripts/retrieve_sql
```
ebdb.sql contains the proper structure for building a database needed to run the tests. Whenever there are database structural changes, this will need to be run again.

## profiles.clj

For local development, <project_root>/profiles.clj is used to define environment variables. However, profiles.clj is included in .gitignore and is not included in the repository. When you first start working on the project, you will have to obtain a profiles.clj from the dev team. You can edit the profiles.clj to match your local environment.

In profiles.clj, the value of :db-host is the database host used for development. If you have MySQL configured on your machine, you can use the value "localhost" with a :db-password that you set. Otherwise, you can use the AWS host and pwd values to access the remote development server. You will eventually need to setup a local MySQL server in order to run tests that access the database. See "Using a local MySQL Database for Development" below on how to configure this.

## Run Tests

There are functional tests that require chrome webdriver in order to pass. Obtain it from 
https://sites.google.com/a/chromium.org/chromedriver/downloads

You can then test the code with

```bash
$ lein test
```

# Development

## Database Setup

You can build a local ebdb_dev MySQL database with *scripts/sync_dev_db*. Edit *scripts/retrieve_dev_db_data* to include your credentials.

Edit the :db-* keywords in profiles.clj to correspond to local database access.

## Start the server

The Clojure dashboard server is developed in tandem with the ClojureScript dashboard-cljs client. In order to run the server, edit profiles.clj to set the port used for the server to 3001:

```clojure
:base-url "http://localhost:3002/"
```

Start the server for development with:

```bash
$ lein ring server
```

## Testing at the repl

Both unit and functional tests can be run from the REPL. You will need to modify the :base-url in profiles.clj

```clojure
:base-url "http://localhost:5744/"
```

Navigate to a file in dashboard-service and use M-x cider-jack-in in Emacs to connect to the repl.

For example functional tests, see portal-service/test/portal/functional/test/portal.clj

There are comments in the source on how to run the functional tests from the repl.


## License

Copyright © 2017 Purple Services, Inc.

All Rights Reserved.
