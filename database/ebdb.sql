-- MySQL dump 10.13  Distrib 5.1.44-ndb-7.1.3, for apple-darwin10.2.0 (i386)
--
-- Host: purple-dev-db.cqxql2suz5ru.us-west-2.rds.amazonaws.com    Database: ebdb
-- ------------------------------------------------------
-- Server version	5.5.42-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Temporary table structure for view `ActiveUsers`
--

DROP TABLE IF EXISTS `ActiveUsers`;
/*!50001 DROP VIEW IF EXISTS `ActiveUsers`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `ActiveUsers` (
  `id` varchar(255),
  `email` varchar(255),
  `phone_number` varchar(50),
  `name` varchar(255),
  `gender` varchar(20),
  `timestamp_created` timestamp,
  `num_orders` bigint(21)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `ActiveUsersInLAWithPush`
--

DROP TABLE IF EXISTS `ActiveUsersInLAWithPush`;
/*!50001 DROP VIEW IF EXISTS `ActiveUsersInLAWithPush`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `ActiveUsersInLAWithPush` (
  `id` varchar(255),
  `email` varchar(255),
  `phone_number` varchar(50),
  `timestamp_created` timestamp,
  `arn_endpoint` varchar(255)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `ActiveUsersWithPush`
--

DROP TABLE IF EXISTS `ActiveUsersWithPush`;
/*!50001 DROP VIEW IF EXISTS `ActiveUsersWithPush`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `ActiveUsersWithPush` (
  `id` varchar(255),
  `email` varchar(255),
  `phone_number` varchar(50),
  `timestamp_created` timestamp,
  `arn_endpoint` varchar(255)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `InactiveUsers`
--

DROP TABLE IF EXISTS `InactiveUsers`;
/*!50001 DROP VIEW IF EXISTS `InactiveUsers`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `InactiveUsers` (
  `id` varchar(255),
  `email` varchar(255),
  `phone_number` varchar(50),
  `name` varchar(255),
  `gender` varchar(20),
  `timestamp_created` timestamp,
  `num_orders` bigint(21)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `InactiveUsersWithPush`
--

DROP TABLE IF EXISTS `InactiveUsersWithPush`;
/*!50001 DROP VIEW IF EXISTS `InactiveUsersWithPush`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `InactiveUsersWithPush` (
  `id` varchar(255),
  `email` varchar(255),
  `phone_number` varchar(50),
  `timestamp_created` timestamp,
  `arn_endpoint` varchar(255)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `MailChimpCSV`
--

DROP TABLE IF EXISTS `MailChimpCSV`;
/*!50001 DROP VIEW IF EXISTS `MailChimpCSV`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `MailChimpCSV` (
  `email` varchar(255),
  `phone_number` varchar(50),
  `name` varchar(255),
  `gender` varchar(20),
  `timestamp_created` timestamp,
  `Orders` bigint(21)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `Users Augmented`
--

DROP TABLE IF EXISTS `Users Augmented`;
/*!50001 DROP VIEW IF EXISTS `Users Augmented`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `Users Augmented` (
  `id` varchar(255),
  `email` varchar(255),
  `phone_number` varchar(50),
  `name` varchar(255),
  `gender` varchar(20),
  `timestamp_created` timestamp,
  `Orders` bigint(21),
  `Referral Gallons Unused` double,
  `Referral Gallons Used` double
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `account_managers`
--

DROP TABLE IF EXISTS `account_managers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `account_managers` (
  `id` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `coupons`
--

DROP TABLE IF EXISTS `coupons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `coupons` (
  `id` varchar(255) NOT NULL,
  `code` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL COMMENT 'standard or referral',
  `value` int(11) NOT NULL DEFAULT '0' COMMENT 'if type is standard, the value of the discount in cents',
  `owner_user_id` varchar(255) NOT NULL DEFAULT '' COMMENT 'if this is a referral, then the user id of the origin account',
  `used_by_license_plates` mediumtext NOT NULL DEFAULT '' COMMENT 'comma-separated list of license plates',
  `used_by_user_ids` mediumtext NOT NULL DEFAULT '' COMMENT 'comma-separated list of user ids',
  `max_uses` int(11) NOT NULL DEFAULT '1999999999',
  `only_for_first_orders` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'All coupons can only be used once, but coupons with this field as TRUE can only be used on vehicles that have never been part of an order',
  `zip_codes` text NOT NULL DEFAULT '',
  `expiration_time` int(11) NOT NULL DEFAULT '1999999999' COMMENT 'coupon can''t be used after this point in time',
  `timestamp_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `couriers`
--

DROP TABLE IF EXISTS `couriers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `couriers` (
  `id` varchar(255) NOT NULL COMMENT 'should match id of ''user'' this corresponds to',
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `on_duty` tinyint(1) NOT NULL DEFAULT '0',
  `connected` tinyint(1) NOT NULL DEFAULT '0',
  `busy` tinyint(1) NOT NULL DEFAULT '0',
  `markets` varchar(4000) DEFAULT '' COMMENT 'comma-separated list of market ids that this courier is assigned to',
  `zones` mediumtext NOT NULL DEFAULT '' COMMENT 'zones they are servicing. ONLY USE ZONE NUMBERS THAT ARE DEFINED IN zones TABLE, or fatal error',
  `gallons_87` double NOT NULL DEFAULT '0',
  `gallons_91` double NOT NULL DEFAULT '0',
  `lat` double NOT NULL,
  `lng` double NOT NULL,
  `last_ping` int(11) NOT NULL DEFAULT '0' COMMENT 'unix time of last ping',
  `timestamp_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dashboard_users`
--

DROP TABLE IF EXISTS `dashboard_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dashboard_users` (
  `id` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL DEFAULT '',
  `reset_key` varchar(255) NOT NULL DEFAULT '',
  `permissions` text COMMENT 'comma-seperated permissions',
  `event_log` text,
  `timestamp_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fleet_accounts`
--

DROP TABLE IF EXISTS `fleet_accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fleet_accounts` (
  `id` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `address_zip` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fleet_deliveries`
--

DROP TABLE IF EXISTS `fleet_deliveries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fleet_deliveries` (
  `id` varchar(255) NOT NULL,
  `account_id` varchar(255) NOT NULL,
  `courier_id` varchar(255) NOT NULL,
  `vin` varchar(255) NOT NULL DEFAULT '',
  `license_plate` varchar(255) NOT NULL DEFAULT '',
  `gallons` double NOT NULL DEFAULT '0',
  `gas_type` varchar(255) NOT NULL,
  `is_top_tier` tinyint(1) NOT NULL DEFAULT '1',
  `gas_price` int(11) NOT NULL DEFAULT '0',
  `timestamp_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `account_id` (`account_id`),
  KEY `courier_id` (`courier_id`),
  KEY `vin` (`vin`),
  KEY `license_plate` (`license_plate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `orders` (
  `id` varchar(255) NOT NULL,
  `status` varchar(50) NOT NULL DEFAULT '',
  `user_id` varchar(255) NOT NULL DEFAULT '',
  `courier_id` varchar(255) NOT NULL DEFAULT '',
  `vehicle_id` varchar(255) NOT NULL DEFAULT '',
  `license_plate` varchar(255) NOT NULL DEFAULT '' COMMENT 'license plate of the vehicle at the time the order was made',
  `target_time_start` int(11) NOT NULL DEFAULT '0',
  `target_time_end` int(11) NOT NULL DEFAULT '0',
  `gallons` double NOT NULL DEFAULT '0',
  `gas_type` varchar(255) NOT NULL DEFAULT '',
  `is_top_tier` tinyint(1) NOT NULL DEFAULT '1',
  `tire_pressure_check` tinyint(1) NOT NULL DEFAULT '0',
  `special_instructions` text NOT NULL DEFAULT '',
  `lat` double NOT NULL DEFAULT '0',
  `lng` double NOT NULL DEFAULT '0',
  `address_street` varchar(255) NOT NULL DEFAULT '',
  `address_city` varchar(255) NOT NULL DEFAULT '',
  `address_state` varchar(255) NOT NULL DEFAULT '',
  `address_zip` varchar(50) NOT NULL DEFAULT '',
  `referral_gallons_used` double NOT NULL DEFAULT '0',
  `coupon_code` varchar(255) NOT NULL DEFAULT '',
  `subscription_id` int(11) NOT NULL DEFAULT '0' COMMENT 'the subscription id that was used on this order - i.e., the subscription_id that the user had at the time they made the order',
  `subscription_discount` int(11) NOT NULL DEFAULT '0' COMMENT 'negative amount of cents; the total discount due to any subscription used',
  `gas_price` int(11) NOT NULL DEFAULT '0' COMMENT 'cents',
  `service_fee` int(11) NOT NULL DEFAULT '0' COMMENT 'cents',
  `total_price` int(11) NOT NULL DEFAULT '0' COMMENT 'cents',
  `paid` tinyint(1) DEFAULT '0',
  `stripe_charge_id` varchar(255) DEFAULT '',
  `stripe_refund_id` varchar(255) DEFAULT '',
  `stripe_customer_id_charged` varchar(255) DEFAULT '',
  `stripe_balance_transaction_id` varchar(255) DEFAULT '',
  `time_paid` int(11) DEFAULT '0',
  `payment_info` text,
  `number_rating` int(11) DEFAULT NULL COMMENT '0-5 stars',
  `text_rating` text NOT NULL DEFAULT '',
  `event_log` mediumtext NOT NULL DEFAULT '',
  `admin_event_log` text NOT NULL DEFAULT '',
  `notes` text NOT NULL DEFAULT '',
  `timestamp_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `user_id_2` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sessions`
--

DROP TABLE IF EXISTS `sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sessions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(255) NOT NULL,
  `token` varchar(255) NOT NULL,
  `ip` varchar(100) NOT NULL,
  `source` varchar(100) DEFAULT '',
  `timestamp_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2794 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `station_blacklist`
--

DROP TABLE IF EXISTS `station_blacklist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `station_blacklist` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `station_id` varchar(255) NOT NULL,
  `creator_user_id` varchar(255) NOT NULL,
  `until` int(11) NOT NULL DEFAULT '1999999999' COMMENT 'station will be blacklisted until this unix time',
  `reason` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `station_id` (`station_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `subscriptions`
--

DROP TABLE IF EXISTS `subscriptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `subscriptions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `price` int(11) NOT NULL COMMENT 'cost in cents for every period (use -1 for subscriptions that can''t be purchased by normal users)',
  `period` int(11) NOT NULL DEFAULT '2592000',
  `num_free_one_hour` int(11) NOT NULL DEFAULT '0',
  `num_free_three_hour` int(11) NOT NULL DEFAULT '0',
  `num_free_five_hour` int(11) NOT NULL DEFAULT '0',
  `num_free_tire_pressure_check` int(11) NOT NULL DEFAULT '0',
  `discount_one_hour` int(11) NOT NULL DEFAULT '0' COMMENT 'discount in cents after the free deliveries are used per a period (should be a negative amount)',
  `discount_three_hour` int(11) NOT NULL DEFAULT '0',
  `discount_five_hour` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `test`
--

DROP TABLE IF EXISTS `test`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `test` (
  `id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `type` varchar(50) NOT NULL COMMENT 'native, facebook, or google',
  `password_hash` varchar(255) NOT NULL DEFAULT '',
  `reset_key` varchar(255) NOT NULL DEFAULT '',
  `phone_number` varchar(50) NOT NULL DEFAULT '',
  `phone_number_verified` tinyint(1) NOT NULL DEFAULT '0',
  `name` varchar(255) NOT NULL DEFAULT '',
  `gender` varchar(20) DEFAULT '',
  `saved_locations` text,
  `referral_code` varchar(255) NOT NULL DEFAULT '',
  `referral_gallons` double NOT NULL DEFAULT '0',
  `is_courier` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'if ''true'', there should be an entry with this id in the ''couriers'' table',
  `account_manager_id` varchar(255) NOT NULL DEFAULT '',
  `subscription_id` int(11) NOT NULL DEFAULT '0' COMMENT '0 means no subscription',
  `subscription_period_start_time` int(11) DEFAULT NULL COMMENT 'not necessarily equal to subscription_expiration_time - [the subscription''s period]',
  `subscription_expiration_time` int(11) DEFAULT NULL,
  `subscription_auto_renew` tinyint(1) NOT NULL DEFAULT '0',
  `subscription_payment_log` text NOT NULL DEFAULT '',
  `stripe_customer_id` varchar(255) DEFAULT '',
  `stripe_cards` text NOT NULL DEFAULT '',
  `stripe_default_card` varchar(255) DEFAULT NULL,
  `apns_token` varchar(255) NOT NULL DEFAULT '',
  `arn_endpoint` varchar(255) NOT NULL DEFAULT '',
  `os` varchar(255) DEFAULT '',
  `app_version` varchar(50) DEFAULT '',
  `sift_score` int(11) DEFAULT NULL COMMENT 'most recent sift score to determine if fraudulent user',
  `admin_event_log` text NOT NULL DEFAULT '',
  `timestamp_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `vehicles`
--

DROP TABLE IF EXISTS `vehicles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vehicles` (
  `id` varchar(255) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `user_id` varchar(255) NOT NULL,
  `year` varchar(50) NOT NULL,
  `make` varchar(255) NOT NULL,
  `model` varchar(255) NOT NULL,
  `color` varchar(255) NOT NULL,
  `gas_type` varchar(255) NOT NULL,
  `only_top_tier` tinyint(1) NOT NULL DEFAULT '1',
  `license_plate` varchar(255) NOT NULL,
  `photo` mediumtext NOT NULL DEFAULT '',
  `timestamp_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `user_id_2` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `zctas`
--

DROP TABLE IF EXISTS `zctas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `zctas` (
  `zip` int(5) unsigned zerofill NOT NULL,
  `coordinates` text NOT NULL DEFAULT '' COMMENT 'zcta boundary coordinates',
  PRIMARY KEY (`zip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='zcta boundary coordinates using cb_2014_us_zcta510_500k.kml';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `zones`
--

DROP TABLE IF EXISTS `zones`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `zones` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `active` tinyint(1) NOT NULL DEFAULT '0',
  `zip_codes` mediumtext NOT NULL DEFAULT '',
  `name` varchar(255) NOT NULL,
  `color` varchar(255) NOT NULL,
  `fuel_prices` varchar(255) NOT NULL COMMENT 'edn format',
  `service_fees` varchar(255) NOT NULL COMMENT 'edn format',
  `service_time_bracket` varchar(255) NOT NULL COMMENT 'edn format',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=154 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Final view structure for view `ActiveUsers`
--

/*!50001 DROP TABLE IF EXISTS `ActiveUsers`*/;
/*!50001 DROP VIEW IF EXISTS `ActiveUsers`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
-- /*!50001 SET character_set_client      = utf8mb4 */;

-- /*!50001 SET character_set_results     = utf8mb4 */;

-- /*!50001 SET collation_connection      = utf8mb4_unicode_ci */;

/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`purplemaster`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `ActiveUsers` AS select `users`.`id` AS `id`,`users`.`email` AS `email`,`users`.`phone_number` AS `phone_number`,`users`.`name` AS `name`,`users`.`gender` AS `gender`,`users`.`timestamp_created` AS `timestamp_created`,(select count(0) from `orders` where ((`users`.`id` = `orders`.`user_id`) and (`orders`.`status` = 'complete'))) AS `num_orders` from `users` where ((select count(0) from `orders` where ((`users`.`id` = `orders`.`user_id`) and (`orders`.`status` = 'complete'))) > 0) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ActiveUsersInLAWithPush`
--

/*!50001 DROP TABLE IF EXISTS `ActiveUsersInLAWithPush`*/;
/*!50001 DROP VIEW IF EXISTS `ActiveUsersInLAWithPush`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
-- /*!50001 SET character_set_client      = utf8mb4 */;

-- /*!50001 SET character_set_results     = utf8mb4 */;

-- /*!50001 SET collation_connection      = utf8mb4_unicode_ci */;

/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`purplemaster`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `ActiveUsersInLAWithPush` AS select `users`.`id` AS `id`,`users`.`email` AS `email`,`users`.`phone_number` AS `phone_number`,`users`.`timestamp_created` AS `timestamp_created`,`users`.`arn_endpoint` AS `arn_endpoint` from `users` where ((`users`.`arn_endpoint` <> '') and ((select count(0) from `orders` where ((`users`.`id` = `orders`.`user_id`) and (`orders`.`status` = 'complete') and (`orders`.`address_zip` regexp '^9[01].*$'))) > 0)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ActiveUsersWithPush`
--

/*!50001 DROP TABLE IF EXISTS `ActiveUsersWithPush`*/;
/*!50001 DROP VIEW IF EXISTS `ActiveUsersWithPush`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
-- /*!50001 SET character_set_client      = utf8mb4 */;

-- /*!50001 SET character_set_results     = utf8mb4 */;

-- /*!50001 SET collation_connection      = utf8mb4_unicode_ci */;

/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`purplemaster`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `ActiveUsersWithPush` AS select `users`.`id` AS `id`,`users`.`email` AS `email`,`users`.`phone_number` AS `phone_number`,`users`.`timestamp_created` AS `timestamp_created`,`users`.`arn_endpoint` AS `arn_endpoint` from `users` where ((`users`.`arn_endpoint` <> '') and ((select count(0) from `orders` where ((`users`.`id` = `orders`.`user_id`) and (`orders`.`status` = 'complete'))) > 0)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `InactiveUsers`
--

/*!50001 DROP TABLE IF EXISTS `InactiveUsers`*/;
/*!50001 DROP VIEW IF EXISTS `InactiveUsers`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
-- /*!50001 SET character_set_client      = utf8mb4 */;

-- /*!50001 SET character_set_results     = utf8mb4 */;

-- /*!50001 SET collation_connection      = utf8mb4_unicode_ci */;

/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`purplemaster`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `InactiveUsers` AS select `users`.`id` AS `id`,`users`.`email` AS `email`,`users`.`phone_number` AS `phone_number`,`users`.`name` AS `name`,`users`.`gender` AS `gender`,`users`.`timestamp_created` AS `timestamp_created`,(select count(0) from `orders` where ((`users`.`id` = `orders`.`user_id`) and (`orders`.`status` = 'complete'))) AS `num_orders` from `users` where ((select count(0) from `orders` where ((`users`.`id` = `orders`.`user_id`) and (`orders`.`status` = 'complete'))) = 0) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `InactiveUsersWithPush`
--

/*!50001 DROP TABLE IF EXISTS `InactiveUsersWithPush`*/;
/*!50001 DROP VIEW IF EXISTS `InactiveUsersWithPush`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
-- /*!50001 SET character_set_client      = utf8mb4 */;

-- /*!50001 SET character_set_results     = utf8mb4 */;

-- /*!50001 SET collation_connection      = utf8mb4_unicode_ci */;

/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`purplemaster`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `InactiveUsersWithPush` AS select `users`.`id` AS `id`,`users`.`email` AS `email`,`users`.`phone_number` AS `phone_number`,`users`.`timestamp_created` AS `timestamp_created`,`users`.`arn_endpoint` AS `arn_endpoint` from `users` where ((`users`.`arn_endpoint` <> '') and ((select count(0) from `orders` where ((`users`.`id` = `orders`.`user_id`) and (`orders`.`status` = 'complete'))) = 0)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `MailChimpCSV`
--

/*!50001 DROP TABLE IF EXISTS `MailChimpCSV`*/;
/*!50001 DROP VIEW IF EXISTS `MailChimpCSV`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
-- /*!50001 SET character_set_client      = utf8mb4 */;

-- /*!50001 SET character_set_results     = utf8mb4 */;

-- /*!50001 SET collation_connection      = utf8mb4_unicode_ci */;

/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`purplemaster`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `MailChimpCSV` AS select `users`.`email` AS `email`,`users`.`phone_number` AS `phone_number`,`users`.`name` AS `name`,`users`.`gender` AS `gender`,`users`.`timestamp_created` AS `timestamp_created`,(select count(0) from `orders` where ((`users`.`id` = `orders`.`user_id`) and (`orders`.`status` = 'complete'))) AS `Orders` from `users` where 1 order by (select count(0) from `orders` where ((`users`.`id` = `orders`.`user_id`) and (`orders`.`status` = 'complete'))) desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `Users Augmented`
--

/*!50001 DROP TABLE IF EXISTS `Users Augmented`*/;
/*!50001 DROP VIEW IF EXISTS `Users Augmented`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
-- /*!50001 SET character_set_client      = utf8mb4 */;

-- /*!50001 SET character_set_results     = utf8mb4 */;

-- /*!50001 SET collation_connection      = utf8mb4_unicode_ci */;

/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`purplemaster`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `Users Augmented` AS select `users`.`id` AS `id`,`users`.`email` AS `email`,`users`.`phone_number` AS `phone_number`,`users`.`name` AS `name`,`users`.`gender` AS `gender`,`users`.`timestamp_created` AS `timestamp_created`,(select count(0) from `orders` where ((`users`.`id` = `orders`.`user_id`) and (`orders`.`status` = 'complete'))) AS `Orders`,`users`.`referral_gallons` AS `Referral Gallons Unused`,(select sum(`orders`.`referral_gallons_used`) from `orders` where ((`users`.`id` = `orders`.`user_id`) and (`orders`.`status` = 'complete'))) AS `Referral Gallons Used` from `users` where 1 order by (select sum(`orders`.`referral_gallons_used`) from `orders` where ((`users`.`id` = `orders`.`user_id`) and (`orders`.`status` = 'complete'))) desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-09-15 16:42:08
