-- 将 order_info 唯一键从 (vin, orderReleaseTime) 改为 vin
-- 同时放宽 order_release_time 允许为 NULL
ALTER TABLE `order_info` 
    DROP INDEX `uk_vin_order`,
    ADD UNIQUE KEY `uk_vin` (`vin`),
    MODIFY COLUMN `order_release_time` datetime NULL COMMENT '订单释放时间';
