-- MariaDB dump 10.17  Distrib 10.4.11-MariaDB, for Win64 (AMD64)
--
-- Host: localhost    Database: alice_teacher
-- ------------------------------------------------------
-- Server version	10.4.11-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `alice_teacher`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `alice_teacher` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;

USE `alice_teacher`;

--
-- Table structure for table `academic_records`
--

DROP TABLE IF EXISTS `academic_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `academic_records` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `class_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `score` double DEFAULT NULL,
  `student_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `teacher_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `term_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `academic_records`
--

LOCK TABLES `academic_records` WRITE;
/*!40000 ALTER TABLE `academic_records` DISABLE KEYS */;
INSERT INTO `academic_records` VALUES (1,'2026-10-30 17:18:00.379154','S2 A',78,'Aline Keza','Mathematics','Alice Uwimana','Term 1'),(2,'2026-10-30 17:18:00.410158','S2 A',64,'Brian Mugisha','Mathematics','Alice Uwimana','Term 1'),(3,'2026-10-30 17:18:00.417168','S2 B',81,'Chloe Uwase','English','Jean Bosco Hakizimana','Term 1'),(4,'2026-10-30 17:18:00.430684','S3 A',57,'David Niyonsenga','Biology','Marie Claire Ingabire','Term 1'),(5,'2026-10-30 17:18:00.441683','S1 A',73,'Elise Iradukunda','History','Eric Habimana','Term 1'),(6,'2026-10-30 17:18:00.450684','S3 B',88,'Felix Habineza','Computer Science','Divine Mukamana','Term 1'),(7,'2026-10-30 17:18:00.467684','S2 C',69,'Gina Mutesi','Physics','Grace Uwase','Term 1');
/*!40000 ALTER TABLE `academic_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `app_settings`
--

DROP TABLE IF EXISTS `app_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_settings` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `setting_key` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `setting_value` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_7p82g7l6uve2vd8l30djhxpel` (`setting_key`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `app_settings`
--

LOCK TABLES `app_settings` WRITE;
/*!40000 ALTER TABLE `app_settings` DISABLE KEYS */;
INSERT INTO `app_settings` VALUES (1,'2026-10-30 17:18:00.537216','teacher.upload','true'),(2,'2026-10-30 17:18:00.545214','teacher.biometric','true'),(3,'2026-10-30 17:18:00.552213','hos.approve','true'),(4,'2026-10-30 17:18:00.560216','report.schedule','18:00'),(5,'2026-10-30 17:18:00.566217','academic.syncEnabled','true'),(6,'2026-10-30 17:18:00.572216','academic.lastImport','Sample data'),(7,'2026-10-30 18:00:29.473854','report.lastRun','2026-10-30');
/*!40000 ALTER TABLE `app_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `approval_logs`
--

DROP TABLE IF EXISTS `approval_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `approval_logs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `action` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `comment` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `document_id` bigint(20) NOT NULL,
  `reviewer_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8t2mtsakohkvah7n0pp35ek40` (`document_id`),
  KEY `FKeyjw0r096iq0vmkl7orhxe9d7` (`reviewer_id`),
  CONSTRAINT `FK8t2mtsakohkvah7n0pp35ek40` FOREIGN KEY (`document_id`) REFERENCES `school_documents` (`id`),
  CONSTRAINT `FKeyjw0r096iq0vmkl7orhxe9d7` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `approval_logs`
--

LOCK TABLES `approval_logs` WRITE;
/*!40000 ALTER TABLE `approval_logs` DISABLE KEYS */;
INSERT INTO `approval_logs` VALUES (1,'2026-10-30 17:17:59.769769','APPROVED','Clear objectives.',1,2),(2,'2026-10-30 17:17:59.843293','RETURNED','Add the names of absent students.',3,2),(3,'2026-10-30 17:17:59.910295','REJECTED','Add one experiment the class can do.',6,2),(4,'2026-10-30 17:25:15.542610','APPROVED','Scheme matches the weeks breakdown. Approved.',4,1);
/*!40000 ALTER TABLE `approval_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance_records`
--

DROP TABLE IF EXISTS `attendance_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `attendance_records` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `attend_date` date NOT NULL,
  `check_in` datetime(6) DEFAULT NULL,
  `check_out` datetime(6) DEFAULT NULL,
  `face_distance` double DEFAULT NULL,
  `method` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `marked_by_id` bigint(20) DEFAULT NULL,
  `teacher_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKl2dbj7jnuae8a22tuf7b148w` (`teacher_id`,`attend_date`),
  KEY `FKf0murgd8q96iigmstxx37mo7w` (`marked_by_id`),
  CONSTRAINT `FKf0murgd8q96iigmstxx37mo7w` FOREIGN KEY (`marked_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKt8xe9257uxubfufs3yuvhvvd1` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_records`
--

LOCK TABLES `attendance_records` WRITE;
/*!40000 ALTER TABLE `attendance_records` DISABLE KEYS */;
INSERT INTO `attendance_records` VALUES (1,'2026-10-30 17:17:59.353656','2026-10-30','2026-10-30 07:55:00.000000',NULL,NULL,'OVERRIDE','Manual override','PRESENT',1,3),(2,'2026-10-30 17:17:59.364659','2026-10-30','2026-10-30 08:27:00.000000','2026-10-30 18:30:13.125774',0.218,'FACE','Arrived after assembly','LATE',1,4),(3,'2026-10-30 17:17:59.371657','2026-10-30',NULL,NULL,NULL,'BULK','No check-in','ABSENT',1,5),(4,'2026-10-30 17:17:59.382658','2026-10-30',NULL,NULL,NULL,'BULK','Sick leave approved by the Head of Studies','ON_LEAVE',1,6),(5,'2026-10-30 17:17:59.387657','2026-10-30','2026-10-30 08:05:00.000000',NULL,NULL,'BULK','','PRESENT',1,7),(6,'2026-10-30 17:17:59.395658','2026-10-30','2026-10-30 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,8),(7,'2026-10-30 17:17:59.402659','2026-10-29','2026-10-29 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,3),(8,'2026-10-30 17:17:59.408656','2026-10-29','2026-10-29 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,4),(9,'2026-10-30 17:17:59.415668','2026-10-29','2026-10-29 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,5),(10,'2026-10-30 17:17:59.421668','2026-10-29','2026-10-29 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,6),(11,'2026-10-30 17:17:59.431190','2026-10-29','2026-10-29 08:22:00.000000',NULL,NULL,'BULK','','LATE',1,7),(12,'2026-10-30 17:17:59.437191','2026-10-29',NULL,NULL,NULL,'BULK','','ABSENT',1,8),(13,'2026-10-30 17:17:59.443189','2026-10-28','2026-10-28 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,3),(14,'2026-10-30 17:17:59.450187','2026-10-28','2026-10-28 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,4),(15,'2026-10-30 17:17:59.458189','2026-10-28','2026-10-28 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,5),(16,'2026-10-30 17:17:59.464191','2026-10-28','2026-10-28 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,6),(17,'2026-10-30 17:17:59.470190','2026-10-28','2026-10-28 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,7),(18,'2026-10-30 17:17:59.476188','2026-10-28','2026-10-28 08:22:00.000000',NULL,NULL,'BULK','','LATE',1,8),(19,'2026-10-30 17:17:59.482191','2026-10-27','2026-10-27 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,3),(20,'2026-10-30 17:17:59.487189','2026-10-27','2026-10-27 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,4),(21,'2026-10-30 17:17:59.494190','2026-10-27','2026-10-27 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,5),(22,'2026-10-30 17:17:59.500191','2026-10-27','2026-10-27 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,6),(23,'2026-10-30 17:17:59.506188','2026-10-27','2026-10-27 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,7),(24,'2026-10-30 17:17:59.511195','2026-10-27','2026-10-27 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,8),(25,'2026-10-30 17:17:59.516198','2026-10-26','2026-10-26 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,3),(26,'2026-10-30 17:17:59.521195','2026-10-26','2026-10-26 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,4),(27,'2026-10-30 17:17:59.526716','2026-10-26','2026-10-26 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,5),(28,'2026-10-30 17:17:59.532715','2026-10-26','2026-10-26 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,6),(29,'2026-10-30 17:17:59.537716','2026-10-26','2026-10-26 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,7),(30,'2026-10-30 17:17:59.542714','2026-10-26','2026-10-26 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,8),(31,'2026-10-30 17:17:59.548721','2026-10-23',NULL,NULL,NULL,'BULK','','ABSENT',1,3),(32,'2026-10-30 17:17:59.555715','2026-10-23','2026-10-23 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,4),(33,'2026-10-30 17:17:59.560715','2026-10-23','2026-10-23 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,5),(34,'2026-10-30 17:17:59.566716','2026-10-23','2026-10-23 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,6),(35,'2026-10-30 17:17:59.572715','2026-10-23','2026-10-23 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,7),(36,'2026-10-30 17:17:59.576713','2026-10-23','2026-10-23 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,8),(37,'2026-10-30 17:17:59.582718','2026-10-22','2026-10-22 08:22:00.000000',NULL,NULL,'BULK','','LATE',1,3),(38,'2026-10-30 17:17:59.587716','2026-10-22',NULL,NULL,NULL,'BULK','','ABSENT',1,4),(39,'2026-10-30 17:17:59.591715','2026-10-22','2026-10-22 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,5),(40,'2026-10-30 17:17:59.596717','2026-10-22','2026-10-22 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,6),(41,'2026-10-30 17:17:59.603716','2026-10-22','2026-10-22 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,7),(42,'2026-10-30 17:17:59.608715','2026-10-22','2026-10-22 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,8),(43,'2026-10-30 17:17:59.614728','2026-10-21','2026-10-21 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,3),(44,'2026-10-30 17:17:59.619728','2026-10-21','2026-10-21 08:22:00.000000',NULL,NULL,'BULK','','LATE',1,4),(45,'2026-10-30 17:17:59.624728','2026-10-21',NULL,NULL,NULL,'BULK','','ABSENT',1,5),(46,'2026-10-30 17:17:59.633242','2026-10-21','2026-10-21 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,6),(47,'2026-10-30 17:17:59.637245','2026-10-21','2026-10-21 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,7),(48,'2026-10-30 17:17:59.643243','2026-10-21','2026-10-21 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,8),(49,'2026-10-30 17:17:59.649243','2026-10-20','2026-10-20 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,3),(50,'2026-10-30 17:17:59.654245','2026-10-20','2026-10-20 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,4),(51,'2026-10-30 17:17:59.660244','2026-10-20','2026-10-20 08:22:00.000000',NULL,NULL,'BULK','','LATE',1,5),(52,'2026-10-30 17:17:59.665246','2026-10-20',NULL,NULL,NULL,'BULK','','ABSENT',1,6),(53,'2026-10-30 17:17:59.670244','2026-10-20','2026-10-20 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,7),(54,'2026-10-30 17:17:59.677245','2026-10-20','2026-10-20 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,8),(55,'2026-10-30 17:17:59.682248','2026-10-19','2026-10-19 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,3),(56,'2026-10-30 17:17:59.687244','2026-10-19','2026-10-19 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,4),(57,'2026-10-30 17:17:59.691241','2026-10-19','2026-10-19 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,5),(58,'2026-10-30 17:17:59.695241','2026-10-19','2026-10-19 08:22:00.000000',NULL,NULL,'BULK','','LATE',1,6),(59,'2026-10-30 17:17:59.700243','2026-10-19',NULL,NULL,NULL,'BULK','','ABSENT',1,7),(60,'2026-10-30 17:17:59.704243','2026-10-19','2026-10-19 07:50:00.000000',NULL,NULL,'BULK','','PRESENT',1,8);
/*!40000 ALTER TABLE `attendance_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `audit_logs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `action` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `detail` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjs4iimve3y0xssbtve5ysyef0` (`user_id`),
  CONSTRAINT `FKjs4iimve3y0xssbtve5ysyef0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_logs`
--

LOCK TABLES `audit_logs` WRITE;
/*!40000 ALTER TABLE `audit_logs` DISABLE KEYS */;
INSERT INTO `audit_logs` VALUES (1,'2026-10-30 17:18:15.488865','LOGIN','Signed in','0:0:0:0:0:0:0:1',1),(2,'2026-10-30 17:18:31.228563','LOGIN','Signed in','0:0:0:0:0:0:0:1',3),(3,'2026-10-30 17:20:19.300034','LOGIN','Signed in','0:0:0:0:0:0:0:1',1),(4,'2026-10-30 17:22:53.041437','ATTENDANCE_BULK','6 teachers on 2026-10-30','0:0:0:0:0:0:0:1',1),(5,'2026-10-30 17:23:53.585733','ATTENDANCE_OVERRIDE','Alice Uwimana PRESENT','0:0:0:0:0:0:0:1',1),(6,'2026-10-30 17:25:15.688660','DOCUMENT_APPROVED','S1 History scheme of work','0:0:0:0:0:0:0:1',1),(7,'2026-10-30 17:26:29.680155','SCHEME_SAVED','Mathematics scheme of work','0:0:0:0:0:0:0:1',1),(8,'2026-10-30 18:18:55.132726','LOGIN','Signed in','0:0:0:0:0:0:0:1',1),(9,'2026-10-30 18:28:05.006584','LOGIN','Signed in','192.168.1.68',1),(10,'2026-10-30 18:29:49.371606','FACE_ENROLL','Jean Bosco Hakizimana','192.168.1.68',1),(11,'2026-10-30 18:33:27.073889','ATTENDANCE_OVERRIDE','Alice Uwimana PRESENT','0:0:0:0:0:0:0:1',1),(12,'2026-10-30 22:01:09.531557','LOGIN','Signed in','192.168.1.68',1),(13,'2026-10-30 22:01:33.410058','FACE_CLEAR','Jean Bosco Hakizimana','192.168.1.68',1),(14,'2026-10-30 22:01:53.721345','FACE_ENROLL','Eric Habimana','192.168.1.68',1);
/*!40000 ALTER TABLE `audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `document_versions`
--

DROP TABLE IF EXISTS `document_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `document_versions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `file_path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version_number` int(11) NOT NULL,
  `document_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKdjvidg379tdgu1wvekvoli4vg` (`document_id`),
  CONSTRAINT `FKdjvidg379tdgu1wvekvoli4vg` FOREIGN KEY (`document_id`) REFERENCES `school_documents` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document_versions`
--

LOCK TABLES `document_versions` WRITE;
/*!40000 ALTER TABLE `document_versions` DISABLE KEYS */;
INSERT INTO `document_versions` VALUES (1,'2026-10-30 17:17:59.752769','9aee3aea-6a22-4bf2-9b6c-03473488e791.txt','Seeded sample','S2 Mathematics lesson plan.txt',1,1),(2,'2026-10-30 17:17:59.799772','4dc43b6b-7d70-4b24-9348-5d79acc641ca.txt','Seeded sample','S2 English lesson plan.txt',1,2),(3,'2026-10-30 17:17:59.833295','d49e62e3-97de-4323-b28b-47cc7d999d50.txt','Seeded sample','S3 Biology class register.txt',1,3),(4,'2026-10-30 17:17:59.864294','007e0392-435c-4180-a580-dd9d7ce5d05f.txt','Seeded sample','S1 History scheme of work.txt',1,4),(5,'2026-10-30 17:17:59.886294','7b753198-5b44-4d38-8bec-7c37569689d3.txt','Seeded sample','ICT monthly report.txt',1,5),(6,'2026-10-30 17:17:59.901294','4528f295-374c-4364-a2b7-4957ba1c714e.txt','Seeded sample','S2 Physics lesson plan.txt',1,6),(7,'2026-10-30 17:26:29.665153','dce09fb0-c303-461f-b5a5-31a8b73ce38f.xlsx','Generated from Syllabus and Weeks breakdown','Mathematics scheme of work.xlsx',1,7);
/*!40000 ALTER TABLE `document_versions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `login_sessions`
--

DROP TABLE IF EXISTS `login_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `login_sessions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `active` bit(1) NOT NULL,
  `ended_at` datetime(6) DEFAULT NULL,
  `ip_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKkpoyfbvelbgh15mdtfxl7dh82` (`user_id`),
  CONSTRAINT `FKkpoyfbvelbgh15mdtfxl7dh82` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `login_sessions`
--

LOCK TABLES `login_sessions` WRITE;
/*!40000 ALTER TABLE `login_sessions` DISABLE KEYS */;
INSERT INTO `login_sessions` VALUES (1,'2026-10-30 17:18:15.358342','',NULL,'0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT; Windows NT 10.0; en-US) WindowsPowerShell/5.1.26100.9818',1),(2,'2026-10-30 17:18:31.202560','',NULL,'0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT; Windows NT 10.0; en-US) WindowsPowerShell/5.1.26100.9818',3),(3,'2026-10-30 17:20:19.074463','',NULL,'0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Cursor/3.27.22 Chrome/148.0.7778.280 Electron/42.11.16 Safari/537.36',1),(4,'2026-10-30 18:18:55.007167','',NULL,'0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/156.0.0.0 Safari/537.36',1),(5,'2026-10-30 18:28:04.928511','',NULL,'192.168.1.68','Dalvik/2.1.0 (Linux; U; Android 15; V2520 Build/AP3A.240905.015.A2_NN_V000L1)',1),(6,'2026-10-30 22:01:09.354646','',NULL,'192.168.1.68','Dalvik/2.1.0 (Linux; U; Android 15; V2520 Build/AP3A.240905.015.A2_NN_V000L1)',1);
/*!40000 ALTER TABLE `login_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_preferences`
--

DROP TABLE IF EXISTS `notification_preferences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notification_preferences` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email_on` bit(1) NOT NULL,
  `in_app_on` bit(1) NOT NULL,
  `sms_on` bit(1) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_n2jopkbm16qv3xelbvoyjkd0g` (`user_id`),
  CONSTRAINT `FKt9qjvmcl36i14utm5uptyqg84` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_preferences`
--

LOCK TABLES `notification_preferences` WRITE;
/*!40000 ALTER TABLE `notification_preferences` DISABLE KEYS */;
INSERT INTO `notification_preferences` VALUES (1,'2026-10-30 17:17:59.153607','\0','','\0',1),(2,'2026-10-30 17:17:59.265129','\0','','\0',2),(3,'2026-10-30 17:17:59.282129','\0','','\0',3),(4,'2026-10-30 17:17:59.293128','\0','','\0',4),(5,'2026-10-30 17:17:59.307127','\0','','\0',5),(6,'2026-10-30 17:17:59.319135','\0','','\0',6),(7,'2026-10-30 17:17:59.331659','\0','','\0',7),(8,'2026-10-30 17:17:59.343658','\0','','\0',8);
/*!40000 ALTER TABLE `notification_preferences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notifications` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `channel` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `priority` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `read_flag` bit(1) NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9y21adhxn0ayjhfocscqox7bh` (`user_id`),
  CONSTRAINT `FK9y21adhxn0ayjhfocscqox7bh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` VALUES (1,'2026-10-30 17:18:00.302624','IN_APP','You are marked absent today. Contact the Head of Studies if this is a mistake.','HIGH','\0','Marked absent','ATTENDANCE',5),(2,'2026-10-30 17:18:00.316640','IN_APP','You were marked late at 08:27.','NORMAL','\0','Late arrival','ATTENDANCE',4),(3,'2026-10-30 17:18:00.328152','IN_APP','S2 Mathematics lesson plan was approved.','NORMAL','\0','Lesson plan approved','APPROVAL',3),(4,'2026-10-30 17:18:00.341153','IN_APP','English lesson plan and History scheme of work are in the approval queue.','HIGH','\0','Documents waiting','APPROVAL',2),(5,'2026-10-30 17:18:00.352152','IN_APP','Sample school data is ready. Password for every demo account is password123.','LOW','','Welcome','SYSTEM',1),(6,'2026-10-30 17:18:00.369154','IN_APP','S2 Physics lesson plan was rejected. Add a practical activity.','HIGH','\0','Lesson plan returned','APPROVAL',8),(7,'2026-10-30 17:25:15.681650','IN_APP','S1 History scheme of work: Scheme matches the weeks breakdown. Approved.','NORMAL','\0','Document approved','APPROVAL',6),(8,'2026-10-30 18:00:29.341204','IN_APP','4 teachers are present or late, and 1 are absent.','NORMAL','\0','Daily attendance summary','SYSTEM',1),(9,'2026-10-30 18:00:29.462854','IN_APP','4 teachers are present or late, and 1 are absent.','NORMAL','\0','Daily attendance summary','SYSTEM',2);
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `school_documents`
--

DROP TABLE IF EXISTS `school_documents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `school_documents` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `analysis_json` longtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `category` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reviewer_comment` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `routed_to` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `track` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` int(11) NOT NULL,
  `owner_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKtnbus1udkwm5qaf5yy2kx0s26` (`owner_id`),
  CONSTRAINT `FKtnbus1udkwm5qaf5yy2kx0s26` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `school_documents`
--

LOCK TABLES `school_documents` WRITE;
/*!40000 ALTER TABLE `school_documents` DISABLE KEYS */;
INSERT INTO `school_documents` VALUES (1,'2026-10-30 17:17:59.716251',NULL,'LESSON_PLAN','text/plain','9aee3aea-6a22-4bf2-9b6c-03473488e791.txt','S2 Mathematics lesson plan.txt','Clear objectives.','Head of Studies','APPROVED','S2 Mathematics lesson plan','REB','2026-10-30 17:17:59.712250',1,3),(2,'2026-10-30 17:17:59.788768',NULL,'LESSON_PLAN','text/plain','4dc43b6b-7d70-4b24-9348-5d79acc641ca.txt','S2 English lesson plan.txt',NULL,'Head of Studies','SUBMITTED','S2 English lesson plan','REB','2026-10-30 17:17:59.787769',1,4),(3,'2026-10-30 17:17:59.811777',NULL,'CLASS_REGISTER','text/plain','d49e62e3-97de-4323-b28b-47cc7d999d50.txt','S3 Biology class register.txt','Add the names of absent students.','Head of Studies','RETURNED','S3 Biology class register','REB','2026-10-30 17:17:59.811777',1,5),(4,'2026-10-30 17:17:59.857295',NULL,'SCHEME_OF_WORK','text/plain','007e0392-435c-4180-a580-dd9d7ce5d05f.txt','S1 History scheme of work.txt','Scheme matches the weeks breakdown. Approved.','Head of Studies','APPROVED','S1 History scheme of work','REB','2026-10-30 17:25:15.527610',1,6),(5,'2026-10-30 17:17:59.878295',NULL,'REPORT','text/plain','7b753198-5b44-4d38-8bec-7c37569689d3.txt','ICT monthly report.txt',NULL,'Head of Studies','DRAFT','ICT monthly report','TVET','2026-10-30 17:17:59.877293',1,7),(6,'2026-10-30 17:17:59.894295',NULL,'LESSON_PLAN','text/plain','4528f295-374c-4364-a2b7-4957ba1c714e.txt','S2 Physics lesson plan.txt','Add one experiment the class can do.','Head of Studies','REJECTED','S2 Physics lesson plan','REB','2026-10-30 17:17:59.893295',1,8),(7,'2026-10-30 17:26:29.642156','{\"summary\":\"This scheme of work for S2 Mathematics covers Units on Numbers and operations, Algebra, and Geometry, as outlined in the REB syllabus. It is structured over 10 weeks, with specific topics and learning outcomes for each week, and includes regular assessment.\",\"items\":[{\"code\":\"\",\"title\":\"Unit 1: Numbers and operations\",\"outcomes\":[\"Compare integers and place them on a number line.\",\"Add and subtract fractions with the same denominator.\",\"Calculate a percentage of a quantity.\"],\"content\":[\"integers\",\"fractions\",\"percentages\"]},{\"code\":\"\",\"title\":\"Unit 2: Algebra\",\"outcomes\":[\"Simplify an expression by collecting like terms.\",\"Solve a linear equation with one unknown.\",\"Check a solution by substitution.\"],\"content\":[\"algebraic expressions\",\"simple equations\"]},{\"code\":\"\",\"title\":\"Unit 3: Geometry\",\"outcomes\":[\"Name and measure acute, right, and obtuse angles.\",\"Find a missing angle in a triangle.\",\"Calculate the perimeter of a rectangle and a triangle.\"],\"content\":[\"angles\",\"triangles\",\"perimeter\"]}],\"weeks\":[{\"week\":1,\"topic\":\"Integers on the number line\",\"hours\":2},{\"week\":2,\"topic\":\"Addition and subtraction of integers\",\"hours\":2},{\"week\":3,\"topic\":\"Fractions with the same denominator\",\"hours\":2},{\"week\":4,\"topic\":\"Percentages of a quantity\",\"hours\":2},{\"week\":5,\"topic\":\"Algebraic expressions and like terms\",\"hours\":2},{\"week\":6,\"topic\":\"Solving simple linear equations\",\"hours\":2},{\"week\":7,\"topic\":\"Angles and their names\",\"hours\":2},{\"week\":8,\"topic\":\"Missing angle in a triangle\",\"hours\":2},{\"week\":9,\"topic\":\"Perimeter of rectangles and triangles\",\"hours\":2},{\"week\":10,\"topic\":\"Revision and short test\",\"hours\":2}],\"scheme\":[{\"week\":1,\"unit\":\"Unit 1: Numbers and operations\",\"lesson\":\"integers\",\"objectives\":\"Compare integers and place them on a number line.\",\"activities\":\"\",\"resources\":\"textbook, ruler, protractor, chalkboard\",\"assessment\":\"short exercise at the end of each lesson, and a weekly quiz.\",\"hours\":2},{\"week\":2,\"unit\":\"Unit 1: Numbers and operations\",\"lesson\":\"integers\",\"objectives\":\"Compare integers and place them on a number line.\",\"activities\":\"\",\"resources\":\"textbook, ruler, protractor, chalkboard\",\"assessment\":\"short exercise at the end of each lesson, and a weekly quiz.\",\"hours\":2},{\"week\":3,\"unit\":\"Unit 1: Numbers and operations\",\"lesson\":\"fractions\",\"objectives\":\"Add and subtract fractions with the same denominator.\",\"activities\":\"\",\"resources\":\"textbook, ruler, protractor, chalkboard\",\"assessment\":\"short exercise at the end of each lesson, and a weekly quiz.\",\"hours\":2},{\"week\":4,\"unit\":\"Unit 1: Numbers and operations\",\"lesson\":\"percentages\",\"objectives\":\"Calculate a percentage of a quantity.\",\"activities\":\"\",\"resources\":\"textbook, ruler, protractor, chalkboard\",\"assessment\":\"short exercise at the end of each lesson, and a weekly quiz.\",\"hours\":2},{\"week\":5,\"unit\":\"Unit 2: Algebra\",\"lesson\":\"algebraic expressions\",\"objectives\":\"Simplify an expression by collecting like terms.\",\"activities\":\"\",\"resources\":\"textbook, ruler, protractor, chalkboard\",\"assessment\":\"short exercise at the end of each lesson, and a weekly quiz.\",\"hours\":2},{\"week\":6,\"unit\":\"Unit 2: Algebra\",\"lesson\":\"simple equations\",\"objectives\":\"Solve a linear equation with one unknown. Check a solution by substitution.\",\"activities\":\"\",\"resources\":\"textbook, ruler, protractor, chalkboard\",\"assessment\":\"short exercise at the end of each lesson, and a weekly quiz.\",\"hours\":2},{\"week\":7,\"unit\":\"Unit 3: Geometry\",\"lesson\":\"angles\",\"objectives\":\"Name and measure acute, right, and obtuse angles.\",\"activities\":\"\",\"resources\":\"textbook, ruler, protractor, chalkboard\",\"assessment\":\"short exercise at the end of each lesson, and a weekly quiz.\",\"hours\":2},{\"week\":8,\"unit\":\"Unit 3: Geometry\",\"lesson\":\"triangles\",\"objectives\":\"Find a missing angle in a triangle.\",\"activities\":\"\",\"resources\":\"textbook, ruler, protractor, chalkboard\",\"assessment\":\"short exercise at the end of each lesson, and a weekly quiz.\",\"hours\":2},{\"week\":9,\"unit\":\"Unit 3: Geometry\",\"lesson\":\"perimeter\",\"objectives\":\"Calculate the perimeter of a rectangle and a triangle.\",\"activities\":\"\",\"resources\":\"textbook, ruler, protractor, chalkboard\",\"assessment\":\"short exercise at the end of each lesson, and a weekly quiz.\",\"hours\":2},{\"week\":10,\"unit\":\"\",\"lesson\":\"\",\"objectives\":\"\",\"activities\":\"\",\"resources\":\"textbook, ruler, protractor, chalkboard\",\"assessment\":\"short exercise at the end of each lesson, and a weekly quiz.\",\"hours\":2}],\"model\":\"gemini-2.5-flash\",\"track\":\"REB\",\"subject\":\"Mathematics\",\"className\":\"S2 A\",\"primaryLabel\":\"Syllabus\",\"secondaryLabel\":\"Weeks breakdown\",\"primaryChars\":903,\"secondaryChars\":556}','SCHEME_OF_WORK','application/vnd.openxmlformats-officedocument.spreadsheetml.sheet','dce09fb0-c303-461f-b5a5-31a8b73ce38f.xlsx','Mathematics scheme of work.xlsx',NULL,'Head of Studies','DRAFT','Mathematics scheme of work','REB','2026-10-30 17:26:29.639152',1,1);
/*!40000 ALTER TABLE `school_documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teaching_sessions`
--

DROP TABLE IF EXISTS `teaching_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `teaching_sessions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `actual_end` datetime(6) DEFAULT NULL,
  `actual_start` datetime(6) DEFAULT NULL,
  `class_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `end_time` time NOT NULL,
  `note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `room` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `session_date` date NOT NULL,
  `start_time` time NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `teacher_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqs7u7hfvnwr7751y8ilhq8e5s` (`teacher_id`),
  CONSTRAINT `FKqs7u7hfvnwr7751y8ilhq8e5s` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teaching_sessions`
--

LOCK TABLES `teaching_sessions` WRITE;
/*!40000 ALTER TABLE `teaching_sessions` DISABLE KEYS */;
INSERT INTO `teaching_sessions` VALUES (1,'2026-10-30 17:17:59.924083','2026-10-26 08:40:00.000000','2026-10-26 08:00:00.000000','S2 A','08:40:00',NULL,'B12','2026-10-26','08:00:00','COMPLETED','Mathematics',3),(2,'2026-10-30 17:17:59.933690','2026-10-27 08:40:00.000000','2026-10-27 08:00:00.000000','S2 A','08:40:00',NULL,'B12','2026-10-27','08:00:00','COMPLETED','Mathematics',3),(3,'2026-10-30 17:17:59.952052','2026-10-28 08:40:00.000000','2026-10-28 08:00:00.000000','S2 A','08:40:00',NULL,'B12','2026-10-28','08:00:00','COMPLETED','Mathematics',3),(4,'2026-10-30 17:17:59.966054','2026-10-29 08:40:00.000000','2026-10-29 08:00:00.000000','S2 A','08:40:00',NULL,'B12','2026-10-29','08:00:00','COMPLETED','Mathematics',3),(5,'2026-10-30 17:17:59.973053','2026-10-30 08:40:00.000000','2026-10-30 08:00:00.000000','S2 A','08:40:00',NULL,'B12','2026-10-30','08:00:00','COMPLETED','Mathematics',3),(6,'2026-10-30 17:17:59.981056','2026-10-26 09:20:00.000000','2026-10-26 08:40:00.000000','S2 B','09:20:00',NULL,'B04','2026-10-26','08:40:00','COMPLETED','English',4),(7,'2026-10-30 17:17:59.989054','2026-10-27 09:20:00.000000','2026-10-27 08:40:00.000000','S2 B','09:20:00',NULL,'B04','2026-10-27','08:40:00','COMPLETED','English',4),(8,'2026-10-30 17:18:00.012060','2026-10-28 09:20:00.000000','2026-10-28 08:40:00.000000','S2 B','09:20:00',NULL,'B04','2026-10-28','08:40:00','COMPLETED','English',4),(9,'2026-10-30 17:18:00.079576','2026-10-29 09:20:00.000000','2026-10-29 08:40:00.000000','S2 B','09:20:00',NULL,'B04','2026-10-29','08:40:00','COMPLETED','English',4),(10,'2026-10-30 17:18:00.089577','2026-10-30 09:30:00.000000','2026-10-30 08:54:00.000000','S2 B','09:20:00','Started late','B04','2026-10-30','08:40:00','COMPLETED','English',4),(11,'2026-10-30 17:18:00.099576','2026-10-26 10:00:00.000000','2026-10-26 09:20:00.000000','S3 A','10:00:00',NULL,'Lab 1','2026-10-26','09:20:00','COMPLETED','Biology',5),(12,'2026-10-30 17:18:00.109578','2026-10-27 10:00:00.000000','2026-10-27 09:20:00.000000','S3 A','10:00:00',NULL,'Lab 1','2026-10-27','09:20:00','COMPLETED','Biology',5),(13,'2026-10-30 17:18:00.119585','2026-10-28 10:00:00.000000','2026-10-28 09:20:00.000000','S3 A','10:00:00',NULL,'Lab 1','2026-10-28','09:20:00','COMPLETED','Biology',5),(14,'2026-10-30 17:18:00.127102','2026-10-29 10:00:00.000000','2026-10-29 09:20:00.000000','S3 A','10:00:00',NULL,'Lab 1','2026-10-29','09:20:00','COMPLETED','Biology',5),(15,'2026-10-30 17:18:00.135106',NULL,NULL,'S3 A','10:00:00','Teacher absent','Lab 1','2026-10-30','09:20:00','MISSED','Biology',5),(16,'2026-10-30 17:18:00.144102','2026-10-26 11:00:00.000000','2026-10-26 10:20:00.000000','S1 A','11:00:00',NULL,'B09','2026-10-26','10:20:00','COMPLETED','History',6),(17,'2026-10-30 17:18:00.153105','2026-10-27 11:00:00.000000','2026-10-27 10:20:00.000000','S1 A','11:00:00',NULL,'B09','2026-10-27','10:20:00','COMPLETED','History',6),(18,'2026-10-30 17:18:00.168101','2026-10-28 11:00:00.000000','2026-10-28 10:20:00.000000','S1 A','11:00:00',NULL,'B09','2026-10-28','10:20:00','COMPLETED','History',6),(19,'2026-10-30 17:18:00.176103','2026-10-29 11:00:00.000000','2026-10-29 10:20:00.000000','S1 A','11:00:00',NULL,'B09','2026-10-29','10:20:00','COMPLETED','History',6),(20,'2026-10-30 17:18:00.185103',NULL,NULL,'S1 A','11:00:00','Teacher on leave','B09','2026-10-30','10:20:00','MISSED','History',6),(21,'2026-10-30 17:18:00.196103','2026-10-26 11:40:00.000000','2026-10-26 11:00:00.000000','S3 B','11:40:00',NULL,'ICT Lab','2026-10-26','11:00:00','COMPLETED','Computer Science',7),(22,'2026-10-30 17:18:00.208102','2026-10-27 11:40:00.000000','2026-10-27 11:00:00.000000','S3 B','11:40:00',NULL,'ICT Lab','2026-10-27','11:00:00','COMPLETED','Computer Science',7),(23,'2026-10-30 17:18:00.219111','2026-10-28 11:40:00.000000','2026-10-28 11:00:00.000000','S3 B','11:40:00',NULL,'ICT Lab','2026-10-28','11:00:00','COMPLETED','Computer Science',7),(24,'2026-10-30 17:18:00.227624','2026-10-29 11:40:00.000000','2026-10-29 11:00:00.000000','S3 B','11:40:00',NULL,'ICT Lab','2026-10-29','11:00:00','COMPLETED','Computer Science',7),(25,'2026-10-30 17:18:00.235622','2026-10-30 11:40:00.000000','2026-10-30 11:00:00.000000','S3 B','11:40:00',NULL,'ICT Lab','2026-10-30','11:00:00','COMPLETED','Computer Science',7),(26,'2026-10-30 17:18:00.245625','2026-10-26 12:20:00.000000','2026-10-26 11:40:00.000000','S2 C','12:20:00',NULL,'Lab 2','2026-10-26','11:40:00','COMPLETED','Physics',8),(27,'2026-10-30 17:18:00.257624','2026-10-27 12:20:00.000000','2026-10-27 11:40:00.000000','S2 C','12:20:00',NULL,'Lab 2','2026-10-27','11:40:00','COMPLETED','Physics',8),(28,'2026-10-30 17:18:00.264625','2026-10-28 12:20:00.000000','2026-10-28 11:40:00.000000','S2 C','12:20:00',NULL,'Lab 2','2026-10-28','11:40:00','COMPLETED','Physics',8),(29,'2026-10-30 17:18:00.274623','2026-10-29 12:20:00.000000','2026-10-29 11:40:00.000000','S2 C','12:20:00',NULL,'Lab 2','2026-10-29','11:40:00','COMPLETED','Physics',8),(30,'2026-10-30 17:18:00.284623','2026-10-30 12:20:00.000000','2026-10-30 11:40:00.000000','S2 C','12:20:00',NULL,'Lab 2','2026-10-30','11:40:00','COMPLETED','Physics',8),(31,'2026-10-30 17:18:00.290622',NULL,NULL,'S2 A','16:40:00','Remedial for linear equations','B12','2026-10-30','16:00:00','MISSED','Mathematics',3);
/*!40000 ALTER TABLE `teaching_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `active` bit(1) NOT NULL,
  `department` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `face_descriptor` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `full_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `photo_path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `staff_code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `subject` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `username` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UK_r43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'2026-10-30 17:17:58.989568','','Administration','admin@school.rw',NULL,'School Admin','$2a$10$gIeVGF0kAvUM4VvogWFwk./qXaTBhYOWB7k404JdyIsZV/MtTvSwu','0788000001',NULL,'ADMIN','A-001','',NULL,'admin'),(2,'2026-10-30 17:17:59.209604','','Head of Studies','patrick@school.rw',NULL,'Patrick Ndayisaba','$2a$10$gIeVGF0kAvUM4VvogWFwk./qXaTBhYOWB7k404JdyIsZV/MtTvSwu','0788000002',NULL,'HEAD_OF_STUDIES','H-001','',NULL,'patrick'),(3,'2026-10-30 17:17:59.276128','','Sciences','alice@school.rw',NULL,'Alice Uwimana','$2a$10$gIeVGF0kAvUM4VvogWFwk./qXaTBhYOWB7k404JdyIsZV/MtTvSwu','0788111001',NULL,'TEACHER','T-001','Mathematics',NULL,'alice'),(4,'2026-10-30 17:17:59.288128','','Languages','jean@school.rw',NULL,'Jean Bosco Hakizimana','$2a$10$gIeVGF0kAvUM4VvogWFwk./qXaTBhYOWB7k404JdyIsZV/MtTvSwu','0788111002',NULL,'TEACHER','T-002','English','2026-10-30 18:29:49.314426','jean'),(5,'2026-10-30 17:17:59.301128','','Sciences','marie@school.rw',NULL,'Marie Claire Ingabire','$2a$10$gIeVGF0kAvUM4VvogWFwk./qXaTBhYOWB7k404JdyIsZV/MtTvSwu','0788111003',NULL,'TEACHER','T-003','Biology',NULL,'marie'),(6,'2026-10-30 17:17:59.312137','','Humanities','eric@school.rw','[0.12004192173480988,-0.06393816322088242,-0.0206147413700819,0.07516279816627502,0.09170626848936081,-0.04848315939307213,0.03918381407856941,-0.02271544747054577,0.07820215821266174,-0.04203927144408226,-0.1087864339351654,-0.06179075315594673,-0.044465549290180206,0.11785848438739777,-0.05276626721024513,-0.08602452278137207,0.0032006397377699614,-0.03431382030248642,-0.1878545880317688,-0.07178104668855667,0.015408340841531754,-0.00564234796911478,0.007669959217309952,0.09019888192415237,0.0031408213544636965,-0.12963396310806274,0.09126750379800797,0.001608485821634531,0.046309635043144226,0.1084398403763771,0.0977926254272461,0.1597735434770584,0.05753147229552269,0.23171411454677582,-0.11096394807100296,-0.08887672424316406,-0.11932708323001862,-0.16099250316619873,0.030556656420230865,-0.021329058334231377,-0.06425443291664124,-0.01757555641233921,0.1550116389989853,0.01099192164838314,0.07163017988204956,0.09748527407646179,0.04458934813737869,-0.11698043346405029,-0.09216347336769104,-0.045296959578990936,-0.02470216527581215,-0.19627903401851654,0.026199597865343094,-0.06629835814237595,0.02330382540822029,0.005528453271836042,-0.02389540895819664,-0.09069599956274033,0.031142817810177803,-0.06517083942890167,-0.25284120440483093,0.12226422876119614,-0.06732799857854843,-0.006674038711935282,0.021309418603777885,0.004595160949975252,-0.08323957771062851,0.11331041157245636,-0.00588954845443368,-0.10262969136238098,0.010325982235372066,0.03297438472509384,-0.04912427067756653,-0.04090699180960655,-0.1541973203420639,0.07238901406526566,0.026101019233465195,-0.21886196732521057,0.06602758914232254,-0.08810820430517197,0.1416100710630417,0.01686834543943405,0.048891693353652954,0.030808797106146812,0.09421367943286896,-0.11471501737833023,0.041114579886198044,0.012491962872445583,-0.02171487547457218,0.004932863172143698,0.03483574464917183,0.13526849448680878,-0.19412066042423248,0.005741198547184467,0.04475061222910881,-0.04253942891955376,-0.05381929874420166,-0.04571438208222389,-0.03102603182196617,0.034929487854242325,-0.025293797254562378,-0.11389388144016266,-0.1303091049194336,-0.0745205283164978,-0.14938494563102722,0.04098590463399887,-0.07305056601762772,-0.18557427823543549,-0.1691790372133255,-0.07574127614498138,0.07429861277341843,0.007235002238303423,-0.0930211991071701,-0.07561985403299332,0.10229022800922394,0.057813603430986404,-0.007320013362914324,0.038592204451560974,0.04415562376379967,0.01950034312903881,0.06021437793970108,0.08835379779338837,0.1371534913778305,0.026686636731028557,0.025884011760354042,-0.09479037672281265,0.061130668967962265,0.0035857888869941235]','Eric Habimana','$2a$10$gIeVGF0kAvUM4VvogWFwk./qXaTBhYOWB7k404JdyIsZV/MtTvSwu','0788111004',NULL,'TEACHER','T-004','History','2026-10-30 22:01:53.717380','eric'),(7,'2026-10-30 17:17:59.324138','','ICT','divine@school.rw',NULL,'Divine Mukamana','$2a$10$gIeVGF0kAvUM4VvogWFwk./qXaTBhYOWB7k404JdyIsZV/MtTvSwu','0788111005',NULL,'TEACHER','T-005','Computer Science',NULL,'divine'),(8,'2026-10-30 17:17:59.336657','','Sciences','grace@school.rw',NULL,'Grace Uwase','$2a$10$gIeVGF0kAvUM4VvogWFwk./qXaTBhYOWB7k404JdyIsZV/MtTvSwu','0788111006',NULL,'TEACHER','T-006','Physics',NULL,'grace');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'alice_teacher'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-31  7:01:05
