# Warehouse Location — Regression Checklist (TASK 12)

Prerequisites: backup DB, run `sql/migration_001_warehouse_locations.sql`, redeploy WAR.

## Location master
1. Create Zone / Rack / Bin with unique codes → OK.
2. Duplicate code under same parent → `err=duplicate`.
3. Deactivate bin/rack/zone with on-hand > 0 → `err=hasstock`.

## Goods receipt
4. Accept one batch into one active bin → batch detail shows location; batch totals = sum locations.
5. Split same batch across two bins (two lines) → two location rows; PO qty controlled.
6. Confirm with inactive / missing bin → `bininactive` / `binrequired`; full rollback.

## Stocktake
7. Stocktake one Bin with shortage → complete adjusts that bin only; movement `DIEU_CHINH_KIEM_KE`.
8. Stocktake one Zone spanning racks → items = all batch+bin in zone.
9. Stocktake while reservation exists; actual &lt; reserved → `belowreserved`.
10. Stock moves during count (system snapshot ≠ live) → `stalecounts`.

## Sales / online
11. POS + online reserve same batch across bins → FEFO expiry then bin; no negative saleable.
12. Cancel/reject releases reservation at reserved bin; dispatch `consumeReservedAtBin`.

## Transfer & invariants
13. Transfer qty ≤ saleable at source; batch on-hand unchanged; movement `CHUYEN_VI_TRI`.
14. After each flow:  
    `batch.on_hand = SUM(location.on_hand)` and `batch.reserved = SUM(location.reserved)`.

## SQL reconcile
```sql
SELECT b.batch_id, b.on_hand_quantity, COALESCE(SUM(l.on_hand_quantity),0) loc_oh,
       b.reserved_quantity, COALESCE(SUM(l.reserved_quantity),0) loc_rs
FROM inventory_batches b
LEFT JOIN inventory_batch_locations l ON l.batch_id = b.batch_id
GROUP BY b.batch_id
HAVING b.on_hand_quantity <> loc_oh OR b.reserved_quantity <> loc_rs;
```
Expect 0 rows after migration and after smoke tests.
