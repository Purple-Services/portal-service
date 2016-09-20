DROP DATABASE if EXISTS ebdb_dev;
DROP DATABASE if EXISTS ebdb_test;
CREATE database if not EXISTS ebdb_dev;
CREATE database if not EXISTS ebdb_test;
GRANT ALL on ebdb_dev.* to 'purplemaster'@'localhost'  identified by 'localpurpledevelopment2015';
GRANT ALL on ebdb_test.* to 'purplemaster'@'localhost'  identified by 'localpurpledevelopment2015';
