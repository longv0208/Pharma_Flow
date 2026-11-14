-- Product barcode is unused. Drop the column and its unique index.
-- Existing barcode values are discarded; no other table references them.
ALTER TABLE products DROP COLUMN barcode;
