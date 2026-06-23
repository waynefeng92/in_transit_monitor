-- Add optimistic lock version column to vehicle_transit table
ALTER TABLE `vehicle_transit` ADD COLUMN `version` int NOT NULL DEFAULT '1' COMMENT '乐观锁版本号' AFTER `section_monitor_status`;

-- Initialize version for existing rows that may have NULL (safety net)
UPDATE `vehicle_transit` SET `version` = 1 WHERE `version` IS NULL OR `version` = 0;
