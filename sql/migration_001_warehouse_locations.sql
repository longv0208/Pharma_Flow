-- PharmaFlow F04 — Warehouse Location & Stocktake Upgrade
-- Run AFTER backup. Target: MySQL 8, schema matching db_vi_full.sql.
-- Strategy: dual quantity (batch totals + inventory_batch_locations), TMP holding bin for legacy stock.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 1) Location hierarchy
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS storage_zones (
  zone_id     BIGINT NOT NULL AUTO_INCREMENT,
  zone_code   VARCHAR(50)  NOT NULL,
  zone_name   VARCHAR(150) NOT NULL,
  status      ENUM('HOAT_DONG','NGUNG_HOAT_DONG') NOT NULL DEFAULT 'HOAT_DONG',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (zone_id),
  UNIQUE KEY uq_zone_code (zone_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS storage_racks (
  rack_id     BIGINT NOT NULL AUTO_INCREMENT,
  zone_id     BIGINT NOT NULL,
  rack_code   VARCHAR(50)  NOT NULL,
  rack_name   VARCHAR(150) NOT NULL,
  status      ENUM('HOAT_DONG','NGUNG_HOAT_DONG') NOT NULL DEFAULT 'HOAT_DONG',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (rack_id),
  UNIQUE KEY uq_rack_code_per_zone (zone_id, rack_code),
  KEY idx_rack_zone (zone_id),
  CONSTRAINT fk_rack_zone FOREIGN KEY (zone_id) REFERENCES storage_zones (zone_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS storage_bins (
  bin_id      BIGINT NOT NULL AUTO_INCREMENT,
  rack_id     BIGINT NOT NULL,
  bin_code    VARCHAR(50) NOT NULL,
  status      ENUM('HOAT_DONG','NGUNG_HOAT_DONG') NOT NULL DEFAULT 'HOAT_DONG',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (bin_id),
  UNIQUE KEY uq_bin_code_per_rack (rack_id, bin_code),
  KEY idx_bin_rack (rack_id),
  CONSTRAINT fk_bin_rack FOREIGN KEY (rack_id) REFERENCES storage_racks (rack_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS inventory_batch_locations (
  batch_location_id BIGINT NOT NULL AUTO_INCREMENT,
  batch_id          BIGINT NOT NULL,
  bin_id            BIGINT NOT NULL,
  on_hand_quantity  INT NOT NULL DEFAULT 0,
  reserved_quantity INT NOT NULL DEFAULT 0,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (batch_location_id),
  UNIQUE KEY uq_batch_bin (batch_id, bin_id),
  KEY idx_ibl_bin (bin_id),
  KEY idx_ibl_batch (batch_id),
  CONSTRAINT fk_ibl_batch FOREIGN KEY (batch_id) REFERENCES inventory_batches (batch_id),
  CONSTRAINT fk_ibl_bin   FOREIGN KEY (bin_id)   REFERENCES storage_bins (bin_id),
  CONSTRAINT chk_ibl_on_hand  CHECK (on_hand_quantity >= 0),
  CONSTRAINT chk_ibl_reserved CHECK (reserved_quantity >= 0),
  CONSTRAINT chk_ibl_reserved_le_on_hand CHECK (reserved_quantity <= on_hand_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 2) TMP holding area for legacy / unverified storage_location strings
-- ---------------------------------------------------------------------------
INSERT INTO storage_zones (zone_code, zone_name, status)
SELECT 'TMP', N'Khu vực tạm — đối soát vị trí cũ', 'HOAT_DONG'
WHERE NOT EXISTS (SELECT 1 FROM storage_zones WHERE zone_code = 'TMP');

INSERT INTO storage_racks (zone_id, rack_code, rack_name, status)
SELECT z.zone_id, 'UNASSIGNED', N'Chưa gán kệ', 'HOAT_DONG'
FROM storage_zones z
WHERE z.zone_code = 'TMP'
  AND NOT EXISTS (
    SELECT 1 FROM storage_racks r
    WHERE r.zone_id = z.zone_id AND r.rack_code = 'UNASSIGNED'
  );

INSERT INTO storage_bins (rack_id, bin_code, status)
SELECT r.rack_id, 'BIN-UNKNOWN', 'HOAT_DONG'
FROM storage_racks r
JOIN storage_zones z ON z.zone_id = r.zone_id
WHERE z.zone_code = 'TMP' AND r.rack_code = 'UNASSIGNED'
  AND NOT EXISTS (
    SELECT 1 FROM storage_bins b
    WHERE b.rack_id = r.rack_id AND b.bin_code = 'BIN-UNKNOWN'
  );

-- ---------------------------------------------------------------------------
-- 3) Migrate existing batch qty into TMP bin (1:1 — do not invent Zone/Rack/Bin)
-- ---------------------------------------------------------------------------
INSERT INTO inventory_batch_locations (batch_id, bin_id, on_hand_quantity, reserved_quantity)
SELECT b.batch_id, bn.bin_id, b.on_hand_quantity, b.reserved_quantity
FROM inventory_batches b
CROSS JOIN (
  SELECT bn.bin_id
  FROM storage_bins bn
  JOIN storage_racks r ON r.rack_id = bn.rack_id
  JOIN storage_zones z ON z.zone_id = r.zone_id
  WHERE z.zone_code = 'TMP' AND r.rack_code = 'UNASSIGNED' AND bn.bin_code = 'BIN-UNKNOWN'
  LIMIT 1
) bn
WHERE NOT EXISTS (
  SELECT 1 FROM inventory_batch_locations x WHERE x.batch_id = b.batch_id
)
AND (b.on_hand_quantity > 0 OR b.reserved_quantity > 0);

-- ---------------------------------------------------------------------------
-- 4) Alter related tables
-- ---------------------------------------------------------------------------
ALTER TABLE goods_receipt_items
  ADD COLUMN bin_id BIGINT NULL AFTER batch_id,
  ADD KEY idx_gri_bin (bin_id),
  ADD CONSTRAINT fk_gri_bin FOREIGN KEY (bin_id) REFERENCES storage_bins (bin_id);

ALTER TABLE inventory_reservations
  ADD COLUMN bin_id BIGINT NULL AFTER batch_id,
  ADD KEY idx_res_bin (bin_id),
  ADD CONSTRAINT fk_res_bin FOREIGN KEY (bin_id) REFERENCES storage_bins (bin_id);

-- Place active reservations onto TMP bin (verified only as batch totals; location unknown)
UPDATE inventory_reservations r
JOIN (
  SELECT bn.bin_id
  FROM storage_bins bn
  JOIN storage_racks rk ON rk.rack_id = bn.rack_id
  JOIN storage_zones z ON z.zone_id = rk.zone_id
  WHERE z.zone_code = 'TMP' AND rk.rack_code = 'UNASSIGNED' AND bn.bin_code = 'BIN-UNKNOWN'
  LIMIT 1
) t
SET r.bin_id = t.bin_id
WHERE r.bin_id IS NULL AND r.status = 'DANG_GIU';

ALTER TABLE inventory_adjustments
  ADD COLUMN bin_id BIGINT NULL AFTER batch_id,
  ADD KEY idx_adj_bin (bin_id),
  ADD CONSTRAINT fk_adj_bin FOREIGN KEY (bin_id) REFERENCES storage_bins (bin_id);

ALTER TABLE stocktakes
  ADD COLUMN scope_type ENUM('TOAN_KHO','ZONE','RACK','BIN') NOT NULL DEFAULT 'TOAN_KHO' AFTER created_by,
  ADD COLUMN scope_id BIGINT NULL AFTER scope_type;

-- stocktake items: one line per batch+bin
-- MySQL keeps uq_stocktake_batch referenced by FKs — drop FKs first.
ALTER TABLE stocktake_items DROP FOREIGN KEY fk_stocktake_item_batch;
ALTER TABLE stocktake_items DROP FOREIGN KEY fk_stocktake_item_stocktake;
ALTER TABLE stocktake_items DROP INDEX uq_stocktake_batch;
ALTER TABLE stocktake_items
  ADD COLUMN bin_id BIGINT NULL AFTER batch_id;

-- Backfill historical stocktake items onto TMP bin so unique key can be added
UPDATE stocktake_items si
JOIN (
  SELECT bn.bin_id
  FROM storage_bins bn
  JOIN storage_racks rk ON rk.rack_id = bn.rack_id
  JOIN storage_zones z ON z.zone_id = rk.zone_id
  WHERE z.zone_code = 'TMP' AND rk.rack_code = 'UNASSIGNED' AND bn.bin_code = 'BIN-UNKNOWN'
  LIMIT 1
) t
SET si.bin_id = t.bin_id
WHERE si.bin_id IS NULL;

ALTER TABLE stocktake_items
  MODIFY COLUMN bin_id BIGINT NOT NULL,
  ADD UNIQUE KEY uq_stocktake_batch_bin (stocktake_id, batch_id, bin_id),
  ADD KEY idx_sti_bin (bin_id),
  ADD CONSTRAINT fk_sti_bin FOREIGN KEY (bin_id) REFERENCES storage_bins (bin_id),
  ADD CONSTRAINT fk_stocktake_item_batch FOREIGN KEY (batch_id) REFERENCES inventory_batches (batch_id),
  ADD CONSTRAINT fk_stocktake_item_stocktake FOREIGN KEY (stocktake_id) REFERENCES stocktakes (stocktake_id);

ALTER TABLE inventory_movements
  ADD COLUMN from_bin_id BIGINT NULL AFTER batch_id,
  ADD COLUMN to_bin_id   BIGINT NULL AFTER from_bin_id,
  ADD KEY idx_mv_from_bin (from_bin_id),
  ADD KEY idx_mv_to_bin (to_bin_id),
  ADD CONSTRAINT fk_mv_from_bin FOREIGN KEY (from_bin_id) REFERENCES storage_bins (bin_id),
  ADD CONSTRAINT fk_mv_to_bin   FOREIGN KEY (to_bin_id)   REFERENCES storage_bins (bin_id);

ALTER TABLE inventory_movements
  MODIFY COLUMN movement_type ENUM(
    'NHAP_KHO','BAN_TAI_QUAY','GIU_HANG_ONLINE','GIAI_PHONG_GIU_HANG',
    'BAN_ONLINE','DIEU_CHINH','DIEU_CHINH_KIEM_KE','KHOA','MO_KHOA','CHUYEN_VI_TRI'
  ) NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- 5) Post-migration reconciliation checks (manual review)
-- ---------------------------------------------------------------------------
-- Batch totals must equal sum of locations:
-- SELECT b.batch_id, b.on_hand_quantity AS batch_oh,
--        COALESCE(SUM(l.on_hand_quantity),0) AS loc_oh,
--        b.reserved_quantity AS batch_rs,
--        COALESCE(SUM(l.reserved_quantity),0) AS loc_rs
-- FROM inventory_batches b
-- LEFT JOIN inventory_batch_locations l ON l.batch_id = b.batch_id
-- GROUP BY b.batch_id
-- HAVING batch_oh <> loc_oh OR batch_rs <> loc_rs;
