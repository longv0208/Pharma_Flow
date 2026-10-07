-- =====================================================================
-- PharmaFlow - DB TIENG VIET DAY DU (DU LIEU/MA NGHIEP VU)
-- Clone tu db.sql goc.
--
-- GIU NGUYEN:
--   - ten database/table/column/index/constraint/FK/PK
--   - kieu du lieu va quan he
--   - ID, SKU, barcode, email, username, password_hash, ma dang ky, ma thue
--
-- DA VIET HOA:
--   - tat ca enum/status/reason/payment/source/token type
--   - role_name
--   - reference_type seed data
--   - mo ta role, danh muc, don vi, dang bao che, vi tri ke
--   - cac mo ta san pham/demo text va audit/reason text trong seed
--
-- MA NGHIEP VU dung UPPER_SNAKE_CASE khong dau de code Java/SQL de on dinh.
-- Chuoi hien thi tu do dung tieng Viet co dau.
--
-- QUAN TRONG:
-- Code hien tai dang dung nhieu literal tieng Anh; can cap nhat dong bo code
-- truoc khi dung file nay lam DB runtime.
-- =====================================================================

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
  `status` enum('DANG_HOAT_DONG','DA_CHUYEN_THANH_DON','DA_XOA') NOT NULL DEFAULT 'DANG_HOAT_DONG',
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
  `status` enum('HOAT_DONG','NGUNG_HOAT_DONG') NOT NULL DEFAULT 'HOAT_DONG',
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
INSERT INTO `categories` VALUES (1,'Giảm đau và hạ sốt','Thuốc dùng để giảm đau và hạ sốt','HOAT_DONG','2026-10-02 02:04:52','2026-10-02 02:04:52'),(2,'Cảm lạnh và cúm','Thuốc dùng cho cảm lạnh, cúm, ho và các triệu chứng liên quan','HOAT_DONG','2026-10-02 02:04:52','2026-10-02 02:04:52'),(3,'Tiêu hóa','Thuốc dùng cho hệ tiêu hóa và các bệnh lý dạ dày','HOAT_DONG','2026-10-02 02:04:52','2026-10-02 02:04:52'),(4,'Vitamin và thực phẩm bổ sung','Các sản phẩm bổ sung vitamin và khoáng chất','HOAT_DONG','2026-10-02 02:04:52','2026-10-02 02:04:52'),(5,'Kháng sinh','Thuốc kháng sinh kê đơn','HOAT_DONG','2026-10-02 02:04:52','2026-10-02 02:04:52'),(6,'Dị ứng','Thuốc dùng để giảm các triệu chứng dị ứng','HOAT_DONG','2026-10-02 02:04:52','2026-10-02 02:04:52'),(7,'Da liễu','Sản phẩm dùng cho các vấn đề về da','HOAT_DONG','2026-10-02 02:04:52','2026-10-02 02:04:52'),(8,'Danh mục ngừng hoạt động','Danh mục dùng để kiểm thử trạng thái ngừng hoạt động','NGUNG_HOAT_DONG','2026-10-02 02:04:52','2026-10-02 02:04:52');
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
INSERT INTO `customer_profiles` VALUES (1,1,'kiểm thử','123','123','123','2026-10-02 03:23:00','2026-10-02 03:39:47'),(4,2,NULL,NULL,NULL,NULL,'2026-10-02 03:49:58','2026-10-02 03:49:58'),(5,3,NULL,NULL,NULL,NULL,'2026-10-02 06:20:50','2026-10-02 06:20:50'),(6,4,NULL,NULL,NULL,NULL,'2026-10-02 06:22:02','2026-10-02 06:22:02'),(7,5,NULL,NULL,NULL,NULL,'2026-10-02 06:23:37','2026-10-02 06:23:37'),(8,6,NULL,NULL,NULL,NULL,'2026-10-02 06:31:53','2026-10-02 06:31:53'),(9,7,NULL,NULL,NULL,NULL,'2026-10-03 22:35:36','2026-10-03 22:35:36');
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
  `inspection_result` enum('CHO_KIEM_TRA','CHAP_NHAN','TU_CHOI') NOT NULL DEFAULT 'CHO_KIEM_TRA',
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
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `goods_receipt_items`
--

LOCK TABLES `goods_receipt_items` WRITE;
/*!40000 ALTER TABLE `goods_receipt_items` DISABLE KEYS */;
INSERT INTO `goods_receipt_items` VALUES (2,1,2,7,15,'1','2026-10-10',11,65000.00,'CHAP_NHAN','','2026-10-06 13:03:38'),(5,2,3,8,16,'abv','2026-10-11',10,125000.00,'CHAP_NHAN','hết hạn','2026-10-06 21:59:56'),(9,3,4,6,17,'ABC111','2026-11-01',10,85000.00,'CHAP_NHAN','','2026-10-06 22:25:12'),(10,3,5,4,NULL,'ABB','2026-10-06',20,36000.00,'TU_CHOI','hết hạn','2026-10-06 22:25:12'),(11,3,6,5,18,'mmm','2026-11-07',100,48000.00,'CHAP_NHAN','','2026-10-06 22:25:12');
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
  `status` enum('BAN_NHAP','DA_XAC_NHAN','CHAP_NHAN_MOT_PHAN','DA_HUY') NOT NULL DEFAULT 'BAN_NHAP',
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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `goods_receipts`
--

LOCK TABLES `goods_receipts` WRITE;
/*!40000 ALTER TABLE `goods_receipts` DISABLE KEYS */;
INSERT INTO `goods_receipts` VALUES (1,3,1,2,'2026-10-05','123','1','DA_XAC_NHAN','2026-10-05 18:37:40','2026-10-06 13:03:38'),(2,3,2,2,'2026-10-06','22','123','DA_XAC_NHAN','2026-10-06 21:59:42','2026-10-06 21:59:56'),(3,2,3,2,'2026-10-06','kiểm thử 123','','CHAP_NHAN_MOT_PHAN','2026-10-06 22:24:46','2026-10-06 22:25:12');
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
  `reason` enum('HU_HONG','THAT_LAC','HET_HAN','DIEU_CHINH_KIEM_DEM','DIEU_CHINH_DU_LIEU','KHAC') NOT NULL,
  `note` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`adjustment_id`),
  KEY `fk_adjustment_batch` (`batch_id`),
  KEY `fk_adjustment_user` (`performed_by`),
  CONSTRAINT `fk_adjustment_batch` FOREIGN KEY (`batch_id`) REFERENCES `inventory_batches` (`batch_id`),
  CONSTRAINT `fk_adjustment_user` FOREIGN KEY (`performed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `chk_adjustment_after` CHECK ((`quantity_after` >= 0)),
  CONSTRAINT `chk_adjustment_before` CHECK ((`quantity_before` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_adjustments`
--

LOCK TABLES `inventory_adjustments` WRITE;
/*!40000 ALTER TABLE `inventory_adjustments` DISABLE KEYS */;
INSERT INTO `inventory_adjustments` VALUES (1,15,2,-5,11,6,'HU_HONG','123','2026-10-06 22:02:00'),(2,15,2,-2,6,4,'HET_HAN','hết hạn','2026-10-06 22:26:07');
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
INSERT INTO `inventory_alert_settings` VALUES (1,NULL,10,10,2,'2026-10-06 22:29:38'),(2,2,10,90,NULL,'2026-10-02 02:04:52');
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
  `status` enum('CO_SAN','SAP_HET_HAN','HET_HAN','BI_KHOA','HET_HANG') NOT NULL DEFAULT 'CO_SAN',
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
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_batches`
--

LOCK TABLES `inventory_batches` WRITE;
/*!40000 ALTER TABLE `inventory_batches` DISABLE KEYS */;
INSERT INTO `inventory_batches` VALUES (1,1,1,NULL,'PARA-2026-01','2028-04-02',100,10,16000.00,'Kệ A1','CO_SAN','2026-10-02 02:04:52','2026-10-02 02:04:52'),(2,1,1,NULL,'PARA-2026-02','2027-08-02',49,0,16500.00,'Kệ A1','CO_SAN','2026-10-02 02:04:52','2026-10-06 22:05:52'),(3,2,1,NULL,'IBU-2026-01','2028-01-02',4,1,24000.00,'Kệ A2','CO_SAN','2026-10-02 02:04:52','2026-10-06 22:31:01'),(4,3,1,NULL,'COLD-2026-01','2027-06-02',30,5,30000.00,'Kệ B1','CO_SAN','2026-10-02 02:04:52','2026-10-02 02:04:52'),(5,4,2,NULL,'OME-NEAR-01','2026-12-02',25,0,36000.00,'Kệ C1','SAP_HET_HAN','2026-10-02 02:04:52','2026-10-02 02:04:52'),(6,5,2,NULL,'VITC-2026-01','2028-06-02',80,12,48000.00,'Kệ D1','CO_SAN','2026-10-02 02:04:52','2026-10-02 02:04:52'),(7,6,2,NULL,'CAL-BLOCK-01','2027-10-02',30,0,85000.00,'Kệ D2','BI_KHOA','2026-10-02 02:04:52','2026-10-02 02:04:52'),(8,6,2,NULL,'CAL-VALID-02','2028-02-02',15,2,87000.00,'Kệ D2','CO_SAN','2026-10-02 02:04:52','2026-10-06 22:28:21'),(9,7,3,NULL,'AMOX-2026-01','2027-12-02',30,0,65000.00,'Kệ thuốc kê đơn RX1','CO_SAN','2026-10-02 02:04:52','2026-10-06 22:03:03'),(10,8,3,NULL,'CEFU-2026-01','2027-12-02',20,0,125000.00,'Kệ thuốc kê đơn RX1','CO_SAN','2026-10-02 02:04:52','2026-10-02 02:04:52'),(11,9,3,NULL,'CET-OLD-01','2026-09-02',20,0,27000.00,'Kệ E1','HET_HAN','2026-10-02 02:04:52','2026-10-06 22:28:21'),(12,9,3,NULL,'CET-NEW-02','2027-07-02',25,3,28000.00,'Kệ E1','CO_SAN','2026-10-02 02:04:52','2026-10-02 02:04:52'),(13,10,1,NULL,'HYDRO-2026-01','2027-08-02',0,0,42000.00,'Kệ F1','HET_HANG','2026-10-02 02:04:52','2026-10-02 02:04:52'),(14,11,1,NULL,'REST-2026-01','2027-10-02',10,0,100000.00,'Kệ thuốc hạn chế','CO_SAN','2026-10-02 02:04:52','2026-10-02 02:04:52'),(15,7,3,1,'1','2026-10-10',4,0,65000.00,NULL,'SAP_HET_HAN','2026-10-06 13:03:38','2026-10-06 22:26:07'),(16,8,3,2,'abv','2026-10-11',10,0,125000.00,NULL,'SAP_HET_HAN','2026-10-06 21:59:56','2026-10-06 21:59:56'),(17,6,2,3,'ABC111','2026-11-01',10,0,85000.00,NULL,'SAP_HET_HAN','2026-10-06 22:25:12','2026-10-06 22:25:12'),(18,5,2,3,'mmm','2026-11-07',100,0,48000.00,NULL,'SAP_HET_HAN','2026-10-06 22:25:12','2026-10-06 22:25:12');
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
  `movement_type` enum('NHAP_KHO','BAN_TAI_QUAY','GIU_HANG_ONLINE','GIAI_PHONG_GIU_HANG','BAN_ONLINE','DIEU_CHINH','DIEU_CHINH_KIEM_KE','KHOA','MO_KHOA') NOT NULL,
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
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_movements`
--

LOCK TABLES `inventory_movements` WRITE;
/*!40000 ALTER TABLE `inventory_movements` DISABLE KEYS */;
INSERT INTO `inventory_movements` VALUES (1,15,2,'NHAP_KHO',11,0,0,11,0,0,'PHIEU_NHAP_KHO',1,'Phiếu nhập kho #1','2026-10-06 13:03:38'),(2,16,2,'NHAP_KHO',10,0,0,10,0,0,'PHIEU_NHAP_KHO',2,'Phiếu nhập kho #2','2026-10-06 21:59:56'),(3,15,2,'KHOA',0,0,11,11,0,0,'LO_HANG',15,'kiểm tra','2026-10-06 22:00:54'),(4,15,2,'MO_KHOA',0,0,11,11,0,0,'LO_HANG',15,'đã kiểm tra xong','2026-10-06 22:01:01'),(5,15,2,'DIEU_CHINH',-5,0,11,6,0,0,'DIEU_CHINH_TON_KHO',1,'Hư hỏng - 123','2026-10-06 22:02:00'),(6,9,2,'DIEU_CHINH_KIEM_KE',-10,0,40,30,0,0,'KIEM_KE',1,'Đối soát kiểm kê #1','2026-10-06 22:03:03'),(7,2,8,'BAN_TAI_QUAY',-1,0,50,49,0,0,'BAN_TAI_QUAY',1,'Bán tại quầy #1','2026-10-06 22:05:52'),(8,17,2,'NHAP_KHO',10,0,0,10,0,0,'PHIEU_NHAP_KHO',3,'Phiếu nhập kho #3','2026-10-06 22:25:12'),(9,18,2,'NHAP_KHO',100,0,0,100,0,0,'PHIEU_NHAP_KHO',3,'Phiếu nhập kho #3','2026-10-06 22:25:12'),(10,15,2,'DIEU_CHINH',-2,0,6,4,0,0,'DIEU_CHINH_TON_KHO',2,'Hết hạn - hết hạn','2026-10-06 22:26:07'),(11,8,2,'DIEU_CHINH_KIEM_KE',-5,0,20,15,2,2,'KIEM_KE',2,'Đối soát kiểm kê #2','2026-10-06 22:28:21'),(12,11,2,'DIEU_CHINH_KIEM_KE',5,0,15,20,0,0,'KIEM_KE',2,'Đối soát kiểm kê #2','2026-10-06 22:28:21'),(13,3,8,'BAN_TAI_QUAY',-2,0,6,4,1,1,'BAN_TAI_QUAY',2,'Bán tại quầy #2','2026-10-06 22:31:01');
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
  `status` enum('DANG_GIU','DA_GIAI_PHONG','DA_HOAN_TAT') NOT NULL DEFAULT 'DANG_GIU',
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
  `payment_method` enum('THANH_TOAN_KHI_NHAN_HANG','THANH_TOAN_TRUC_TUYEN') NOT NULL,
  `payment_status` enum('CHO_THANH_TOAN','DA_THANH_TOAN','THAT_BAI','KHONG_YEU_CAU') NOT NULL DEFAULT 'CHO_THANH_TOAN',
  `total_amount` decimal(15,2) NOT NULL,
  `order_status` enum('CHO_XU_LY','DA_XAC_NHAN','DANG_CHUAN_BI','SAN_SANG','DANG_GIAO','HOAN_TAT','DA_HUY','TU_CHOI') NOT NULL DEFAULT 'CHO_XU_LY',
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
  `payment_status` enum('CHO_XU_LY','THANH_CONG','THAT_BAI') NOT NULL DEFAULT 'CHO_XU_LY',
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
-- Table structure for table `prescriptions`
--

DROP TABLE IF EXISTS `prescriptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prescriptions` (
  `prescription_id` bigint NOT NULL AUTO_INCREMENT,
  `healthcare_facility` varchar(200) NOT NULL,
  `prescriber` varchar(200) NOT NULL,
  `validated_by` bigint DEFAULT NULL,
  `validated_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`prescription_id`),
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
  `product_type` enum('KHONG_KE_DON','KE_DON','HAN_CHE') NOT NULL,
  `selling_unit` varchar(100) NOT NULL,
  `selling_price` decimal(15,2) NOT NULL,
  `online_sale_allowed` tinyint(1) NOT NULL DEFAULT '0',
  `status` enum('HOAT_DONG','NGUNG_HOAT_DONG') NOT NULL DEFAULT 'HOAT_DONG',
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
INSERT INTO `products` VALUES (1,1,'Paracetamol 500mg','PARA500','893000000001','Paracetamol','500mg','Viên nén','PharmaTest Vietnam','REG-PARA-001','Thuốc giảm đau, hạ sốt thông dụng chứa paracetamol 500mg.','Dùng để giảm tạm thời các cơn đau nhẹ đến vừa như đau đầu, đau răng, đau cơ và hạ sốt.','Dùng theo hướng dẫn trên nhãn hoặc theo chỉ dẫn của dược sĩ/bác sĩ. Uống với nước và không vượt quá liều khuyến cáo hằng ngày.','Tránh dùng đồng thời với các sản phẩm khác có chứa paracetamol. Thận trọng ở người có vấn đề về gan hoặc thường xuyên sử dụng rượu bia.','Không dùng cho người mẫn cảm với paracetamol hoặc bất kỳ thành phần nào của sản phẩm.','KHONG_KE_DON','Hộp',25000.00,1,'HOAT_DONG','2026-10-02 02:04:52','2026-10-02 05:31:46'),(2,1,'Ibuprofen 200mg','IBU200','893000000002','Ibuprofen','200mg','Viên nén','Health Pharma','REG-IBU-002','Thuốc chống viêm không steroid dùng để giảm đau và hạ sốt.','Dùng để giảm tạm thời đau nhẹ đến vừa, viêm và sốt.','Dùng theo hướng dẫn trên nhãn hoặc của nhân viên y tế. Thường dùng cùng thức ăn hoặc sau bữa ăn để giảm khó chịu ở dạ dày.','Có thể gây kích ứng dạ dày. Thận trọng ở người có tiền sử loét dạ dày, bệnh thận, bệnh tim mạch hoặc hen.','Không dùng cho người mẫn cảm với ibuprofen hoặc các NSAID khác, hoặc một số trường hợp đang xuất huyết tiêu hóa.','KHONG_KE_DON','Hộp',35000.00,1,'HOAT_DONG','2026-10-02 02:04:52','2026-10-02 05:31:46'),(3,2,'Thuốc cảm cúm tổng hợp','COLD001','893000000003','Paracetamol + Chlorpheniramine','500mg + 2mg','Viên nén','MediCare','REG-COLD-003','Thuốc phối hợp giúp giảm tạm thời các triệu chứng cảm lạnh và cúm.','Dùng để giảm các triệu chứng như sốt, đau đầu, sổ mũi và các triệu chứng cảm lạnh thông thường khác.','Dùng theo hướng dẫn trên bao bì. Uống với nước và tuân thủ liều cũng như khoảng cách giữa các lần dùng.','Có thể gây buồn ngủ tùy thành phần. Tránh dùng cùng thuốc khác chứa paracetamol nếu chưa có tư vấn chuyên môn.','Không dùng cho người mẫn cảm với bất kỳ thành phần nào của sản phẩm.','KHONG_KE_DON','Hộp',45000.00,1,'HOAT_DONG','2026-10-02 02:04:52','2026-10-02 05:31:46'),(4,3,'Omeprazole 20mg','OME20','893000000004','Omeprazole','20mg','Viên nang','Gastro Pharma','REG-OME-004','Thuốc làm giảm acid dạ dày, thường dùng cho các bệnh lý tiêu hóa liên quan đến tăng tiết acid.','Dùng trong các tình trạng liên quan đến tăng acid dạ dày như trào ngược acid và một số triệu chứng dạ dày.','Dùng theo chỉ định hoặc hướng dẫn trên nhãn. Thường được dùng trước bữa ăn.','Nếu cần dùng kéo dài hoặc lặp lại, nên trao đổi với nhân viên y tế. Các triệu chứng bụng kéo dài cần được thăm khám.','Không dùng cho người mẫn cảm với omeprazole hoặc các thuốc liên quan.','KHONG_KE_DON','Hộp',55000.00,1,'HOAT_DONG','2026-10-02 02:04:52','2026-10-02 05:31:46'),(5,4,'Vitamin C 500mg','VITC500','893000000005','Ascorbic Acid','500mg','Viên nén','VitaHealth','REG-VITC-005','Sản phẩm bổ sung vitamin C chứa acid ascorbic 500mg.','Dùng để bổ sung vitamin C khi khẩu phần ăn không đáp ứng đủ hoặc khi cần bổ sung thêm.','Dùng theo hướng dẫn trên nhãn sản phẩm. Có thể dùng cùng hoặc sau bữa ăn.','Dùng quá mức có thể gây khó chịu đường tiêu hóa. Người có nguy cơ sỏi thận hoặc một số bệnh lý nên tham khảo ý kiến nhân viên y tế.','Không dùng cho người mẫn cảm với bất kỳ thành phần nào của sản phẩm.','KHONG_KE_DON','Chai',70000.00,1,'HOAT_DONG','2026-10-02 02:04:52','2026-10-02 05:31:46'),(6,4,'Calcium D3','CALD3','893000000006','Calcium Carbonate + Vitamin D3','500mg + 400IU','Viên nén','VitaHealth','REG-CAL-006','Sản phẩm bổ sung canxi và vitamin D3, hỗ trợ sức khỏe xương và khoáng chất.','Dùng để bổ sung canxi và vitamin D khi khẩu phần ăn không đủ hoặc khi được khuyến nghị bổ sung.','Dùng theo hướng dẫn trên nhãn hoặc khuyến nghị của nhân viên y tế, ưu tiên dùng cùng bữa ăn khi phù hợp.','Không vượt quá liều khuyến cáo. Người mắc bệnh thận, sỏi thận hoặc tăng canxi máu nên tham khảo ý kiến chuyên môn trước khi dùng.','Không dùng cho người tăng canxi máu hoặc mẫn cảm với bất kỳ thành phần nào của sản phẩm.','KHONG_KE_DON','Chai',125000.00,1,'HOAT_DONG','2026-10-02 02:04:52','2026-10-02 05:31:46'),(7,5,'Amoxicillin 500mg','AMOX500','893000000007','Amoxicillin','500mg','Viên nang','Antibiotic Pharma','REG-AMOX-007','Thuốc kháng sinh kê đơn chứa amoxicillin 500mg.','Dùng điều trị các nhiễm khuẩn nhạy cảm khi được nhân viên y tế có chuyên môn kê đơn.','Thuốc kê đơn. Chỉ sử dụng theo đơn của bác sĩ và hoàn thành liệu trình được kê, trừ khi có chỉ định khác từ nhân viên y tế.','Không tự ý sử dụng kháng sinh. Dùng sai có thể góp phần gây kháng kháng sinh. Cần đi khám nếu xuất hiện phản ứng dị ứng.','Không dùng cho người mẫn cảm với amoxicillin, penicillin hoặc các kháng sinh beta-lactam liên quan.','KE_DON','Hộp',95000.00,0,'HOAT_DONG','2026-10-02 02:04:52','2026-10-02 05:31:46'),(8,5,'Cefuroxime 500mg','CEFU500','893000000008','Cefuroxime','500mg','Viên nén','Antibiotic Pharma','REG-CEFU-008','Thuốc kháng sinh cephalosporin kê đơn chứa cefuroxime 500mg.','Dùng điều trị các nhiễm khuẩn nhạy cảm khi được nhân viên y tế kê đơn.','Thuốc kê đơn. Chỉ sử dụng theo đơn và tuân thủ thời gian điều trị đã được kê.','Chỉ sử dụng dưới sự giám sát chuyên môn. Hãy thông báo cho người kê đơn nếu từng có phản ứng nặng với penicillin hoặc kháng sinh cephalosporin.','Không dùng cho người mẫn cảm với cefuroxime hoặc kháng sinh cephalosporin.','KE_DON','Hộp',180000.00,0,'HOAT_DONG','2026-10-02 02:04:52','2026-10-02 05:31:46'),(9,6,'Cetirizine 10mg','CET10','893000000009','Cetirizine','10mg','Viên nén','AllergyCare','REG-CET-009','Thuốc kháng histamin chứa cetirizine 10mg dùng cho các triệu chứng dị ứng thông thường.','Dùng để giảm các triệu chứng dị ứng như hắt hơi, sổ mũi, ngứa/chảy nước mắt và một số biểu hiện dị ứng trên da.','Dùng theo hướng dẫn trên bao bì hoặc tư vấn của nhân viên y tế. Uống với nước và tuân thủ liều khuyến cáo hằng ngày.','Có thể gây buồn ngủ ở một số người. Thận trọng khi lái xe hoặc vận hành máy móc cho đến khi biết thuốc ảnh hưởng đến bạn như thế nào.','Không dùng cho người mẫn cảm với cetirizine hoặc bất kỳ thành phần nào của sản phẩm.','KHONG_KE_DON','Hộp',40000.00,1,'HOAT_DONG','2026-10-02 02:04:52','2026-10-02 05:31:46'),(10,7,'Hydrocortisone Cream 1%','HYDRO1','893000000010','Hydrocortisone','1%','Kem bôi','DermCare','REG-HYD-010','Kem hydrocortisone bôi ngoài da dùng để giảm tạm thời các triệu chứng viêm da nhẹ.','Dùng ngoài da cho một số tình trạng viêm nhẹ hoặc ngứa da khi phù hợp.','Bôi một lớp mỏng lên vùng da bị ảnh hưởng theo hướng dẫn trên nhãn hoặc của nhân viên y tế. Chỉ dùng ngoài da.','Tránh tiếp xúc với mắt và tránh sử dụng kéo dài hoặc trên diện rộng nếu chưa có tư vấn chuyên môn. Không bôi lên vùng da nhiễm trùng hoặc tổn thương nặng nếu chưa được chỉ định.','Không dùng cho người mẫn cảm với hydrocortisone hoặc bất kỳ thành phần nào của kem.','KHONG_KE_DON','Tuýp',65000.00,1,'HOAT_DONG','2026-10-02 02:04:52','2026-10-02 05:31:46'),(11,1,'Thuốc giảm đau hạn chế','REST001','893000000011','Test Restricted Ingredient','10mg','Viên nén','Controlled Pharma','REG-REST-011','Thuốc hạn chế dùng để minh họa cách hệ thống PharmaFlow xử lý sản phẩm được kiểm soát.','Đây là sản phẩm minh họa dùng để kiểm thử việc xử lý thuốc hạn chế trong hệ thống.','Chỉ sử dụng theo các yêu cầu chuyên môn và quy định áp dụng.','Sản phẩm hạn chế. Không được bán qua luồng mua hàng trực tuyến thông thường.','Tham khảo thông tin sản phẩm được phê duyệt và yêu cầu chuyên môn trước khi sử dụng.','HAN_CHE','Hộp',150000.00,0,'NGUNG_HOAT_DONG','2026-10-02 02:04:52','2026-10-02 05:31:46'),(12,2,'Thuốc cảm đã ngừng kinh doanh','DISC001','893000000012','Test Ingredient','100mg','Viên nén','Old Pharma','REG-DISC-012','Thuốc cảm minh họa đã ngừng kinh doanh, được giữ lại để kiểm thử lịch sử và sản phẩm ngừng hoạt động.','Dữ liệu kiểm thử đại diện cho một thuốc cảm trước đây từng được kinh doanh.','Sản phẩm này đã ngừng hoạt động và không được cung cấp.','Sản phẩm đã ngừng kinh doanh/ngừng hoạt động. Không được hiển thị là có thể mua.','Không áp dụng cho bán hàng thông thường vì sản phẩm đang ngừng hoạt động.','KHONG_KE_DON','Hộp',30000.00,1,'HOAT_DONG','2026-10-02 02:04:52','2026-10-02 05:31:46'),(13,6,'kiểm thử','TEST01','893000000013','Test Ingredient','100mg','Viên nén','Old Pharma','REG-DISC-012','Sản phẩm kiểm thử dùng để phát triển và xác nhận các chức năng quản lý sản phẩm của PharmaFlow.','Chỉ là dữ liệu kiểm thử phát triển.','Không dùng cho mục đích sử dụng thuốc thực tế.','Chỉ là bản ghi kiểm thử.','Không dùng cho mục đích sử dụng thuốc thực tế.','KHONG_KE_DON','Hộp',120000.00,1,'NGUNG_HOAT_DONG','2026-10-02 04:32:32','2026-10-03 22:42:00');
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
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_order_items`
--

LOCK TABLES `purchase_order_items` WRITE;
/*!40000 ALTER TABLE `purchase_order_items` DISABLE KEYS */;
INSERT INTO `purchase_order_items` VALUES (2,1,7,19,11,65000.00,1235000.00),(3,2,8,20,10,125000.00,2500000.00),(4,3,6,50,10,85000.00,4250000.00),(5,3,4,20,0,36000.00,720000.00),(6,3,5,100,100,48000.00,4800000.00);
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
  `status` enum('BAN_NHAP','DA_DAT_HANG','DA_NHAN_MOT_PHAN','DA_NHAN_DU','DA_HUY') NOT NULL DEFAULT 'BAN_NHAP',
  `source_type` enum('THU_CONG','TON_KHO_THAP','GOI_Y_AI') NOT NULL DEFAULT 'THU_CONG',
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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_orders`
--

LOCK TABLES `purchase_orders` WRITE;
/*!40000 ALTER TABLE `purchase_orders` DISABLE KEYS */;
INSERT INTO `purchase_orders` VALUES (1,3,2,'2026-10-06','2026-10-09','DA_NHAN_MOT_PHAN','THU_CONG',1235000.00,'đơn mua kiểm thử','2026-10-05 17:05:12','2026-10-06 13:03:38'),(2,3,2,'2026-10-07','2026-10-09','DA_NHAN_MOT_PHAN','THU_CONG',2500000.00,'123','2026-10-06 21:58:43','2026-10-06 21:59:56'),(3,2,2,'2026-10-06','2026-10-11','DA_NHAN_MOT_PHAN','THU_CONG',9770000.00,'123','2026-10-06 22:23:09','2026-10-06 22:25:12');
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
INSERT INTO `roles` VALUES (1,'CHU_QUAN_QUAN_TRI','Chủ nhà thuốc hoặc quản trị viên'),(2,'NHAN_VIEN','Nhân viên phụ trách bán hàng tại quầy và nghiệp vụ vận hành'),(3,'NHAN_VIEN_GIAO_HANG','Nhân viên giao hàng'),(4,'KHACH_HANG','Khách hàng mua thuốc trực tuyến');
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sale_item_batch_allocations`
--

LOCK TABLES `sale_item_batch_allocations` WRITE;
/*!40000 ALTER TABLE `sale_item_batch_allocations` DISABLE KEYS */;
INSERT INTO `sale_item_batch_allocations` VALUES (1,1,2,1),(2,2,3,2);
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sale_items`
--

LOCK TABLES `sale_items` WRITE;
/*!40000 ALTER TABLE `sale_items` DISABLE KEYS */;
INSERT INTO `sale_items` VALUES (1,1,1,1,25000.00,25000.00),(2,2,2,2,35000.00,70000.00);
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
  `payment_method` enum('TIEN_MAT','CHUYEN_KHOAN','THE') NOT NULL,
  `total_amount` decimal(15,2) NOT NULL,
  `status` enum('CHO_XU_LY','HOAN_TAT','THAT_BAI','DA_HUY') NOT NULL DEFAULT 'CHO_XU_LY',
  `sale_datetime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`sale_transaction_id`),
  KEY `fk_sale_prescription` (`prescription_id`),
  KEY `idx_sale_staff` (`staff_id`),
  KEY `idx_sale_status` (`status`),
  KEY `idx_sale_datetime` (`sale_datetime`),
  CONSTRAINT `fk_sale_prescription` FOREIGN KEY (`prescription_id`) REFERENCES `prescriptions` (`prescription_id`),
  CONSTRAINT `fk_sale_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_profiles` (`staff_id`),
  CONSTRAINT `chk_sale_total` CHECK ((`total_amount` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sale_transactions`
--

LOCK TABLES `sale_transactions` WRITE;
/*!40000 ALTER TABLE `sale_transactions` DISABLE KEYS */;
INSERT INTO `sale_transactions` VALUES (1,1,NULL,'TIEN_MAT',25000.00,'HOAN_TAT','2026-10-06 22:05:52'),(2,1,NULL,'TIEN_MAT',70000.00,'HOAN_TAT','2026-10-06 22:31:01');
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `staff_profiles`
--

LOCK TABLES `staff_profiles` WRITE;
/*!40000 ALTER TABLE `staff_profiles` DISABLE KEYS */;
INSERT INTO `staff_profiles` VALUES (1,8,'STAFF001','2026-10-06 21:48:45','2026-10-06 21:48:45');
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
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stocktake_items`
--

LOCK TABLES `stocktake_items` WRITE;
/*!40000 ALTER TABLE `stocktake_items` DISABLE KEYS */;
INSERT INTO `stocktake_items` VALUES (1,1,1,100,100,0),(2,1,2,50,50,0),(3,1,3,6,6,0),(4,1,4,30,30,0),(5,1,5,25,25,0),(6,1,6,80,80,0),(7,1,7,30,30,0),(8,1,8,20,20,0),(9,1,9,40,30,-10),(10,1,10,20,20,0),(11,1,11,15,15,0),(12,1,12,25,25,0),(13,1,13,0,0,0),(14,1,14,10,10,0),(15,1,15,6,6,0),(16,1,16,10,10,0),(17,2,1,100,100,0),(18,2,2,49,49,0),(19,2,3,6,6,0),(20,2,4,30,30,0),(21,2,5,25,25,0),(22,2,6,80,80,0),(23,2,7,30,30,0),(24,2,8,20,15,-5),(25,2,9,30,30,0),(26,2,10,20,20,0),(27,2,11,15,20,5),(28,2,12,25,25,0),(29,2,13,0,0,0),(30,2,14,10,10,0),(31,2,15,4,4,0),(32,2,16,10,10,0),(33,2,17,10,10,0),(34,2,18,100,100,0);
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
  `status` enum('BAN_NHAP','DANG_KIEM_KE','HOAN_TAT') NOT NULL DEFAULT 'BAN_NHAP',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `completed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`stocktake_id`),
  KEY `fk_stocktake_creator` (`created_by`),
  CONSTRAINT `fk_stocktake_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stocktakes`
--

LOCK TABLES `stocktakes` WRITE;
/*!40000 ALTER TABLE `stocktakes` DISABLE KEYS */;
INSERT INTO `stocktakes` VALUES (1,2,'HOAN_TAT','2026-10-06 22:02:16','2026-10-06 22:03:03'),(2,2,'HOAN_TAT','2026-10-06 22:27:26','2026-10-06 22:28:21');
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
  `status` enum('HOAT_DONG','NGUNG_HOAT_DONG') NOT NULL DEFAULT 'HOAT_DONG',
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
INSERT INTO `suppliers` VALUES (1,'MediSupply Việt Nam','Nguyễn Văn Minh','0901000001','contact@medisupply.test','12 Nguyễn Trãi, Thanh Xuân, Hà Nội','TAX-MSV-001','HOAT_DONG','2026-10-02 02:04:52','2026-10-02 02:04:52'),(2,'Phân phối HealthCare','Trần Thị Lan','0901000002','sales@healthcare.test','45 Cầu Giấy, Cầu Giấy, Hà Nội','TAX-HCD-002','HOAT_DONG','2026-10-02 02:04:52','2026-10-02 02:04:52'),(3,'Công ty Cổ phần Phân phối Dược','Lê Hoàng Nam','0901000003','info@pharmadistribution.test','80 Lê Văn Lương, Hà Nội','TAX-PDJ-003','HOAT_DONG','2026-10-02 02:04:52','2026-10-02 02:04:52'),(4,'Nhà cung cấp cũ','Liên hệ kiểm thử','0901000099','old@supplier.test','Hà Nội','TAX-OLD-999','NGUNG_HOAT_DONG','2026-10-02 02:04:52','2026-10-02 02:04:52'),(5,'kiểm thử','123132','0321333123','old@supplier.test','123123','kiểm thử','HOAT_DONG','2026-10-02 05:41:20','2026-10-02 05:41:20');
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
  `status` enum('HOAT_DONG','NGUNG_HOAT_DONG') NOT NULL DEFAULT 'HOAT_DONG',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uq_user_email` (`email`),
  UNIQUE KEY `uq_user_username` (`username`),
  KEY `idx_user_role` (`role_id`),
  KEY `idx_user_status` (`status`),
  CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,4,'tester','ab@gmail.com','tester','M6Ek/DlW0lWb9gl7Q31UJw==:C1mMTfRLw/Om9F9xLiX6V6ZIf++88akWiyqsQGFKdcs=','0321333123','HOAT_DONG','2026-10-02 03:23:00','2026-10-02 03:23:00'),(2,1,'admin','admin@gmail.com','admin','oE0/rPQiI7heNCMYTq9Y6A==:R+x3d0Ag6aSd6HnkeILUHmATMoWAG0Ru6y+EysmIXS0=','0321333123','HOAT_DONG','2026-10-02 03:49:58','2026-10-02 03:58:43'),(3,4,'tester1','laxosi9810@bitproy.com','tét01','bBGAlMq3wWxXAdihc2AMtw==:3Vbh1Iudt3a2UqdA0ordBigzH+6n5sKADXiMZmQYyDo=','0324192333','NGUNG_HOAT_DONG','2026-10-02 06:20:50','2026-10-02 06:20:50'),(4,4,'tester1','repewe3761@caps7.com','tét011','vVmyHqaYUbfcwiTvaPaWGQ==:/CKsXznb0/hZEb0q+zzfGdUGRq8B17zZG0D2nujyEUs=','0324192333','NGUNG_HOAT_DONG','2026-10-02 06:22:02','2026-10-02 06:22:02'),(5,4,'123132','vovit73479@caps7.com','test123','A9qUQC/i1QvU8KKClpxwaA==:Bea2vR3JmnW1dyDbcQPHwRX2OdfHwgRPZJiY2pj0m14=','0324192333','NGUNG_HOAT_DONG','2026-10-02 06:23:37','2026-10-02 06:23:37'),(6,4,'t123','bifop90335@caps7.com','t123','KU7HALZRppdwjvsBleLGhQ==:6+N1A0m5tNegpsalJmGd3n+sHTcTcJLp6mymSorqRiU=','0324192333','HOAT_DONG','2026-10-02 06:31:53','2026-10-02 06:41:32'),(7,4,'123','123@gmail.com','123','UuwtDPikUF6iVtBIVUkcCw==:AyjCp+dhG13YYpP7bIaCj8KjCbLOTmDpTo7aNuSxP7g=','123','NGUNG_HOAT_DONG','2026-10-03 22:35:36','2026-10-03 22:35:36'),(8,2,'Nhân viên POS','staff@gmail.com','staff','M6Ek/DlW0lWb9gl7Q31UJw==:C1mMTfRLw/Om9F9xLiX6V6ZIf++88akWiyqsQGFKdcs=','0900000001','HOAT_DONG','2026-10-06 21:48:45','2026-10-06 21:49:03');
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
  `token_type` enum('XAC_THUC_EMAIL','DAT_LAI_MAT_KHAU') NOT NULL,
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
INSERT INTO `verification_tokens` VALUES (1,3,'2b3c8fc72ed8dc55bee087fb3221082e559a9feae6baa2dcf8af424de8f821b5','XAC_THUC_EMAIL','2026-10-01 23:30:50',NULL,'2026-10-02 06:20:50'),(2,4,'731c94a3d34b85e8457f78266c0988e0f89d51842b89dd8930d0f484292656bb','XAC_THUC_EMAIL','2026-10-01 23:32:02',NULL,'2026-10-02 06:22:02'),(3,5,'6cb08b0dbd6b9243288514e0cc5ae9b95d86039dd75946a3705f75f3c9e0db85','XAC_THUC_EMAIL','2026-10-01 23:33:38','2026-10-02 06:24:11','2026-10-02 06:23:37'),(4,5,'64df9e0ddd4683a0602895316a11a0cc4f98116712de03b7bfd996822cd9f0ca','XAC_THUC_EMAIL','2026-10-01 23:34:11',NULL,'2026-10-02 06:24:11'),(5,6,'b2c48ad5185d6cf69ce016731022b82ad9c54303126d1bf45887bc00618a85b4','XAC_THUC_EMAIL','2026-10-02 06:46:53','2026-10-02 06:32:28','2026-10-02 06:31:53'),(6,6,'207e8417c575f10f10f96d8fdbe8f13cc65f6d0a682acf0934316bae82a4cf4e','DAT_LAI_MAT_KHAU','2026-10-02 06:49:19','2026-10-02 06:39:51','2026-10-02 06:34:19'),(7,6,'c2169cd88d1ca3e47d3fdd0b5d1a74f01c0ebdf80b2c052b37d53de496d3c0b7','DAT_LAI_MAT_KHAU','2026-10-02 06:54:51','2026-10-02 06:40:52','2026-10-02 06:39:51'),(8,6,'698f7ac2e01b2d10e2bb3cc6344f2b6814adcbdf657a4db9c7af50c01effd3fd','DAT_LAI_MAT_KHAU','2026-10-02 06:55:52','2026-10-02 06:41:32','2026-10-02 06:40:52'),(9,7,'48a75f018a480d21cb3c0a0cc7f9ced69dce8ec7f42ff0080b9d1ceb8d996f2c','XAC_THUC_EMAIL','2026-10-03 22:50:36',NULL,'2026-10-03 22:35:36');
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

-- Dump completed on 2026-10-07 18:28:13
