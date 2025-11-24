-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Nov 24, 2025 at 01:01 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `unpcat_alog`
--

-- --------------------------------------------------------

--
-- Table structure for table `accounts`
--

CREATE TABLE `accounts` (
  `account_id` int(11) NOT NULL,
  `username` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `account_type` varchar(11) NOT NULL DEFAULT 'caretaker',
  `caretaker_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `accounts`
--

INSERT INTO `accounts` (`account_id`, `username`, `password`, `account_type`, `caretaker_id`) VALUES
(1, 'admin', 'admin', 'admin', 0),
(3, 'test', 'test', 'caretaker', 1),
(4, 'testtest', 'test', 'caretaker', 4);

-- --------------------------------------------------------

--
-- Table structure for table `adopter`
--

CREATE TABLE `adopter` (
  `adopter_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `contact_info` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `adopter`
--

INSERT INTO `adopter` (`adopter_id`, `name`, `contact_info`) VALUES
(1, 'John Doe', 'john.doe@example.com'),
(2, 'Mary Smith', 'mary.smith@example.com');

-- --------------------------------------------------------

--
-- Table structure for table `adoption_status`
--

CREATE TABLE `adoption_status` (
  `status_id` int(11) NOT NULL,
  `cat_id` int(11) NOT NULL,
  `status` enum('Available','Adopted','Fostered','Missing','Other') NOT NULL,
  `changed_at` datetime NOT NULL DEFAULT current_timestamp(),
  `notes` varchar(255) DEFAULT NULL,
  `adopter_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `adoption_status`
--

INSERT INTO `adoption_status` (`status_id`, `cat_id`, `status`, `changed_at`, `notes`, `adopter_id`) VALUES
(1, 2, 'Available', '2025-07-01 09:00:00', 'Brought to campus rescue', NULL),
(2, 2, 'Adopted', '2025-10-01 10:00:00', 'Adopted at weekend adoption fair', 1),
(3, 1, 'Available', '2024-05-15 08:00:00', 'Resident library cat', NULL),
(4, 4, 'Fostered', '2025-08-01 12:00:00', 'Temporary foster placed', 2),
(5, 4, 'Other', '2025-11-23 13:46:49', 'Adoption request submitted', NULL),
(6, 7, 'Other', '2025-11-23 13:55:05', 'Adoption request submitted', NULL),
(9, 4, 'Available', '2025-11-23 15:53:47', 'Cat was returned due to reasons from the adopter', NULL),
(10, 4, 'Available', '2025-11-23 16:03:35', 'Last found near dorms\n', NULL),
(11, 4, 'Adopted', '2025-11-23 16:04:03', 'Adopted in AdoptionFest', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `area`
--

CREATE TABLE `area` (
  `area_id` int(11) NOT NULL,
  `area_name` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `area`
--

INSERT INTO `area` (`area_id`, `area_name`) VALUES
(1, 'College of Communication & Information Technology'),
(2, 'Main Library'),
(3, 'Lagoon'),
(4, 'Gym'),
(5, 'Laboratory School'),
(6, 'College of Teacher Education'),
(7, 'College of Business Administration and Accountancy'),
(8, 'College of Technology'),
(9, 'College of Hospitality and Tourism Management'),
(10, 'College of Architecture'),
(11, 'College of Health Sciences'),
(12, 'College of Criminal and Justice Education'),
(13, 'College of Fine Arts and Design'),
(14, 'College of Public Administration'),
(15, 'College of Engineering'),
(16, 'College of Arts and Sciences');

-- --------------------------------------------------------

--
-- Table structure for table `behavior`
--

CREATE TABLE `behavior` (
  `behavior_id` int(11) NOT NULL,
  `cat_id` int(11) NOT NULL,
  `personality` text DEFAULT NULL,
  `notes` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `behavior`
--

INSERT INTO `behavior` (`behavior_id`, `cat_id`, `personality`, `notes`) VALUES
(1, 1, 'Friendly', 'Enjoys being petted near library steps.'),
(2, 2, 'Shy', 'Startles easily, prefers quiet spots.'),
(3, 3, 'Aloof', 'Often hides under dormitory stairs.'),
(4, 4, 'Affectionate', 'Very social with students.'),
(9, 1, 'test', 'test');

-- --------------------------------------------------------

--
-- Table structure for table `caretaker`
--

CREATE TABLE `caretaker` (
  `caretaker_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `contact_info` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `caretaker`
--

INSERT INTO `caretaker` (`caretaker_id`, `name`, `contact_info`) VALUES
(1, 'Alice Santos', 'alice.santos@school.edu'),
(2, 'Ben Cruz', 'ben.cruz@school.edu'),
(3, 'Campus Facilities', 'facilities@school.edu'),
(4, 'test ey', 'test@gmail.com');

-- --------------------------------------------------------

--
-- Table structure for table `cat`
--

CREATE TABLE `cat` (
  `cat_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `gender` enum('Male','Female','Unknown') NOT NULL,
  `breed` varchar(100) DEFAULT NULL,
  `color` varchar(100) DEFAULT NULL,
  `area_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `cat`
--

INSERT INTO `cat` (`cat_id`, `name`, `gender`, `breed`, `color`, `area_id`) VALUES
(1, 'Mittens', 'Female', 'Domestic Shorthair', 'Calico', 2),
(2, 'Tiger', 'Male', 'Tabby', 'Brown tabby', 1),
(3, 'Shadow', 'Male', 'Domestic Shorthair', 'Black', 4),
(4, 'Luna', 'Female', 'Siamese', 'Cream', 5),
(5, 'Oreo', 'Unknown', 'Mixed', 'Black & White', 3),
(6, 'Trojan', 'Male', 'Unknown', 'Black and White', 1),
(7, 'test', 'Male', 'test', 'test', 16);

-- --------------------------------------------------------

--
-- Table structure for table `cat_caretaker`
--

CREATE TABLE `cat_caretaker` (
  `cat_id` int(11) NOT NULL,
  `caretaker_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `cat_caretaker`
--

INSERT INTO `cat_caretaker` (`cat_id`, `caretaker_id`) VALUES
(1, 1),
(1, 2),
(2, 2),
(3, 3),
(4, 1),
(4, 2),
(5, 2),
(6, 1),
(6, 3),
(7, 1);

-- --------------------------------------------------------

--
-- Table structure for table `health_record`
--

CREATE TABLE `health_record` (
  `health_id` int(11) NOT NULL,
  `cat_id` int(11) NOT NULL,
  `conditions` text DEFAULT NULL,
  `date` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `health_record`
--

INSERT INTO `health_record` (`health_id`, `cat_id`, `conditions`, `date`) VALUES
(1, 1, 'Vaccinated: Rabies, FVRCP. No chronic conditions.', '2025-09-15'),
(2, 2, 'Vaccinated: Rabies. Minor flea treatment given.', '2025-08-20'),
(3, 3, 'Unknown vaccination status. Observed limp on left paw (healed).', '2024-12-01'),
(4, 4, 'Spayed. Vaccinated: Rabies, FVRCP. Healthy.', '2025-07-10'),
(10, 6, 'ahhhhh', '2025-11-16');

-- --------------------------------------------------------

--
-- Table structure for table `incident_report`
--

CREATE TABLE `incident_report` (
  `incident_id` int(11) NOT NULL,
  `cat_id` int(11) NOT NULL,
  `date` datetime NOT NULL,
  `desc` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `incident_report`
--

INSERT INTO `incident_report` (`incident_id`, `cat_id`, `date`, `desc`) VALUES
(1, 1, '2025-10-05 14:30:00', 'Found near main entrance with a small cut; cleaned and treated.'),
(2, 3, '2025-09-20 19:10:00', 'Reported missing overnight; returned next morning.'),
(3, 5, '2025-10-10 11:00:00', 'Observed near cafeteria stealing food.');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `accounts`
--
ALTER TABLE `accounts`
  ADD PRIMARY KEY (`account_id`),
  ADD KEY `caretaker_id` (`caretaker_id`);

--
-- Indexes for table `adopter`
--
ALTER TABLE `adopter`
  ADD PRIMARY KEY (`adopter_id`);

--
-- Indexes for table `adoption_status`
--
ALTER TABLE `adoption_status`
  ADD PRIMARY KEY (`status_id`),
  ADD KEY `cat_id` (`cat_id`),
  ADD KEY `adopter_id` (`adopter_id`);

--
-- Indexes for table `area`
--
ALTER TABLE `area`
  ADD PRIMARY KEY (`area_id`);

--
-- Indexes for table `behavior`
--
ALTER TABLE `behavior`
  ADD PRIMARY KEY (`behavior_id`),
  ADD KEY `cat_id` (`cat_id`);

--
-- Indexes for table `caretaker`
--
ALTER TABLE `caretaker`
  ADD PRIMARY KEY (`caretaker_id`);

--
-- Indexes for table `cat`
--
ALTER TABLE `cat`
  ADD PRIMARY KEY (`cat_id`),
  ADD KEY `area_id` (`area_id`);

--
-- Indexes for table `cat_caretaker`
--
ALTER TABLE `cat_caretaker`
  ADD PRIMARY KEY (`cat_id`,`caretaker_id`),
  ADD KEY `caretaker_id` (`caretaker_id`);

--
-- Indexes for table `health_record`
--
ALTER TABLE `health_record`
  ADD PRIMARY KEY (`health_id`),
  ADD KEY `cat_id` (`cat_id`);

--
-- Indexes for table `incident_report`
--
ALTER TABLE `incident_report`
  ADD PRIMARY KEY (`incident_id`),
  ADD KEY `cat_id` (`cat_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `accounts`
--
ALTER TABLE `accounts`
  MODIFY `account_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `adopter`
--
ALTER TABLE `adopter`
  MODIFY `adopter_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT for table `adoption_status`
--
ALTER TABLE `adoption_status`
  MODIFY `status_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT for table `area`
--
ALTER TABLE `area`
  MODIFY `area_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- AUTO_INCREMENT for table `behavior`
--
ALTER TABLE `behavior`
  MODIFY `behavior_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT for table `caretaker`
--
ALTER TABLE `caretaker`
  MODIFY `caretaker_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `cat`
--
ALTER TABLE `cat`
  MODIFY `cat_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT for table `health_record`
--
ALTER TABLE `health_record`
  MODIFY `health_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15;

--
-- AUTO_INCREMENT for table `incident_report`
--
ALTER TABLE `incident_report`
  MODIFY `incident_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `adoption_status`
--
ALTER TABLE `adoption_status`
  ADD CONSTRAINT `adoption_status_ibfk_1` FOREIGN KEY (`cat_id`) REFERENCES `cat` (`cat_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `adoption_status_ibfk_2` FOREIGN KEY (`adopter_id`) REFERENCES `adopter` (`adopter_id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `behavior`
--
ALTER TABLE `behavior`
  ADD CONSTRAINT `behavior_ibfk_1` FOREIGN KEY (`cat_id`) REFERENCES `cat` (`cat_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `cat`
--
ALTER TABLE `cat`
  ADD CONSTRAINT `cat_ibfk_1` FOREIGN KEY (`area_id`) REFERENCES `area` (`area_id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `cat_caretaker`
--
ALTER TABLE `cat_caretaker`
  ADD CONSTRAINT `cat_caretaker_ibfk_1` FOREIGN KEY (`cat_id`) REFERENCES `cat` (`cat_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `cat_caretaker_ibfk_2` FOREIGN KEY (`caretaker_id`) REFERENCES `caretaker` (`caretaker_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `health_record`
--
ALTER TABLE `health_record`
  ADD CONSTRAINT `health_record_ibfk_1` FOREIGN KEY (`cat_id`) REFERENCES `cat` (`cat_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `incident_report`
--
ALTER TABLE `incident_report`
  ADD CONSTRAINT `incident_report_ibfk_1` FOREIGN KEY (`cat_id`) REFERENCES `cat` (`cat_id`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
