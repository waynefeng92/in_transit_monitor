-- MySQL dump 10.13  Distrib 9.6.0, for macos14.8 (arm64)
--
-- Host: localhost    Database: ro_ro_monitor
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `ro_ro_monitor`
--

/*!40000 DROP DATABASE IF EXISTS `ro_ro_monitor`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `ro_ro_monitor` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `ro_ro_monitor`;

--
-- Table structure for table `brand_dict`
--

DROP TABLE IF EXISTS `brand_dict`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brand_dict` (
  `id` int NOT NULL AUTO_INCREMENT,
  `brand_name` varchar(50) NOT NULL COMMENT '品牌名称：上汽大众、通用、宝马等',
  `wmi_code` varchar(3) DEFAULT NULL COMMENT 'WMI车辆识别代码（前三位）：SVW、LSG、WBA等',
  `is_active` tinyint DEFAULT '1' COMMENT '是否启用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `brand_name` (`brand_name`),
  UNIQUE KEY `wmi_code` (`wmi_code`),
  KEY `idx_brand_name` (`brand_name`),
  KEY `idx_wmi_code` (`wmi_code`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='品牌字典表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `excel_field_mapping`
--

DROP TABLE IF EXISTS `excel_field_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `excel_field_mapping` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '映射ID',
  `brand_id` int DEFAULT NULL COMMENT '品牌ID，关联brand_dict，NULL表示默认规则',
  `standard_field` varchar(50) NOT NULL COMMENT '标准字段名',
  `excel_column_names` varchar(500) NOT NULL COMMENT 'Excel表头名称，多个用逗号分隔',
  `date_format` varchar(50) DEFAULT NULL COMMENT '时间格式',
  `is_required` tinyint DEFAULT '1' COMMENT '是否必填：1必填，0可选',
  `default_value` varchar(100) DEFAULT NULL COMMENT '默认值',
  `sort_order` int DEFAULT '0' COMMENT '排序字段',
  `is_active` tinyint DEFAULT '1' COMMENT '是否启用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_brand` (`brand_id`),
  KEY `idx_standard_field` (`standard_field`),
  CONSTRAINT `excel_field_mapping_ibfk_1` FOREIGN KEY (`brand_id`) REFERENCES `brand_dict` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=181 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Excel字段映射配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `excel_parse_config`
--

DROP TABLE IF EXISTS `excel_parse_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `excel_parse_config` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `brand_id` int NOT NULL COMMENT '品牌ID，关联brand_dict',
  `sheet_locate_type` varchar(20) DEFAULT 'NAME' COMMENT 'Sheet定位方式：NAME/INDEX/AUTO',
  `sheet_name` varchar(100) DEFAULT NULL COMMENT 'Sheet名称（当locate_type=NAME时使用）',
  `sheet_index` int DEFAULT '0' COMMENT 'Sheet索引（当locate_type=INDEX时使用，0表示第一个）',
  `header_row_index` int DEFAULT '0' COMMENT '表头所在行号（0表示第一行）',
  `data_start_row_index` int DEFAULT '1' COMMENT '数据起始行号（1表示第二行）',
  `is_active` tinyint DEFAULT '1' COMMENT '是否启用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_brand` (`brand_id`),
  KEY `idx_brand` (`brand_id`),
  CONSTRAINT `excel_parse_config_ibfk_1` FOREIGN KEY (`brand_id`) REFERENCES `brand_dict` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Excel解析配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `monitor_status_dict`
--

DROP TABLE IF EXISTS `monitor_status_dict`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `monitor_status_dict` (
  `status_code` varchar(10) NOT NULL,
  `status_name` varchar(20) NOT NULL,
  PRIMARY KEY (`status_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监控状态字典表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_info`
--

DROP TABLE IF EXISTS `order_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_info` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '订单记录ID',
  `brand_id` int NOT NULL COMMENT '品牌ID，关联brand_dict',
  `vin` varchar(17) NOT NULL COMMENT '车架号',
  `order_release_time` datetime NULL COMMENT '订单释放时间',
  `origin_city` varchar(50) DEFAULT NULL COMMENT '出发地',
  `dest_city` varchar(50) DEFAULT NULL COMMENT '目的地',
  `route_id` int DEFAULT NULL COMMENT '线路ID，关联route_dict',
  `is_active` tinyint DEFAULT '1' COMMENT '是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vin` (`vin`),
  KEY `idx_brand` (`brand_id`),
  KEY `idx_vin` (`vin`),
  KEY `idx_order_time` (`order_release_time`),
  KEY `idx_route` (`route_id`),
  CONSTRAINT `order_info_ibfk_1` FOREIGN KEY (`brand_id`) REFERENCES `brand_dict` (`id`),
  CONSTRAINT `order_info_ibfk_2` FOREIGN KEY (`route_id`) REFERENCES `route_dict` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2152 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `port_dict`
--

DROP TABLE IF EXISTS `port_dict`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `port_dict` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '港口ID',
  `port_name` varchar(100) NOT NULL COMMENT '港口中文名',
  `port_code` varchar(20) DEFAULT NULL COMMENT '港口代码（如CNSHA、CNDLC）',
  `is_active` tinyint DEFAULT '1' COMMENT '是否启用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `port_name` (`port_name`),
  KEY `idx_port_name` (`port_name`),
  KEY `idx_port_code` (`port_code`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='港口字典表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `route_dict`
--

DROP TABLE IF EXISTS `route_dict`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `route_dict` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '线路ID',
  `brand_id` int NOT NULL COMMENT '品牌ID，关联brand_dict',
  `origin_city` varchar(50) DEFAULT NULL COMMENT '出发地城市',
  `origin_port_id` int NOT NULL COMMENT '出发港ID，关联port_dict',
  `dest_port_id` int NOT NULL COMMENT '目的港ID，关联port_dict',
  `dest_city` varchar(50) DEFAULT NULL COMMENT '目的地城市',
  `is_active` tinyint DEFAULT '1' COMMENT '是否启用',
  PRIMARY KEY (`id`),
  KEY `idx_brand` (`brand_id`),
  KEY `idx_origin_port` (`origin_port_id`),
  KEY `idx_dest_port` (`dest_port_id`),
  CONSTRAINT `route_dict_ibfk_1` FOREIGN KEY (`brand_id`) REFERENCES `brand_dict` (`id`),
  CONSTRAINT `route_dict_ibfk_2` FOREIGN KEY (`origin_port_id`) REFERENCES `port_dict` (`id`),
  CONSTRAINT `route_dict_ibfk_3` FOREIGN KEY (`dest_port_id`) REFERENCES `port_dict` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='线路表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `route_otd_config`
--

DROP TABLE IF EXISTS `route_otd_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `route_otd_config` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'OTD配置ID',
  `route_id` int NOT NULL COMMENT '线路ID，关联route_dict',
  `not_departed_otd` double DEFAULT NULL COMMENT '未出库 → 集港在途 标准时效',
  `to_port_otd` double DEFAULT NULL COMMENT '集港在途 → 已集港待装船 标准时效',
  `at_port_wait_otd` double DEFAULT NULL COMMENT '已集港待装船 → 水运在途 标准时效',
  `on_sea_otd` double DEFAULT NULL COMMENT '水运在途 → 已到港待卸船 标准时效',
  `at_dest_wait_otd` double DEFAULT NULL COMMENT '已到港待卸船 → 已卸船待分拨 标准时效',
  `unload_wait_dispatch_otd` double DEFAULT NULL COMMENT '已卸船待分拨 → 分拨在途 标准时效',
  `dispatching_otd` double DEFAULT NULL COMMENT '分拨在途 → 已到达 标准时效',
  `not_departed_warn` double DEFAULT NULL COMMENT '未出库预警时效',
  `to_port_warn` double DEFAULT NULL COMMENT '集港在途预警时效',
  `at_port_wait_warn` double DEFAULT NULL COMMENT '已集港待装船预警时效',
  `on_sea_warn` double DEFAULT NULL COMMENT '水运在途预警时效',
  `at_dest_wait_warn` double DEFAULT NULL COMMENT '已到港待卸船预警时效',
  `unload_wait_dispatch_warn` double DEFAULT NULL COMMENT '已卸船待分拨预警时效',
  `dispatching_warn` double DEFAULT NULL COMMENT '分拨在途预警时效',
  `is_active` tinyint DEFAULT '1' COMMENT '是否启用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_route` (`route_id`),
  KEY `idx_route` (`route_id`),
  CONSTRAINT `route_otd_config_ibfk_1` FOREIGN KEY (`route_id`) REFERENCES `route_dict` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='线路OTD时效配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transport_status_dict`
--

DROP TABLE IF EXISTS `transport_status_dict`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transport_status_dict` (
  `status_code` varchar(30) NOT NULL COMMENT '状态唯一编码',
  `status_name` varchar(50) NOT NULL COMMENT '显示名称',
  `display_order` int DEFAULT '0' COMMENT '排序字段，用于图表从左到右展示',
  PRIMARY KEY (`status_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='在途状态字典表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `upload_batch`
--

DROP TABLE IF EXISTS `upload_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `upload_batch` (
  `id` int NOT NULL AUTO_INCREMENT,
  `batch_id` varchar(50) NOT NULL COMMENT '批次号',
  `file_name` varchar(200) DEFAULT NULL COMMENT '上传文件名',
  `upload_user` varchar(50) DEFAULT NULL COMMENT '上传人',
  `record_count` int DEFAULT '0' COMMENT '本批次记录条数',
  `status` varchar(20) DEFAULT 'PROCESSING' COMMENT '处理状态：PROCESSING/SUCCESS/FAILED',
  `error_message` text COMMENT '错误信息',
  `upload_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `batch_id` (`batch_id`),
  KEY `idx_batch_time` (`upload_time`),
  KEY `idx_batch_id` (`batch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='上传批次管理表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `vehicle_transit`
--

DROP TABLE IF EXISTS `vehicle_transit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vehicle_transit` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '在途记录ID',
  `order_id` int NOT NULL COMMENT '订单ID，关联order_info',
  `depart_warehouse_time` datetime DEFAULT NULL COMMENT '出库时间（离开仓库的时间）',
  `arrive_port_time` datetime DEFAULT NULL COMMENT '集港到港时间',
  `ship_depart_time` datetime DEFAULT NULL COMMENT '船离始发港时间',
  `ship_arrive_time` datetime DEFAULT NULL COMMENT '船到目的港时间',
  `unload_finish_time` datetime DEFAULT NULL COMMENT '卸船完成时间',
  `dispatch_time` datetime DEFAULT NULL COMMENT '分拨时间',
  `arrive_shop_time` datetime DEFAULT NULL COMMENT '到店时间',
  `transport_status` varchar(30) NOT NULL COMMENT '在途状态',
  `monitor_status` varchar(10) NOT NULL COMMENT '预警状态',
  `batch_id` varchar(50) DEFAULT NULL COMMENT '上传批次号',
  `data_source` varchar(20) DEFAULT 'EXCEL' COMMENT '数据来源：EXCEL/MANUAL/API',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `overall_monitor_status` varchar(20) DEFAULT NULL COMMENT '整段监控状态：NORMAL/WARN/OVERDUE',
  `section_monitor_status` varchar(20) DEFAULT NULL COMMENT '三段监控状态：NORMAL/WARN/OVERDUE',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order` (`order_id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_transport_status` (`transport_status`),
  KEY `idx_monitor_status` (`monitor_status`),
  KEY `idx_batch` (`batch_id`),
  CONSTRAINT `vehicle_transit_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `order_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `vehicle_transit_ibfk_2` FOREIGN KEY (`transport_status`) REFERENCES `transport_status_dict` (`status_code`),
  CONSTRAINT `vehicle_transit_ibfk_3` FOREIGN KEY (`monitor_status`) REFERENCES `monitor_status_dict` (`status_code`),
  CONSTRAINT `vehicle_transit_ibfk_4` FOREIGN KEY (`batch_id`) REFERENCES `upload_batch` (`batch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2152 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='在途状态表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-09 19:11:17
-- MySQL dump 10.13  Distrib 9.6.0, for macos14.8 (arm64)
--
-- Host: localhost    Database: ro_ro_monitor
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `brand_dict`
--

LOCK TABLES `brand_dict` WRITE;
/*!40000 ALTER TABLE `brand_dict` DISABLE KEYS */;
INSERT INTO `brand_dict` (`id`, `brand_name`, `wmi_code`, `is_active`) VALUES (1,'上汽大众','LSV',1),(2,'上汽通用','LSG',1),(3,'特斯拉','LRW',1),(6,'理想','HLX',1),(7,'广汽商贸','',1);
/*!40000 ALTER TABLE `brand_dict` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-09 19:11:30
-- MySQL dump 10.13  Distrib 9.6.0, for macos14.8 (arm64)
--
-- Host: localhost    Database: ro_ro_monitor
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `excel_field_mapping`
--

LOCK TABLES `excel_field_mapping` WRITE;
/*!40000 ALTER TABLE `excel_field_mapping` DISABLE KEYS */;
INSERT INTO `excel_field_mapping` (`id`, `brand_id`, `standard_field`, `excel_column_names`, `date_format`, `is_required`, `default_value`, `sort_order`, `is_active`, `created_at`) VALUES (1,NULL,'vin','车架号,VIN,vin,VIN码,车架码',NULL,1,NULL,1,1,'2026-04-13 03:44:58'),(2,NULL,'brandName','品牌,品牌名称,brand',NULL,0,NULL,2,1,'2026-04-13 03:44:58'),(3,NULL,'orderReleaseTime','订单释放时间,开单时间,订单时间,下单时间','yyyy-MM-dd HH:mm:ss',1,NULL,3,1,'2026-04-13 03:44:58'),(4,NULL,'originCity','出发地,始发地,出发城市,始发城市',NULL,0,NULL,4,1,'2026-04-13 03:44:58'),(5,NULL,'destCity','目的地,目的城市,到达城市',NULL,0,NULL,5,1,'2026-04-13 03:44:58'),(6,NULL,'departWarehouseTime','出库时间,离开仓库时间,发运时间','yyyy-MM-dd HH:mm:ss',0,NULL,6,1,'2026-04-13 03:44:58'),(7,NULL,'arrivePortTime','集港时间,到港时间,集港到港时间,到达港口时间','yyyy-MM-dd HH:mm:ss',0,NULL,7,1,'2026-04-13 03:44:58'),(8,NULL,'shipDepartTime','船离港时间,船离始发港时间,发船时间,离港时间','yyyy-MM-dd HH:mm:ss',0,NULL,8,1,'2026-04-13 03:44:58'),(9,NULL,'shipArriveTime','船到港时间,船到目的港时间,到港时间,抵港时间','yyyy-MM-dd HH:mm:ss',0,NULL,9,1,'2026-04-13 03:44:58'),(10,NULL,'unloadFinishTime','卸船时间,卸船完成时间,卸车完成时间','yyyy-MM-dd HH:mm:ss',0,NULL,10,1,'2026-04-13 03:44:58'),(11,NULL,'dispatchTime','分拨时间,分拨出库时间,配送时间','yyyy-MM-dd HH:mm:ss',0,NULL,11,1,'2026-04-13 03:44:58'),(12,NULL,'arriveShopTime','到店时间,到达经销商时间,交付时间,签收时间','yyyy-MM-dd HH:mm:ss',0,NULL,12,1,'2026-04-13 03:44:58'),(97,3,'vin','VIN',NULL,1,'',0,1,'2026-04-13 08:10:09'),(98,3,'brandName','',NULL,1,'特斯拉',0,1,'2026-04-13 08:10:09'),(99,3,'orderReleaseTime','订单日期','yyyy-MM-dd HH:mm:ss',1,'',0,1,'2026-04-13 08:10:09'),(100,3,'originCity','',NULL,1,'上海',0,1,'2026-04-13 08:10:09'),(101,3,'destCity','目的地',NULL,1,'',0,1,'2026-04-13 08:10:09'),(102,3,'departWarehouseTime','','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-13 08:10:09'),(103,3,'arrivePortTime','','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-13 08:10:09'),(104,3,'shipDepartTime','实际离港日期','yyyy-MM-dd HH:mm:ss',1,'',0,1,'2026-04-13 08:10:09'),(105,3,'shipArriveTime','实际到达目的港日期','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-13 08:10:09'),(106,3,'unloadFinishTime','卸船时间','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-13 08:10:09'),(107,3,'dispatchTime','后端分拨实际发运日期','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-13 08:10:09'),(108,3,'arriveShopTime','到店时间','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-13 08:10:09'),(145,6,'vin','VIN',NULL,1,'',0,1,'2026-04-15 04:17:46'),(146,6,'brandName','',NULL,1,'理想',0,1,'2026-04-15 04:17:46'),(147,6,'orderReleaseTime','实际下发时间','yyyy-MM-dd HH:mm:ss',1,'',0,1,'2026-04-15 04:17:46'),(148,6,'originCity','',NULL,1,'常州',0,1,'2026-04-15 04:17:46'),(149,6,'destCity','目的城市',NULL,1,'',0,1,'2026-04-15 04:17:46'),(150,6,'departWarehouseTime','提车交接时间【总库提车交接】','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-15 04:17:46'),(151,6,'arrivePortTime','集港到港时间【一段司机到达】','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-15 04:17:46'),(152,6,'shipDepartTime','水运启运时间【集货地离港】','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-15 04:17:46'),(153,6,'shipArriveTime','','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-15 04:17:46'),(154,6,'unloadFinishTime','实际到港时间【二段交车交接】','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-15 04:17:46'),(155,6,'dispatchTime','实际分拨时间【三段提车交接】','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-15 04:17:46'),(156,6,'arriveShopTime','司机到达时间【三段到达时间】','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-15 04:17:46'),(169,7,'vin','VIN码',NULL,1,'',0,1,'2026-04-29 07:41:57'),(170,7,'brandName','',NULL,1,'广汽商贸',0,1,'2026-04-29 07:41:57'),(171,7,'orderReleaseTime','子订单已确认时间','yyyy-MM-dd HH:mm:ss',1,'',0,1,'2026-04-29 07:41:57'),(172,7,'originCity','',NULL,1,'广州',0,1,'2026-04-29 07:41:57'),(173,7,'destCity','目的地城市',NULL,1,'',0,1,'2026-04-29 07:41:57'),(174,7,'departWarehouseTime','出库时间（前端）','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-29 07:41:57'),(175,7,'arrivePortTime','前端到达时间','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-29 07:41:57'),(176,7,'shipDepartTime','离港时间','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-29 07:41:57'),(177,7,'shipArriveTime','到港时间','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-29 07:41:57'),(178,7,'unloadFinishTime','到港时间','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-29 07:41:57'),(179,7,'dispatchTime','出库时间（后端）','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-29 07:41:57'),(180,7,'arriveShopTime','实际抵达时间','yyyy-MM-dd HH:mm:ss',0,'',0,1,'2026-04-29 07:41:57');
/*!40000 ALTER TABLE `excel_field_mapping` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-09 19:11:30
-- MySQL dump 10.13  Distrib 9.6.0, for macos14.8 (arm64)
--
-- Host: localhost    Database: ro_ro_monitor
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `excel_parse_config`
--

LOCK TABLES `excel_parse_config` WRITE;
/*!40000 ALTER TABLE `excel_parse_config` DISABLE KEYS */;
INSERT INTO `excel_parse_config` (`id`, `brand_id`, `sheet_locate_type`, `sheet_name`, `sheet_index`, `header_row_index`, `data_start_row_index`, `is_active`, `created_at`, `updated_at`) VALUES (1,1,'NAME','在途数据',0,0,1,1,'2026-04-13 04:02:32','2026-04-13 04:02:32'),(3,3,'NAME','Transit Data',0,0,1,1,'2026-04-13 04:04:12','2026-04-13 04:04:12');
/*!40000 ALTER TABLE `excel_parse_config` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-09 19:11:30
-- MySQL dump 10.13  Distrib 9.6.0, for macos14.8 (arm64)
--
-- Host: localhost    Database: ro_ro_monitor
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `monitor_status_dict`
--

LOCK TABLES `monitor_status_dict` WRITE;
/*!40000 ALTER TABLE `monitor_status_dict` DISABLE KEYS */;
INSERT INTO `monitor_status_dict` (`status_code`, `status_name`) VALUES ('NORMAL','正常'),('OVERDUE','已超期'),('WARN','预警');
/*!40000 ALTER TABLE `monitor_status_dict` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-09 19:11:30
-- MySQL dump 10.13  Distrib 9.6.0, for macos14.8 (arm64)
--
-- Host: localhost    Database: ro_ro_monitor
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `port_dict`
--

LOCK TABLES `port_dict` WRITE;
/*!40000 ALTER TABLE `port_dict` DISABLE KEYS */;
INSERT INTO `port_dict` (`id`, `port_name`, `port_code`, `is_active`) VALUES (1,'上海南港','',1),(2,'大连港','',1),(3,'南沙港','',1),(4,'文昌港','',1),(5,'烟台港','',1),(6,'天津港','',1),(7,'宁波港','',1),(8,'小漠港','',1);
/*!40000 ALTER TABLE `port_dict` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-09 19:11:30
-- MySQL dump 10.13  Distrib 9.6.0, for macos14.8 (arm64)
--
-- Host: localhost    Database: ro_ro_monitor
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `route_dict`
--

LOCK TABLES `route_dict` WRITE;
/*!40000 ALTER TABLE `route_dict` DISABLE KEYS */;
INSERT INTO `route_dict` (`id`, `brand_id`, `origin_city`, `origin_port_id`, `dest_port_id`, `dest_city`, `is_active`) VALUES (1,3,'上海',1,2,'大连交付中心',1),(2,3,'上海',1,2,'沈阳交付中心',1),(3,3,'上海',1,2,'长春交付中心',1),(4,3,'上海',1,3,'广州交付中心',1),(5,3,'上海',1,2,'哈尔滨交付中心',1),(6,3,'上海',1,4,'海口交付中心',1),(7,3,'上海',1,4,'三亚交付中心',1),(8,6,'常州',1,2,'大连市',1),(9,6,'常州',1,2,'沈阳市',1),(10,3,'上海',1,3,'南沙港（港澳外贸）',1),(11,6,'常州',1,6,'天津市',1),(12,6,'常州',1,6,'北京市',1),(13,6,'常州',1,3,'广州市',1),(14,6,'常州',1,3,'深圳市',1),(15,6,'常州',1,3,'东莞市',1),(16,7,'广州',3,6,'北京市市辖区',1),(17,7,'广州',3,6,'北京市市辖县',1),(18,7,'广州',3,6,'天津市市辖区',1),(19,7,'广州',3,2,'沈阳市',1),(20,7,'广州',3,2,'大连市',1),(21,7,'广州',3,2,'鞍山市',1),(22,7,'广州',3,2,'本溪市',1),(23,7,'广州',3,2,'丹东市',1),(24,7,'广州',3,2,'锦州市',1),(25,7,'广州',3,2,'营口市',1),(26,7,'广州',3,2,'阜新市',1),(27,7,'广州',3,2,'辽阳市',1),(28,7,'广州',3,2,'盘锦市',1),(29,7,'广州',3,2,'朝阳市',1),(30,7,'广州',3,2,'铁岭市',1),(31,7,'广州',3,2,'葫芦岛市',1),(32,7,'广州',3,2,'长春市',1),(33,7,'广州',3,2,'吉林市',1),(34,7,'广州',3,2,'四平市',1),(35,7,'广州',3,2,'辽源市',1),(36,7,'广州',3,2,'通化市',1),(37,7,'广州',3,2,'松原市',1),(38,7,'广州',3,2,'延吉市',1),(39,7,'广州',3,2,'白山市',1),(40,7,'广州',3,2,'延边市',1),(41,7,'广州',3,2,'哈尔滨市',1),(42,7,'广州',3,2,'齐齐哈尔市',1),(43,7,'广州',3,2,'大庆市',1),(44,7,'广州',3,2,'佳木斯市',1),(45,7,'广州',3,2,'七台河市',1),(46,7,'广州',3,2,'牡丹江市',1),(47,7,'广州',3,2,'绥化市',1),(48,7,'广州',3,2,'双鸭山市',1),(49,7,'广州',3,1,'上海市',1),(50,7,'广州',3,1,'南京市',1),(51,7,'广州',3,1,'无锡市',1),(52,7,'广州',3,1,'徐州市',1),(53,7,'广州',3,1,'常州市',1),(54,7,'广州',3,1,'苏州市',1),(55,7,'广州',3,1,'南通市',1),(56,7,'广州',3,1,'连云港市',1),(57,7,'广州',3,1,'淮安市',1),(58,7,'广州',3,1,'盐城市',1),(59,7,'广州',3,1,'扬州市',1),(60,7,'广州',3,1,'镇江市',1),(61,7,'广州',3,1,'泰州市',1),(62,7,'广州',3,1,'宿迁市',1);
/*!40000 ALTER TABLE `route_dict` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-09 19:11:30
-- MySQL dump 10.13  Distrib 9.6.0, for macos14.8 (arm64)
--
-- Host: localhost    Database: ro_ro_monitor
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `route_otd_config`
--

LOCK TABLES `route_otd_config` WRITE;
/*!40000 ALTER TABLE `route_otd_config` DISABLE KEYS */;
INSERT INTO `route_otd_config` (`id`, `route_id`, `not_departed_otd`, `to_port_otd`, `at_port_wait_otd`, `on_sea_otd`, `at_dest_wait_otd`, `unload_wait_dispatch_otd`, `dispatching_otd`, `not_departed_warn`, `to_port_warn`, `at_port_wait_warn`, `on_sea_warn`, `at_dest_wait_warn`, `unload_wait_dispatch_warn`, `dispatching_warn`, `is_active`) VALUES (1,1,0,0,0,48,12,6,6,0,0,0,40,8,3,3,1),(2,8,48,12,48,48,12,12,12,24,10,40,40,10,10,10,1),(3,9,48,12,48,48,12,12,36,24,10,40,40,10,10,24,1),(4,11,48,12,48,72,12,12,12,24,10,40,64,10,10,10,1),(5,12,48,12,48,72,12,12,36,24,10,40,64,10,10,24,1),(6,13,48,12,48,72,12,12,12,24,10,40,64,10,10,10,1),(7,14,48,12,48,72,12,12,12,24,10,40,64,10,10,10,1),(8,15,48,12,48,72,12,12,12,24,10,40,64,10,10,10,1),(9,2,0,0,0,48,12,6,6,0,0,0,40,8,3,3,1),(10,3,0,0,0,48,12,6,30,0,0,0,40,8,3,24,1),(11,4,0,0,0,72,12,6,6,0,0,0,60,8,3,3,1),(12,5,0,0,0,48,12,6,54,0,0,0,40,8,3,44,1),(13,6,0,0,0,192,12,6,6,0,0,0,170,8,3,3,1),(14,7,0,0,0,192,12,6,6,0,0,0,170,8,3,3,1),(15,10,0,0,0,72,12,0,0,0,0,0,60,8,0,0,1);
/*!40000 ALTER TABLE `route_otd_config` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-09 19:11:30
-- MySQL dump 10.13  Distrib 9.6.0, for macos14.8 (arm64)
--
-- Host: localhost    Database: ro_ro_monitor
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `transport_status_dict`
--

LOCK TABLES `transport_status_dict` WRITE;
/*!40000 ALTER TABLE `transport_status_dict` DISABLE KEYS */;
INSERT INTO `transport_status_dict` (`status_code`, `status_name`, `display_order`) VALUES ('ARRIVED','已到达',8),('AT_DEST_WAIT_UNLOAD','已到港待卸船',5),('AT_PORT_WAIT_SHIP','已集港待装船',3),('DISPATCHING','分拨在途',7),('NOT_DEPARTED','未出库',1),('ON_SEA','水运在途',4),('TO_PORT','集港在途',2),('UNLOADED_WAIT_DISPATCH','已卸船待分拨',6);
/*!40000 ALTER TABLE `transport_status_dict` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-09 19:11:30
