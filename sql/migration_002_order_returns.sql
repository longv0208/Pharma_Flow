-- Return requests on delivered online orders, plus the stock-in movement
-- written when staff accepts a return. Apply once on an existing database.
-- Safe to re-read: each statement is guarded so a second run does not fail.

SET @db := DATABASE();

SET @has_reason := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'online_orders' AND COLUMN_NAME = 'return_reason'
);
SET @sql := IF(@has_reason = 0,
  'ALTER TABLE online_orders ADD COLUMN return_reason VARCHAR(500) NULL AFTER order_status',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE online_orders
  MODIFY COLUMN order_status ENUM(
    'CHO_XU_LY','DA_XAC_NHAN','DANG_CHUAN_BI','SAN_SANG','DANG_GIAO','HOAN_TAT',
    'DA_HUY','TU_CHOI','YEU_CAU_HOAN','DA_HOAN'
  ) NOT NULL DEFAULT 'CHO_XU_LY';

ALTER TABLE inventory_movements
  MODIFY COLUMN movement_type ENUM(
    'NHAP_KHO','BAN_TAI_QUAY','GIU_HANG_ONLINE','GIAI_PHONG_GIU_HANG',
    'BAN_ONLINE','DIEU_CHINH','DIEU_CHINH_KIEM_KE','KHOA','MO_KHOA',
    'CHUYEN_VI_TRI','HOAN_HANG'
  ) NOT NULL;
