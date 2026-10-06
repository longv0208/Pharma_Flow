-- MySQL dump 10.13  Distrib 8.0.36, for Win64 (x86_64)
--
-- Host: localhost    Database: pharmaflow
-- ------------------------------------------------------
-- Server version	8.0.37

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `cart_items`
--

DROP TABLE IF EXISTS `cart_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart_items` (
  `cart_item_id` bigint NOT NULL AUTO_INCREMENT,
  `cart_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`cart_item_id`),
  UNIQUE KEY `uq_cart_product` (`cart_id`,`product_id`),
  KEY `fk_cart_item_product` (`product_id`),
  CONSTRAINT `fk_cart_item_cart` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`cart_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cart_item_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  CONSTRAINT `chk_cart_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart_items`
--

LOCK TABLES `cart_items` WRITE;
/*!40000 ALTER TABLE `cart_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `cart_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `carts`
--

DROP TABLE IF EXISTS `carts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carts` (
  `cart_id` bigint NOT NULL AUTO_INCREMENT,
  `customer_id` bigint NOT NULL,
  `status` enum('ACTIVE','CONVERTED','CLEARED') NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`cart_id`),
  KEY `fk_cart_customer` (`customer_id`),
  CONSTRAINT `fk_cart_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer_profiles` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `carts`
--

LOCK TABLES `carts` WRITE;
/*!40000 ALTER TABLE `carts` DISABLE KEYS */;
/*!40000 ALTER TABLE `carts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `category_id` bigint NOT NULL AUTO_INCREMENT,
  `category_name` varchar(150) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `uq_category_name` (`category_name`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (1,'Pain Relief','Medicines used for pain and fever relief','ACTIVE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(2,'Cold & Flu','Medicines for cold, flu, cough and related symptoms','ACTIVE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(3,'Digestive Health','Medicines for digestive system and stomach conditions','ACTIVE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(4,'Vitamins & Supplements','Vitamin and mineral supplements','ACTIVE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(5,'Antibiotics','Prescription antibiotic medicines','ACTIVE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(6,'Allergy','Medicines used for allergy symptoms','ACTIVE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(7,'Dermatology','Products for skin-related conditions','ACTIVE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(8,'Inactive Category','Category used to test inactive status','INACTIVE','2026-10-02 02:04:52','2026-10-02 02:04:52');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customer_profiles`
--

DROP TABLE IF EXISTS `customer_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_profiles` (
  `customer_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `province_city` varchar(100) DEFAULT NULL,
  `district` varchar(100) DEFAULT NULL,
  `ward` varchar(100) DEFAULT NULL,
  `detailed_address` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`customer_id`),
  UNIQUE KEY `uq_customer_user` (`user_id`),
  CONSTRAINT `fk_customer_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer_profiles`
--

LOCK TABLES `customer_profiles` WRITE;
/*!40000 ALTER TABLE `customer_profiles` DISABLE KEYS */;
INSERT INTO `customer_profiles` VALUES (1,1,'test','123','123','123','2026-10-02 03:23:00','2026-10-02 03:39:47'),(4,2,NULL,NULL,NULL,NULL,'2026-10-02 03:49:58','2026-10-02 03:49:58'),(5,3,NULL,NULL,NULL,NULL,'2026-10-02 06:20:50','2026-10-02 06:20:50'),(6,4,NULL,NULL,NULL,NULL,'2026-10-02 06:22:02','2026-10-02 06:22:02'),(7,5,NULL,NULL,NULL,NULL,'2026-10-02 06:23:37','2026-10-02 06:23:37'),(8,6,NULL,NULL,NULL,NULL,'2026-10-02 06:31:53','2026-10-02 06:31:53'),(9,7,NULL,NULL,NULL,NULL,'2026-10-03 22:35:36','2026-10-03 22:35:36');
/*!40000 ALTER TABLE `customer_profiles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `goods_receipt_items`
--

DROP TABLE IF EXISTS `goods_receipt_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goods_receipt_items` (
  `goods_receipt_item_id` bigint NOT NULL AUTO_INCREMENT,
  `goods_receipt_id` bigint NOT NULL,
  `purchase_order_item_id` bigint DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `batch_id` bigint DEFAULT NULL,
  `batch_number` varchar(100) NOT NULL,
  `expiry_date` date NOT NULL,
  `quantity` int NOT NULL,
  `cost_price` decimal(15,2) NOT NULL,
  `inspection_result` enum('PENDING','ACCEPTED','REJECTED') NOT NULL DEFAULT 'PENDING',
  `rejection_reason` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`goods_receipt_item_id`),
  KEY `fk_receipt_item_receipt` (`goods_receipt_id`),
  KEY `fk_receipt_item_po_item` (`purchase_order_item_id`),
  KEY `fk_receipt_item_product` (`product_id`),
  KEY `fk_receipt_item_batch` (`batch_id`),
  CONSTRAINT `fk_receipt_item_batch` FOREIGN KEY (`batch_id`) REFERENCES `inventory_batches` (`batch_id`),
  CONSTRAINT `fk_receipt_item_po_item` FOREIGN KEY (`purchase_order_item_id`) REFERENCES `purchase_order_items` (`purchase_order_item_id`),
  CONSTRAINT `fk_receipt_item_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  CONSTRAINT `fk_receipt_item_receipt` FOREIGN KEY (`goods_receipt_id`) REFERENCES `goods_receipts` (`goods_receipt_id`),
  CONSTRAINT `chk_receipt_cost` CHECK ((`cost_price` >= 0)),
  CONSTRAINT `chk_receipt_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `goods_receipt_items`
--

LOCK TABLES `goods_receipt_items` WRITE;
/*!40000 ALTER TABLE `goods_receipt_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `goods_receipt_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `goods_receipts`
--

DROP TABLE IF EXISTS `goods_receipts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goods_receipts` (
  `goods_receipt_id` bigint NOT NULL AUTO_INCREMENT,
  `supplier_id` bigint NOT NULL,
  `purchase_order_id` bigint DEFAULT NULL,
  `received_by` bigint NOT NULL,
  `receipt_date` date NOT NULL,
  `invoice_number` varchar(100) DEFAULT NULL,
  `note` varchar(500) DEFAULT NULL,
  `status` enum('DRAFT','CONFIRMED','PARTIALLY_ACCEPTED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`goods_receipt_id`),
  KEY `fk_goods_receipt_user` (`received_by`),
  KEY `idx_goods_receipt_supplier` (`supplier_id`),
  KEY `idx_goods_receipt_po` (`purchase_order_id`),
  KEY `idx_goods_receipt_date` (`receipt_date`),
  CONSTRAINT `fk_goods_receipt_purchase_order` FOREIGN KEY (`purchase_order_id`) REFERENCES `purchase_orders` (`purchase_order_id`),
  CONSTRAINT `fk_goods_receipt_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `suppliers` (`supplier_id`),
  CONSTRAINT `fk_goods_receipt_user` FOREIGN KEY (`received_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `goods_receipts`
--

LOCK TABLES `goods_receipts` WRITE;
/*!40000 ALTER TABLE `goods_receipts` DISABLE KEYS */;
/*!40000 ALTER TABLE `goods_receipts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventory_adjustments`
--

DROP TABLE IF EXISTS `inventory_adjustments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_adjustments` (
  `adjustment_id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `performed_by` bigint NOT NULL,
  `quantity_change` int NOT NULL,
  `quantity_before` int NOT NULL,
  `quantity_after` int NOT NULL,
  `reason` enum('DAMAGED','LOST','EXPIRED','COUNT_CORRECTION','DATA_CORRECTION','OTHER') NOT NULL,
  `note` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`adjustment_id`),
  KEY `fk_adjustment_batch` (`batch_id`),
  KEY `fk_adjustment_user` (`performed_by`),
  CONSTRAINT `fk_adjustment_batch` FOREIGN KEY (`batch_id`) REFERENCES `inventory_batches` (`batch_id`),
  CONSTRAINT `fk_adjustment_user` FOREIGN KEY (`performed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `chk_adjustment_after` CHECK ((`quantity_after` >= 0)),
  CONSTRAINT `chk_adjustment_before` CHECK ((`quantity_before` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_adjustments`
--

LOCK TABLES `inventory_adjustments` WRITE;
/*!40000 ALTER TABLE `inventory_adjustments` DISABLE KEYS */;
/*!40000 ALTER TABLE `inventory_adjustments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventory_alert_settings`
--

DROP TABLE IF EXISTS `inventory_alert_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_alert_settings` (
  `setting_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint DEFAULT NULL,
  `minimum_stock_level` int NOT NULL DEFAULT '10',
  `near_expiry_warning_days` int NOT NULL DEFAULT '90',
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`setting_id`),
  UNIQUE KEY `uq_alert_setting_product` (`product_id`),
  KEY `fk_alert_setting_user` (`updated_by`),
  CONSTRAINT `fk_alert_setting_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  CONSTRAINT `fk_alert_setting_user` FOREIGN KEY (`updated_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `chk_minimum_stock_level` CHECK ((`minimum_stock_level` >= 0)),
  CONSTRAINT `chk_near_expiry_days` CHECK ((`near_expiry_warning_days` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_alert_settings`
--

LOCK TABLES `inventory_alert_settings` WRITE;
/*!40000 ALTER TABLE `inventory_alert_settings` DISABLE KEYS */;
INSERT INTO `inventory_alert_settings` VALUES (1,NULL,10,90,NULL,'2026-10-02 02:04:52'),(2,2,10,90,NULL,'2026-10-02 02:04:52');
/*!40000 ALTER TABLE `inventory_alert_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventory_batches`
--

DROP TABLE IF EXISTS `inventory_batches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_batches` (
  `batch_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `supplier_id` bigint DEFAULT NULL,
  `source_goods_receipt_id` bigint DEFAULT NULL,
  `batch_number` varchar(100) NOT NULL,
  `expiry_date` date NOT NULL,
  `on_hand_quantity` int NOT NULL DEFAULT '0',
  `reserved_quantity` int NOT NULL DEFAULT '0',
  `cost_price` decimal(15,2) DEFAULT NULL,
  `storage_location` varchar(150) DEFAULT NULL,
  `status` enum('AVAILABLE','NEAR_EXPIRY','EXPIRED','BLOCKED','OUT_OF_STOCK') NOT NULL DEFAULT 'AVAILABLE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`batch_id`),
  UNIQUE KEY `uq_product_batch` (`product_id`,`batch_number`),
  KEY `fk_batch_supplier` (`supplier_id`),
  KEY `fk_batch_goods_receipt` (`source_goods_receipt_id`),
  KEY `idx_batch_product` (`product_id`),
  KEY `idx_batch_expiry` (`expiry_date`),
  KEY `idx_batch_status` (`status`),
  CONSTRAINT `fk_batch_goods_receipt` FOREIGN KEY (`source_goods_receipt_id`) REFERENCES `goods_receipts` (`goods_receipt_id`),
  CONSTRAINT `fk_batch_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  CONSTRAINT `fk_batch_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `suppliers` (`supplier_id`),
  CONSTRAINT `chk_batch_cost` CHECK (((`cost_price` is null) or (`cost_price` >= 0))),
  CONSTRAINT `chk_batch_on_hand` CHECK ((`on_hand_quantity` >= 0)),
  CONSTRAINT `chk_batch_reserved` CHECK ((`reserved_quantity` >= 0)),
  CONSTRAINT `chk_batch_reserved_on_hand` CHECK ((`reserved_quantity` <= `on_hand_quantity`))
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_batches`
--

LOCK TABLES `inventory_batches` WRITE;
/*!40000 ALTER TABLE `inventory_batches` DISABLE KEYS */;
INSERT INTO `inventory_batches` VALUES (1,1,1,NULL,'PARA-2026-01','2028-04-02',100,10,16000.00,'Shelf A1','AVAILABLE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(2,1,1,NULL,'PARA-2026-02','2027-08-02',50,0,16500.00,'Shelf A1','AVAILABLE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(3,2,1,NULL,'IBU-2026-01','2028-01-02',6,1,24000.00,'Shelf A2','AVAILABLE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(4,3,1,NULL,'COLD-2026-01','2027-06-02',30,5,30000.00,'Shelf B1','AVAILABLE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(5,4,2,NULL,'OME-NEAR-01','2026-12-02',25,0,36000.00,'Shelf C1','NEAR_EXPIRY','2026-10-02 02:04:52','2026-10-02 02:04:52'),(6,5,2,NULL,'VITC-2026-01','2028-06-02',80,12,48000.00,'Shelf D1','AVAILABLE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(7,6,2,NULL,'CAL-BLOCK-01','2027-10-02',30,0,85000.00,'Shelf D2','BLOCKED','2026-10-02 02:04:52','2026-10-02 02:04:52'),(8,6,2,NULL,'CAL-VALID-02','2028-02-02',20,2,87000.00,'Shelf D2','AVAILABLE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(9,7,3,NULL,'AMOX-2026-01','2027-12-02',40,0,65000.00,'Shelf RX1','AVAILABLE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(10,8,3,NULL,'CEFU-2026-01','2027-12-02',20,0,125000.00,'Shelf RX1','AVAILABLE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(11,9,3,NULL,'CET-OLD-01','2026-09-02',15,0,27000.00,'Shelf E1','EXPIRED','2026-10-02 02:04:52','2026-10-02 02:04:52'),(12,9,3,NULL,'CET-NEW-02','2027-07-02',25,3,28000.00,'Shelf E1','AVAILABLE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(13,10,1,NULL,'HYDRO-2026-01','2027-08-02',0,0,42000.00,'Shelf F1','OUT_OF_STOCK','2026-10-02 02:04:52','2026-10-02 02:04:52'),(14,11,1,NULL,'REST-2026-01','2027-10-02',10,0,100000.00,'Restricted Shelf','AVAILABLE','2026-10-02 02:04:52','2026-10-02 02:04:52');
/*!40000 ALTER TABLE `inventory_batches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventory_movements`
--

DROP TABLE IF EXISTS `inventory_movements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_movements` (
  `movement_id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `performed_by` bigint DEFAULT NULL,
  `movement_type` enum('STOCK_RECEIPT','POS_SALE','ONLINE_RESERVATION','RESERVATION_RELEASE','ONLINE_SALE','ADJUSTMENT','STOCKTAKE_ADJUSTMENT','BLOCK','UNBLOCK') NOT NULL,
  `on_hand_change` int NOT NULL DEFAULT '0',
  `reserved_change` int NOT NULL DEFAULT '0',
  `on_hand_before` int NOT NULL,
  `on_hand_after` int NOT NULL,
  `reserved_before` int NOT NULL,
  `reserved_after` int NOT NULL,
  `reference_type` varchar(50) DEFAULT NULL,
  `reference_id` bigint DEFAULT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`movement_id`),
  KEY `fk_inventory_movement_user` (`performed_by`),
  KEY `idx_inventory_movement_batch` (`batch_id`),
  KEY `idx_inventory_movement_type` (`movement_type`),
  KEY `idx_inventory_movement_reference` (`reference_type`,`reference_id`),
  KEY `idx_inventory_movement_created` (`created_at`),
  CONSTRAINT `fk_inventory_movement_batch` FOREIGN KEY (`batch_id`) REFERENCES `inventory_batches` (`batch_id`),
  CONSTRAINT `fk_inventory_movement_user` FOREIGN KEY (`performed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `chk_movement_on_hand_after` CHECK ((`on_hand_after` >= 0)),
  CONSTRAINT `chk_movement_on_hand_before` CHECK ((`on_hand_before` >= 0)),
  CONSTRAINT `chk_movement_reserved_after` CHECK ((`reserved_after` >= 0)),
  CONSTRAINT `chk_movement_reserved_before` CHECK ((`reserved_before` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_movements`
--

LOCK TABLES `inventory_movements` WRITE;
/*!40000 ALTER TABLE `inventory_movements` DISABLE KEYS */;
/*!40000 ALTER TABLE `inventory_movements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventory_reservations`
--

DROP TABLE IF EXISTS `inventory_reservations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_reservations` (
  `reservation_id` bigint NOT NULL AUTO_INCREMENT,
  `online_order_id` bigint NOT NULL,
  `online_order_item_id` bigint NOT NULL,
  `batch_id` bigint NOT NULL,
  `reserved_quantity` int NOT NULL,
  `status` enum('ACTIVE','RELEASED','FULFILLED') NOT NULL DEFAULT 'ACTIVE',
  `reserved_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `released_at` datetime DEFAULT NULL,
  PRIMARY KEY (`reservation_id`),
  KEY `fk_reservation_order_item` (`online_order_item_id`),
  KEY `idx_reservation_order` (`online_order_id`),
  KEY `idx_reservation_batch` (`batch_id`),
  KEY `idx_reservation_status` (`status`),
  CONSTRAINT `fk_reservation_batch` FOREIGN KEY (`batch_id`) REFERENCES `inventory_batches` (`batch_id`),
  CONSTRAINT `fk_reservation_order` FOREIGN KEY (`online_order_id`) REFERENCES `online_orders` (`online_order_id`),
  CONSTRAINT `fk_reservation_order_item` FOREIGN KEY (`online_order_item_id`) REFERENCES `online_order_items` (`online_order_item_id`),
  CONSTRAINT `chk_reservation_quantity` CHECK ((`reserved_quantity` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_reservations`
--

LOCK TABLES `inventory_reservations` WRITE;
/*!40000 ALTER TABLE `inventory_reservations` DISABLE KEYS */;
/*!40000 ALTER TABLE `inventory_reservations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `online_order_items`
--

DROP TABLE IF EXISTS `online_order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `online_order_items` (
  `online_order_item_id` bigint NOT NULL AUTO_INCREMENT,
  `online_order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  `selling_unit` varchar(100) NOT NULL,
  `unit_price` decimal(15,2) NOT NULL,
  `subtotal` decimal(15,2) NOT NULL,
  PRIMARY KEY (`online_order_item_id`),
  KEY `fk_online_order_item_order` (`online_order_id`),
  KEY `fk_online_order_item_product` (`product_id`),
  CONSTRAINT `fk_online_order_item_order` FOREIGN KEY (`online_order_id`) REFERENCES `online_orders` (`online_order_id`),
  CONSTRAINT `fk_online_order_item_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  CONSTRAINT `chk_online_order_item_price` CHECK ((`unit_price` >= 0)),
  CONSTRAINT `chk_online_order_item_quantity` CHECK ((`quantity` > 0)),
  CONSTRAINT `chk_online_order_item_subtotal` CHECK ((`subtotal` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `online_order_items`
--

LOCK TABLES `online_order_items` WRITE;
/*!40000 ALTER TABLE `online_order_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `online_order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `online_orders`
--

DROP TABLE IF EXISTS `online_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `online_orders` (
  `online_order_id` bigint NOT NULL AUTO_INCREMENT,
  `customer_id` bigint NOT NULL,
  `customer_name` varchar(150) NOT NULL,
  `customer_phone` varchar(30) NOT NULL,
  `province_city` varchar(100) NOT NULL,
  `district` varchar(100) NOT NULL,
  `ward` varchar(100) NOT NULL,
  `detailed_address` varchar(255) NOT NULL,
  `payment_method` enum('COD','ONLINE_PAYMENT') NOT NULL,
  `payment_status` enum('PENDING','PAID','FAILED','NOT_REQUIRED') NOT NULL DEFAULT 'PENDING',
  `total_amount` decimal(15,2) NOT NULL,
  `order_status` enum('PENDING','CONFIRMED','PREPARING','READY','SHIPPING','COMPLETED','CANCELLED','REJECTED') NOT NULL DEFAULT 'PENDING',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`online_order_id`),
  KEY `idx_online_order_customer` (`customer_id`),
  KEY `idx_online_order_status` (`order_status`),
  KEY `idx_online_order_created` (`created_at`),
  CONSTRAINT `fk_online_order_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer_profiles` (`customer_id`),
  CONSTRAINT `chk_online_order_total` CHECK ((`total_amount` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `online_orders`
--

LOCK TABLES `online_orders` WRITE;
/*!40000 ALTER TABLE `online_orders` DISABLE KEYS */;
/*!40000 ALTER TABLE `online_orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `online_payments`
--

DROP TABLE IF EXISTS `online_payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `online_payments` (
  `payment_id` bigint NOT NULL AUTO_INCREMENT,
  `online_order_id` bigint NOT NULL,
  `provider` varchar(100) DEFAULT NULL,
  `transaction_reference` varchar(150) DEFAULT NULL,
  `amount` decimal(15,2) NOT NULL,
  `payment_status` enum('PENDING','SUCCESS','FAILED') NOT NULL DEFAULT 'PENDING',
  `paid_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`payment_id`),
  KEY `idx_online_payment_order` (`online_order_id`),
  CONSTRAINT `fk_online_payment_order` FOREIGN KEY (`online_order_id`) REFERENCES `online_orders` (`online_order_id`),
  CONSTRAINT `chk_online_payment_amount` CHECK ((`amount` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `online_payments`
--

LOCK TABLES `online_payments` WRITE;
/*!40000 ALTER TABLE `online_payments` DISABLE KEYS */;
/*!40000 ALTER TABLE `online_payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `prescription_items`
--

DROP TABLE IF EXISTS `prescription_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prescription_items` (
  `prescription_item_id` bigint NOT NULL AUTO_INCREMENT,
  `prescription_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL,
  `drug_name` varchar(200) NOT NULL,
  `strength` varchar(100) DEFAULT NULL,
  `prescribed_quantity` int NOT NULL,
  `usage_instruction` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`prescription_item_id`),
  KEY `fk_prescription_item_prescription` (`prescription_id`),
  KEY `fk_prescription_item_product` (`product_id`),
  CONSTRAINT `fk_prescription_item_prescription` FOREIGN KEY (`prescription_id`) REFERENCES `prescriptions` (`prescription_id`),
  CONSTRAINT `fk_prescription_item_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  CONSTRAINT `chk_prescription_quantity` CHECK ((`prescribed_quantity` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `prescription_items`
--

LOCK TABLES `prescription_items` WRITE;
/*!40000 ALTER TABLE `prescription_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `prescription_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `prescriptions`
--

DROP TABLE IF EXISTS `prescriptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prescriptions` (
  `prescription_id` bigint NOT NULL AUTO_INCREMENT,
  `prescription_code` varchar(100) NOT NULL,
  `prescription_date` date NOT NULL,
  `healthcare_facility` varchar(200) NOT NULL,
  `prescriber` varchar(200) NOT NULL,
  `patient_name` varchar(150) NOT NULL,
  `validation_status` enum('VALID','INVALID') NOT NULL,
  `validated_by` bigint DEFAULT NULL,
  `validated_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`prescription_id`),
  UNIQUE KEY `uq_prescription_code` (`prescription_code`),
  KEY `fk_prescription_validator` (`validated_by`),
  CONSTRAINT `fk_prescription_validator` FOREIGN KEY (`validated_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `prescriptions`
--

LOCK TABLES `prescriptions` WRITE;
/*!40000 ALTER TABLE `prescriptions` DISABLE KEYS */;
/*!40000 ALTER TABLE `prescriptions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `product_id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL,
  `product_name` varchar(200) NOT NULL,
  `sku` varchar(100) NOT NULL,
  `barcode` varchar(100) DEFAULT NULL,
  `active_ingredient` varchar(255) DEFAULT NULL,
  `strength` varchar(100) DEFAULT NULL,
  `dosage_form` varchar(100) DEFAULT NULL,
  `manufacturer` varchar(200) DEFAULT NULL,
  `registration_number` varchar(100) DEFAULT NULL,
  `short_description` varchar(500) DEFAULT NULL,
  `indication` text,
  `usage_instruction` text,
  `warnings` text,
  `contraindications` text,
  `product_type` enum('OTC','RX','RESTRICTED') NOT NULL,
  `selling_unit` varchar(100) NOT NULL,
  `selling_price` decimal(15,2) NOT NULL,
  `online_sale_allowed` tinyint(1) NOT NULL DEFAULT '0',
  `status` enum('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`product_id`),
  UNIQUE KEY `uq_product_sku` (`sku`),
  UNIQUE KEY `uq_product_barcode` (`barcode`),
  KEY `idx_product_category` (`category_id`),
  KEY `idx_product_type` (`product_type`),
  KEY `idx_product_status` (`status`),
  KEY `idx_product_name` (`product_name`),
  CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`),
  CONSTRAINT `chk_product_price` CHECK ((`selling_price` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (1,1,'Paracetamol 500mg','PARA500','893000000001','Paracetamol','500mg','Tablet','PharmaTest Vietnam','REG-PARA-001','Common pain reliever and fever reducer containing paracetamol 500mg.','Used for temporary relief of mild to moderate pain such as headache, toothache, muscle pain and fever.','Use according to the product label or pharmacist/doctor instructions. Take with water and do not exceed the recommended daily dose.','Avoid taking together with other products containing paracetamol. Use with caution in people with liver problems or frequent alcohol use.','Do not use in patients with known hypersensitivity to paracetamol or any component of the product.','OTC','Box',25000.00,1,'ACTIVE','2026-10-02 02:04:52','2026-10-02 05:31:46'),(2,1,'Ibuprofen 200mg','IBU200','893000000002','Ibuprofen','200mg','Tablet','Health Pharma','REG-IBU-002','Non-steroidal anti-inflammatory medicine used for pain and fever relief.','Used for temporary relief of mild to moderate pain, inflammation and fever.','Take according to the product label or healthcare professional instructions. It is commonly taken with food or after meals to reduce stomach discomfort.','May cause stomach irritation. Use with caution in patients with a history of stomach ulcers, kidney disease, cardiovascular disease or asthma.','Do not use in patients with hypersensitivity to ibuprofen or other NSAIDs, or in certain patients with active gastrointestinal bleeding.','OTC','Box',35000.00,1,'ACTIVE','2026-10-02 02:04:52','2026-10-02 05:31:46'),(3,2,'Cold Relief Plus','COLD001','893000000003','Paracetamol + Chlorpheniramine','500mg + 2mg','Tablet','MediCare','REG-COLD-003','Combination medicine for temporary relief of common cold and flu symptoms.','Used to relieve symptoms such as fever, headache, runny nose and other common cold symptoms.','Use according to the package instructions. Take with water and follow the recommended dose and dosing interval.','May cause drowsiness depending on its ingredients. Avoid combining with other medicines containing paracetamol without professional advice.','Do not use in patients with hypersensitivity to any ingredient in the product.','OTC','Box',45000.00,1,'ACTIVE','2026-10-02 02:04:52','2026-10-02 05:31:46'),(4,3,'Omeprazole 20mg','OME20','893000000004','Omeprazole','20mg','Capsule','Gastro Pharma','REG-OME-004','Medicine that reduces stomach acid and is commonly used for acid-related digestive conditions.','Used for conditions associated with excessive stomach acid, such as acid reflux and certain gastric symptoms.','Use according to the prescribed or labelled instructions. It is commonly taken before meals.','Long-term or repeated use should be discussed with a healthcare professional. Persistent abdominal symptoms should be medically evaluated.','Do not use in patients with known hypersensitivity to omeprazole or related medicines.','OTC','Box',55000.00,1,'ACTIVE','2026-10-02 02:04:52','2026-10-02 05:31:46'),(5,4,'Vitamin C 500mg','VITC500','893000000005','Ascorbic Acid','500mg','Tablet','VitaHealth','REG-VITC-005','Vitamin C supplement containing ascorbic acid 500mg.','Used as a vitamin C supplement when dietary intake is insufficient or additional supplementation is appropriate.','Take according to the product label. May be taken with or after food.','Excessive intake may cause gastrointestinal discomfort. People with kidney stone risk or certain medical conditions should consult a healthcare professional.','Do not use in patients with known hypersensitivity to any component of the product.','OTC','Bottle',70000.00,1,'ACTIVE','2026-10-02 02:04:52','2026-10-02 05:31:46'),(6,4,'Calcium D3','CALD3','893000000006','Calcium Carbonate + Vitamin D3','500mg + 400IU','Tablet','VitaHealth','REG-CAL-006','Calcium and vitamin D3 supplement supporting normal bone and mineral health.','Used to supplement calcium and vitamin D when dietary intake is inadequate or supplementation is recommended.','Take according to the product label or healthcare professional recommendation, preferably with meals when appropriate.','Do not exceed the recommended dose. People with kidney disease, kidney stones or high blood calcium should seek professional advice before use.','Do not use in patients with hypercalcemia or known hypersensitivity to any component of the product.','OTC','Bottle',125000.00,1,'ACTIVE','2026-10-02 02:04:52','2026-10-02 05:31:46'),(7,5,'Amoxicillin 500mg','AMOX500','893000000007','Amoxicillin','500mg','Capsule','Antibiotic Pharma','REG-AMOX-007','Prescription antibiotic containing amoxicillin 500mg.','Used for susceptible bacterial infections when prescribed by a qualified healthcare professional.','Prescription medicine. Use only according to a doctor\'s prescription and complete the prescribed course unless instructed otherwise by a healthcare professional.','Do not self-medicate with antibiotics. Incorrect use may contribute to antibiotic resistance. Seek medical attention if an allergic reaction occurs.','Do not use in patients with known hypersensitivity to amoxicillin, penicillins or related beta-lactam antibiotics.','RX','Box',95000.00,0,'ACTIVE','2026-10-02 02:04:52','2026-10-02 05:31:46'),(8,5,'Cefuroxime 500mg','CEFU500','893000000008','Cefuroxime','500mg','Tablet','Antibiotic Pharma','REG-CEFU-008','Prescription cephalosporin antibiotic containing cefuroxime 500mg.','Used for susceptible bacterial infections when prescribed by a healthcare professional.','Prescription medicine. Use only as prescribed and follow the prescribed treatment duration.','Use only under professional supervision. Inform the prescriber about previous severe reactions to penicillin or cephalosporin antibiotics.','Do not use in patients with known hypersensitivity to cefuroxime or cephalosporin antibiotics.','RX','Box',180000.00,0,'ACTIVE','2026-10-02 02:04:52','2026-10-02 05:31:46'),(9,6,'Cetirizine 10mg','CET10','893000000009','Cetirizine','10mg','Tablet','AllergyCare','REG-CET-009','Antihistamine containing cetirizine 10mg for common allergy symptoms.','Used to relieve symptoms associated with allergic conditions such as sneezing, runny nose, itchy or watery eyes and certain allergic skin symptoms.','Use according to the package instructions or healthcare professional advice. Take with water and follow the recommended daily dose.','May cause drowsiness in some people. Use caution when driving or operating machinery until you know how the medicine affects you.','Do not use in patients with known hypersensitivity to cetirizine or any component of the product.','OTC','Box',40000.00,1,'ACTIVE','2026-10-02 02:04:52','2026-10-02 05:31:46'),(10,7,'Hydrocortisone Cream 1%','HYDRO1','893000000010','Hydrocortisone','1%','Cream','DermCare','REG-HYD-010','Topical hydrocortisone cream used for temporary relief of mild inflammatory skin symptoms.','Used externally for selected mild inflammatory or itchy skin conditions when appropriate.','Apply a thin layer to the affected area according to the product label or healthcare professional instructions. For external use only.','Avoid contact with eyes and avoid prolonged or extensive use without professional advice. Do not apply to infected or severely damaged skin unless instructed.','Do not use in patients with hypersensitivity to hydrocortisone or any component of the cream.','OTC','Tube',65000.00,1,'ACTIVE','2026-10-02 02:04:52','2026-10-02 05:31:46'),(11,1,'Restricted Pain Medicine','REST001','893000000011','Test Restricted Ingredient','10mg','Tablet','Controlled Pharma','REG-REST-011','Restricted test medicine used to demonstrate controlled-product behavior in PharmaFlow.','This is a demonstration product for testing restricted medicine handling in the system.','Use only according to applicable professional and regulatory requirements.','Restricted product. It must not be sold through the normal online purchase flow.','Refer to the approved product information and professional requirements before use.','RESTRICTED','Box',150000.00,0,'INACTIVE','2026-10-02 02:04:52','2026-10-02 05:31:46'),(12,2,'Discontinued Cold Medicine','DISC001','893000000012','Test Ingredient','100mg','Tablet','Old Pharma','REG-DISC-012','Discontinued demonstration cold medicine retained for historical and inactive-product testing.','Test data representing a previously available cold medicine.','This product is inactive and should not be supplied.','Discontinued/inactive product. It should not appear as available for purchase.','Not applicable for normal sale because the product is inactive.','OTC','Box',30000.00,1,'ACTIVE','2026-10-02 02:04:52','2026-10-02 05:31:46'),(13,6,'test','TEST01','893000000013','Test Ingredient','100mg','Tablet','Old Pharma','REG-DISC-012','Test product used for development and validation of PharmaFlow product management features.','Development test data only.','Not intended for real medicinal use.','Test record only.','Not intended for real medicinal use.','OTC','Box',120000.00,1,'INACTIVE','2026-10-02 04:32:32','2026-10-03 22:42:00');
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase_order_items`
--

DROP TABLE IF EXISTS `purchase_order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_order_items` (
  `purchase_order_item_id` bigint NOT NULL AUTO_INCREMENT,
  `purchase_order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `ordered_quantity` int NOT NULL,
  `received_quantity` int NOT NULL DEFAULT '0',
  `unit_cost` decimal(15,2) DEFAULT NULL,
  `subtotal` decimal(15,2) DEFAULT NULL,
  PRIMARY KEY (`purchase_order_item_id`),
  UNIQUE KEY `uq_purchase_order_product` (`purchase_order_id`,`product_id`),
  KEY `fk_purchase_order_item_product` (`product_id`),
  CONSTRAINT `fk_purchase_order_item_order` FOREIGN KEY (`purchase_order_id`) REFERENCES `purchase_orders` (`purchase_order_id`),
  CONSTRAINT `fk_purchase_order_item_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  CONSTRAINT `chk_po_ordered_quantity` CHECK ((`ordered_quantity` > 0)),
  CONSTRAINT `chk_po_received_quantity` CHECK (((`received_quantity` >= 0) and (`received_quantity` <= `ordered_quantity`))),
  CONSTRAINT `chk_po_subtotal` CHECK (((`subtotal` is null) or (`subtotal` >= 0))),
  CONSTRAINT `chk_po_unit_cost` CHECK (((`unit_cost` is null) or (`unit_cost` >= 0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_order_items`
--

LOCK TABLES `purchase_order_items` WRITE;
/*!40000 ALTER TABLE `purchase_order_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `purchase_order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase_orders`
--

DROP TABLE IF EXISTS `purchase_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_orders` (
  `purchase_order_id` bigint NOT NULL AUTO_INCREMENT,
  `supplier_id` bigint NOT NULL,
  `created_by` bigint NOT NULL,
  `order_date` date NOT NULL,
  `expected_delivery_date` date DEFAULT NULL,
  `status` enum('DRAFT','ORDERED','PARTIALLY_RECEIVED','RECEIVED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
  `source_type` enum('MANUAL','LOW_STOCK','AI_SUGGESTION') NOT NULL DEFAULT 'MANUAL',
  `total_amount` decimal(15,2) NOT NULL DEFAULT '0.00',
  `note` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`purchase_order_id`),
  KEY `fk_purchase_order_creator` (`created_by`),
  KEY `idx_purchase_order_supplier` (`supplier_id`),
  KEY `idx_purchase_order_status` (`status`),
  KEY `idx_purchase_order_date` (`order_date`),
  CONSTRAINT `fk_purchase_order_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_purchase_order_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `suppliers` (`supplier_id`),
  CONSTRAINT `chk_purchase_order_total` CHECK ((`total_amount` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_orders`
--

LOCK TABLES `purchase_orders` WRITE;
/*!40000 ALTER TABLE `purchase_orders` DISABLE KEYS */;
/*!40000 ALTER TABLE `purchase_orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `receipts`
--

DROP TABLE IF EXISTS `receipts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `receipts` (
  `receipt_id` bigint NOT NULL AUTO_INCREMENT,
  `sale_transaction_id` bigint NOT NULL,
  `issued_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`receipt_id`),
  UNIQUE KEY `uq_receipt_sale` (`sale_transaction_id`),
  CONSTRAINT `fk_receipt_sale` FOREIGN KEY (`sale_transaction_id`) REFERENCES `sale_transactions` (`sale_transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `receipts`
--

LOCK TABLES `receipts` WRITE;
/*!40000 ALTER TABLE `receipts` DISABLE KEYS */;
/*!40000 ALTER TABLE `receipts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `role_id` bigint NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uq_role_name` (`role_name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'OWNER_ADMIN','Pharmacy owner or administrator'),(2,'STAFF','Staff responsible mainly for POS sales'),(3,'SHIPPER','shipper'),(4,'CUSTOMER','Online pharmacy customer');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sale_item_batch_allocations`
--

DROP TABLE IF EXISTS `sale_item_batch_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sale_item_batch_allocations` (
  `allocation_id` bigint NOT NULL AUTO_INCREMENT,
  `sale_item_id` bigint NOT NULL,
  `batch_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  PRIMARY KEY (`allocation_id`),
  UNIQUE KEY `uq_sale_item_batch` (`sale_item_id`,`batch_id`),
  KEY `fk_sale_batch_allocation_batch` (`batch_id`),
  CONSTRAINT `fk_sale_batch_allocation_batch` FOREIGN KEY (`batch_id`) REFERENCES `inventory_batches` (`batch_id`),
  CONSTRAINT `fk_sale_batch_allocation_item` FOREIGN KEY (`sale_item_id`) REFERENCES `sale_items` (`sale_item_id`),
  CONSTRAINT `chk_sale_allocation_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sale_item_batch_allocations`
--

LOCK TABLES `sale_item_batch_allocations` WRITE;
/*!40000 ALTER TABLE `sale_item_batch_allocations` DISABLE KEYS */;
/*!40000 ALTER TABLE `sale_item_batch_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sale_items`
--

DROP TABLE IF EXISTS `sale_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sale_items` (
  `sale_item_id` bigint NOT NULL AUTO_INCREMENT,
  `sale_transaction_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(15,2) NOT NULL,
  `subtotal` decimal(15,2) NOT NULL,
  PRIMARY KEY (`sale_item_id`),
  KEY `fk_sale_item_sale` (`sale_transaction_id`),
  KEY `fk_sale_item_product` (`product_id`),
  CONSTRAINT `fk_sale_item_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  CONSTRAINT `fk_sale_item_sale` FOREIGN KEY (`sale_transaction_id`) REFERENCES `sale_transactions` (`sale_transaction_id`),
  CONSTRAINT `chk_sale_item_price` CHECK ((`unit_price` >= 0)),
  CONSTRAINT `chk_sale_item_quantity` CHECK ((`quantity` > 0)),
  CONSTRAINT `chk_sale_item_subtotal` CHECK ((`subtotal` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sale_items`
--

LOCK TABLES `sale_items` WRITE;
/*!40000 ALTER TABLE `sale_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `sale_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sale_transactions`
--

DROP TABLE IF EXISTS `sale_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sale_transactions` (
  `sale_transaction_id` bigint NOT NULL AUTO_INCREMENT,
  `staff_id` bigint NOT NULL,
  `prescription_id` bigint DEFAULT NULL,
  `payment_method` enum('CASH','BANK_TRANSFER','CARD') NOT NULL,
  `total_amount` decimal(15,2) NOT NULL,
  `status` enum('PENDING','COMPLETED','FAILED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  `sale_datetime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`sale_transaction_id`),
  KEY `fk_sale_prescription` (`prescription_id`),
  KEY `idx_sale_staff` (`staff_id`),
  KEY `idx_sale_status` (`status`),
  KEY `idx_sale_datetime` (`sale_datetime`),
  CONSTRAINT `fk_sale_prescription` FOREIGN KEY (`prescription_id`) REFERENCES `prescriptions` (`prescription_id`),
  CONSTRAINT `fk_sale_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_profiles` (`staff_id`),
  CONSTRAINT `chk_sale_total` CHECK ((`total_amount` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sale_transactions`
--

LOCK TABLES `sale_transactions` WRITE;
/*!40000 ALTER TABLE `sale_transactions` DISABLE KEYS */;
/*!40000 ALTER TABLE `sale_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `staff_profiles`
--

DROP TABLE IF EXISTS `staff_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `staff_profiles` (
  `staff_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `employee_code` varchar(50) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`staff_id`),
  UNIQUE KEY `uq_staff_user` (`user_id`),
  UNIQUE KEY `uq_staff_employee_code` (`employee_code`),
  CONSTRAINT `fk_staff_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `staff_profiles`
--

LOCK TABLES `staff_profiles` WRITE;
/*!40000 ALTER TABLE `staff_profiles` DISABLE KEYS */;
/*!40000 ALTER TABLE `staff_profiles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stocktake_items`
--

DROP TABLE IF EXISTS `stocktake_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stocktake_items` (
  `stocktake_item_id` bigint NOT NULL AUTO_INCREMENT,
  `stocktake_id` bigint NOT NULL,
  `batch_id` bigint NOT NULL,
  `system_quantity` int NOT NULL,
  `actual_quantity` int DEFAULT NULL,
  `difference_quantity` int DEFAULT NULL,
  PRIMARY KEY (`stocktake_item_id`),
  UNIQUE KEY `uq_stocktake_batch` (`stocktake_id`,`batch_id`),
  KEY `fk_stocktake_item_batch` (`batch_id`),
  CONSTRAINT `fk_stocktake_item_batch` FOREIGN KEY (`batch_id`) REFERENCES `inventory_batches` (`batch_id`),
  CONSTRAINT `fk_stocktake_item_stocktake` FOREIGN KEY (`stocktake_id`) REFERENCES `stocktakes` (`stocktake_id`),
  CONSTRAINT `chk_stocktake_actual_quantity` CHECK (((`actual_quantity` is null) or (`actual_quantity` >= 0))),
  CONSTRAINT `chk_stocktake_system_quantity` CHECK ((`system_quantity` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stocktake_items`
--

LOCK TABLES `stocktake_items` WRITE;
/*!40000 ALTER TABLE `stocktake_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `stocktake_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stocktakes`
--

DROP TABLE IF EXISTS `stocktakes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stocktakes` (
  `stocktake_id` bigint NOT NULL AUTO_INCREMENT,
  `created_by` bigint NOT NULL,
  `status` enum('DRAFT','IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'DRAFT',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `completed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`stocktake_id`),
  KEY `fk_stocktake_creator` (`created_by`),
  CONSTRAINT `fk_stocktake_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stocktakes`
--

LOCK TABLES `stocktakes` WRITE;
/*!40000 ALTER TABLE `stocktakes` DISABLE KEYS */;
/*!40000 ALTER TABLE `stocktakes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supplier_products`
--

DROP TABLE IF EXISTS `supplier_products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supplier_products` (
  `supplier_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `supplier_product_code` varchar(100) DEFAULT NULL,
  `last_cost_price` decimal(15,2) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`supplier_id`,`product_id`),
  KEY `fk_supplier_product_product` (`product_id`),
  CONSTRAINT `fk_supplier_product_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  CONSTRAINT `fk_supplier_product_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `suppliers` (`supplier_id`),
  CONSTRAINT `chk_supplier_product_cost` CHECK (((`last_cost_price` is null) or (`last_cost_price` >= 0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supplier_products`
--

LOCK TABLES `supplier_products` WRITE;
/*!40000 ALTER TABLE `supplier_products` DISABLE KEYS */;
INSERT INTO `supplier_products` VALUES (1,1,'MS-PARA500',16000.00,'2026-10-02 02:04:52','2026-10-02 02:04:52'),(1,2,'MS-IBU200',24000.00,'2026-10-02 02:04:52','2026-10-02 02:04:52'),(1,3,'MS-COLD001',30000.00,'2026-10-02 02:04:52','2026-10-02 02:04:52'),(1,10,'MS-HYDRO1',42000.00,'2026-10-02 02:04:52','2026-10-02 02:04:52'),(2,4,'HC-OME20',36000.00,'2026-10-02 02:04:52','2026-10-02 02:04:52'),(2,5,'HC-VITC500',48000.00,'2026-10-02 02:04:52','2026-10-02 02:04:52'),(2,6,'HC-CALD3',85000.00,'2026-10-02 02:04:52','2026-10-02 02:04:52'),(3,7,'PD-AMOX500',65000.00,'2026-10-02 02:04:52','2026-10-02 02:04:52'),(3,8,'PD-CEFU500',125000.00,'2026-10-02 02:04:52','2026-10-02 02:04:52'),(3,9,'PD-CET10',27000.00,'2026-10-02 02:04:52','2026-10-02 02:04:52');
/*!40000 ALTER TABLE `supplier_products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `suppliers`
--

DROP TABLE IF EXISTS `suppliers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `suppliers` (
  `supplier_id` bigint NOT NULL AUTO_INCREMENT,
  `supplier_name` varchar(200) NOT NULL,
  `contact_person` varchar(150) DEFAULT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `email` varchar(150) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `tax_business_info` varchar(255) DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`supplier_id`),
  KEY `idx_supplier_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `suppliers`
--

LOCK TABLES `suppliers` WRITE;
/*!40000 ALTER TABLE `suppliers` DISABLE KEYS */;
INSERT INTO `suppliers` VALUES (1,'MediSupply Vietnam','Nguyen Van Minh','0901000001','contact@medisupply.test','12 Nguyen Trai, Thanh Xuan, Hanoi','TAX-MSV-001','ACTIVE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(2,'HealthCare Distribution','Tran Thi Lan','0901000002','sales@healthcare.test','45 Cau Giay, Cau Giay, Hanoi','TAX-HCD-002','ACTIVE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(3,'Pharma Distribution JSC','Le Hoang Nam','0901000003','info@pharmadistribution.test','80 Le Van Luong, Hanoi','TAX-PDJ-003','ACTIVE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(4,'Old Supplier','Test Contact','0901000099','old@supplier.test','Hanoi','TAX-OLD-999','INACTIVE','2026-10-02 02:04:52','2026-10-02 02:04:52'),(5,'test','123132','0321333123','old@supplier.test','123123','test','ACTIVE','2026-10-02 05:41:20','2026-10-02 05:41:20');
/*!40000 ALTER TABLE `suppliers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `full_name` varchar(150) NOT NULL,
  `email` varchar(150) NOT NULL,
  `username` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uq_user_email` (`email`),
  UNIQUE KEY `uq_user_username` (`username`),
  KEY `idx_user_role` (`role_id`),
  KEY `idx_user_status` (`status`),
  CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,4,'tester','ab@gmail.com','tester','M6Ek/DlW0lWb9gl7Q31UJw==:C1mMTfRLw/Om9F9xLiX6V6ZIf++88akWiyqsQGFKdcs=','0321333123','ACTIVE','2026-10-02 03:23:00','2026-10-02 03:23:00'),(2,1,'admin','admin@gmail.com','admin','oE0/rPQiI7heNCMYTq9Y6A==:R+x3d0Ag6aSd6HnkeILUHmATMoWAG0Ru6y+EysmIXS0=','0321333123','ACTIVE','2026-10-02 03:49:58','2026-10-02 03:58:43'),(3,4,'tester1','laxosi9810@bitproy.com','tét01','bBGAlMq3wWxXAdihc2AMtw==:3Vbh1Iudt3a2UqdA0ordBigzH+6n5sKADXiMZmQYyDo=','0324192333','INACTIVE','2026-10-02 06:20:50','2026-10-02 06:20:50'),(4,4,'tester1','repewe3761@caps7.com','tét011','vVmyHqaYUbfcwiTvaPaWGQ==:/CKsXznb0/hZEb0q+zzfGdUGRq8B17zZG0D2nujyEUs=','0324192333','INACTIVE','2026-10-02 06:22:02','2026-10-02 06:22:02'),(5,4,'123132','vovit73479@caps7.com','test123','A9qUQC/i1QvU8KKClpxwaA==:Bea2vR3JmnW1dyDbcQPHwRX2OdfHwgRPZJiY2pj0m14=','0324192333','INACTIVE','2026-10-02 06:23:37','2026-10-02 06:23:37'),(6,4,'t123','bifop90335@caps7.com','t123','KU7HALZRppdwjvsBleLGhQ==:6+N1A0m5tNegpsalJmGd3n+sHTcTcJLp6mymSorqRiU=','0324192333','ACTIVE','2026-10-02 06:31:53','2026-10-02 06:41:32'),(7,4,'123','123@gmail.com','123','UuwtDPikUF6iVtBIVUkcCw==:AyjCp+dhG13YYpP7bIaCj8KjCbLOTmDpTo7aNuSxP7g=','123','INACTIVE','2026-10-03 22:35:36','2026-10-03 22:35:36');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `verification_tokens`
--

DROP TABLE IF EXISTS `verification_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `verification_tokens` (
  `token_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  `token_type` enum('VERIFY_EMAIL','RESET_PASSWORD') NOT NULL,
  `expires_at` datetime NOT NULL,
  `used_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`token_id`),
  KEY `idx_vt_user` (`user_id`),
  KEY `idx_vt_hash` (`token_hash`),
  CONSTRAINT `fk_vt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `verification_tokens`
--

LOCK TABLES `verification_tokens` WRITE;
/*!40000 ALTER TABLE `verification_tokens` DISABLE KEYS */;
INSERT INTO `verification_tokens` VALUES (1,3,'2b3c8fc72ed8dc55bee087fb3221082e559a9feae6baa2dcf8af424de8f821b5','VERIFY_EMAIL','2026-10-01 23:30:50',NULL,'2026-10-02 06:20:50'),(2,4,'731c94a3d34b85e8457f78266c0988e0f89d51842b89dd8930d0f484292656bb','VERIFY_EMAIL','2026-10-01 23:32:02',NULL,'2026-10-02 06:22:02'),(3,5,'6cb08b0dbd6b9243288514e0cc5ae9b95d86039dd75946a3705f75f3c9e0db85','VERIFY_EMAIL','2026-10-01 23:33:38','2026-10-02 06:24:11','2026-10-02 06:23:37'),(4,5,'64df9e0ddd4683a0602895316a11a0cc4f98116712de03b7bfd996822cd9f0ca','VERIFY_EMAIL','2026-10-01 23:34:11',NULL,'2026-10-02 06:24:11'),(5,6,'b2c48ad5185d6cf69ce016731022b82ad9c54303126d1bf45887bc00618a85b4','VERIFY_EMAIL','2026-10-02 06:46:53','2026-10-02 06:32:28','2026-10-02 06:31:53'),(6,6,'207e8417c575f10f10f96d8fdbe8f13cc65f6d0a682acf0934316bae82a4cf4e','RESET_PASSWORD','2026-10-02 06:49:19','2026-10-02 06:39:51','2026-10-02 06:34:19'),(7,6,'c2169cd88d1ca3e47d3fdd0b5d1a74f01c0ebdf80b2c052b37d53de496d3c0b7','RESET_PASSWORD','2026-10-02 06:54:51','2026-10-02 06:40:52','2026-10-02 06:39:51'),(8,6,'698f7ac2e01b2d10e2bb3cc6344f2b6814adcbdf657a4db9c7af50c01effd3fd','RESET_PASSWORD','2026-10-02 06:55:52','2026-10-02 06:41:32','2026-10-02 06:40:52'),(9,7,'48a75f018a480d21cb3c0a0cc7f9ced69dce8ec7f42ff0080b9d1ceb8d996f2c','VERIFY_EMAIL','2026-10-03 22:50:36',NULL,'2026-10-03 22:35:36');
/*!40000 ALTER TABLE `verification_tokens` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-05 16:37:26
