-- MVP 补充唯一约束与规范化字段（基于 database_enhanced_v3.sql）
-- 注意：在执行前请备份数据库，并评估历史数据是否有冲突

-- 用户表：增加规范化手机号列与唯一索引（若不存在）
ALTER TABLE `users`
  ADD COLUMN IF NOT EXISTS `phone_normalized` VARCHAR(32) NULL AFTER `phone`;

-- 将历史 phone 复制到规范化列（简化：去空格与破折号；如需更严格可在应用层完成）
UPDATE `users`
SET `phone_normalized` = REPLACE(REPLACE(TRIM(`phone`), ' ', ''), '-', '')
WHERE `phone_normalized` IS NULL OR `phone_normalized` = '';

-- 唯一索引（用户）：避免重复手机号
DROP INDEX IF EXISTS `uk_user_phone` ON `users`;
CREATE UNIQUE INDEX `uk_user_phone` ON `users`(`phone_normalized`);

-- 客资表：增加规范化手机号列与唯一索引（若不存在）
ALTER TABLE `customer_leads`
  ADD COLUMN IF NOT EXISTS `phone_normalized` VARCHAR(32) NULL AFTER `phone`;

-- 填充历史数据
UPDATE `customer_leads`
SET `phone_normalized` = REPLACE(REPLACE(TRIM(`phone`), ' ', ''), '-', '')
WHERE `phone_normalized` IS NULL OR `phone_normalized` = '';

-- 删除旧的 uniq_phone 索引，改用规范化列唯一
DROP INDEX IF EXISTS `uniq_phone` ON `customer_leads`;
CREATE UNIQUE INDEX `uk_lead_phone` ON `customer_leads`(`phone_normalized`);

-- 如存在 wechat_union_id 列，则增加唯一索引（防重复）
SET @has_union := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_leads' AND COLUMN_NAME = 'wechat_union_id'
);
SET @sql := IF(@has_union>0, 'CREATE UNIQUE INDEX IF NOT EXISTS uk_lead_union ON customer_leads(wechat_union_id);', 'SELECT 1;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

