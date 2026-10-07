# Feature Ownership Map

Map từng chức năng → file liên quan, để ai commit lại phần đó thì biết đúng
phạm vi của mình. Khi thêm feature mới, thêm một mục ở đây.

Quy ước đường dẫn: tương đối từ repo root `Pharma_Flow/`. Commit message
lowercase, chia theo layer (model → dao → servlet → views/links) như các
commit trước.

## Auth / Account

- Servlet: `src/java/controller/AuthenController.java`
- DAO: `src/java/dao/UserDAO.java`, `src/java/dao/VerificationTokenDAO.java`,
  `src/java/dao/CustomerProfileDAO.java`
- Model: `src/java/model/User.java`, `src/java/model/CustomerProfile.java`
- Util: `src/java/util/PasswordUtil.java`, `src/java/util/TokenUtil.java`,
  `src/java/util/EmailSender.java`
- Views: `web/WEB-INF/views/auth/*` (login, register, verify-email,
  forgot-password, reset-otp, reset-password), `web/WEB-INF/views/error.jsp`
- Profile: `src/java/controller/ProfileServlet.java`,
  `web/WEB-INF/views/customer/profile.jsp`

## Storefront (customer-facing)

- Servlet: `src/java/controller/HomeServlet.java`,
  `src/java/controller/ProductServlet.java`
- DAO: `src/java/dao/ProductDAO.java`, `src/java/dao/CategoryDAO.java`
- Model: `src/java/model/Product.java`, `src/java/model/ProductType.java`,
  `src/java/model/Category.java`
- Views: `web/WEB-INF/views/home.jsp`, `products.jsp`, `product-detail.jsp`
- Shared includes: `web/WEB-INF/jspf/header.jspf`, `footer.jspf`

## Admin Catalog (Products / Categories / Suppliers)

- Servlet: `src/java/controller/AdminServlet.java` (products, categories,
  suppliers — dispatch `?action=`)
- DAO: `src/java/dao/ProductDAO.java`, `src/java/dao/CategoryDAO.java`,
  `src/java/dao/SupplierDAO.java`
- Model: `src/java/model/Supplier.java`
- Views: `web/WEB-INF/views/admin/product-*.jsp`, `category-*.jsp`,
  `supplier-*.jsp`, `dashboard.jsp`
- Sidebar: `web/WEB-INF/jspf/admin-sidebar.jspf` (label "Catalog",
  CHU_QUAN_QUAN_TRI only)

## Purchasing (Purchase Orders)

- Servlet: `src/java/controller/PurchaseOrderServlet.java`
- DAO: `src/java/dao/PurchaseOrderDAO.java`
- Model: `src/java/model/PurchaseOrder.java`,
  `src/java/model/PurchaseOrderItem.java`
- Views: `web/WEB-INF/views/admin/purchase-order-*.jsp`
- Sidebar: label "Purchasing" (CHU_QUAN_QUAN_TRI only)

## Inventory Visibility / Batches / History

- Servlet: `src/java/controller/InventoryServlet.java` (`list`, `product`,
  `batch`, `history`; POST `block-batch`, `unblock-batch`)
- DAO: `src/java/dao/InventoryDAO.java` — chứa `ALLOCATABLE` predicate,
  `statusForExpiry`, `insertMovement`, `LOW_STOCK_THRESHOLD`
- Model: `src/java/model/InventoryBatch.java`,
  `src/java/model/InventoryMovement.java`
- Views: `web/WEB-INF/views/admin/inventory-list.jsp`, `inventory-product.jsp`,
  `inventory-batch.jsp`, `inventory-history.jsp`
- Doc quy tắc tồn kho: `docs/inventory-availability-rules.md`

## Stock Receiving (Goods Receipts)

- Servlet: `src/java/controller/GoodsReceiptServlet.java`
- DAO: `src/java/dao/GoodsReceiptDAO.java` — `confirmReceipt` là transaction
  mẫu (find-or-create batch, PO received_qty, NHAP_KHO movement)
- Model: `src/java/model/GoodsReceipt.java`,
  `src/java/model/GoodsReceiptItem.java`
- Views: `web/WEB-INF/views/admin/goods-receipt-*.jsp`
- Sidebar link "Stock Receiving" trong Inventory section

## Inventory Adjustments

- Servlet: `src/java/controller/InventoryAdjustmentServlet.java`
- DAO: `src/java/dao/InventoryAdjustmentDAO.java` — `createAdjustment`
  transaction, reconciled-status logic (BI_KHOA stays, HET_HAN wins,
  qty<=0 → HET_HANG)
- Model: `src/java/model/InventoryAdjustment.java`
- Views: `web/WEB-INF/views/admin/inventory-adjustments.jsp`,
  `inventory-adjustment-form.jsp`
- Movement type `DIEU_CHINH`, link từ `inventory-history.jsp` và batch detail

## Stocktake

- Servlet: `src/java/controller/StocktakeServlet.java`
- DAO: `src/java/dao/StocktakeDAO.java` — `StocktakeResult`,
  `completeStocktake` transaction
- Model: `src/java/model/Stocktake.java`, `src/java/model/StocktakeItem.java`
- Views: `web/WEB-INF/views/admin/stocktake-list.jsp`, `stocktake-detail.jsp`
- Movement type `DIEU_CHINH_KIEM_KE`

## Inventory Alerts

- Servlet: `src/java/controller/InventoryAlertServlet.java`
- DAO: `src/java/dao/InventoryAlertDAO.java` — dynamic alert calculation +
  global settings
- Model: `src/java/model/InventoryAlert.java`,
  `src/java/model/InventoryAlertSetting.java`
- Views: `web/WEB-INF/views/admin/inventory-alerts.jsp`,
  `inventory-alert-settings.jsp`

## Point of Sale (POS)

- Servlet: `src/java/controller/PosServlet.java` — NHAN_VIEN only; GET
  `main`/`history`/`detail`, POST `add`, `update-qty`, `remove`, `clear`,
  `checkout`
- DAO: `src/java/dao/PosDAO.java` — `completeSale` là một transaction duy
  nhất: lock products (id ASC) + FEFO batches (`FOR UPDATE`, expiry ASC →
  batch_id ASC), với giỏ Rx thì insert 1 row `prescriptions` (audit kiểm tra
  thủ công, `validated_by` = users.user_id, `validated_at` = CURRENT_TIMESTAMP)
  sau khi allocate thành công, insert `sale_transactions` (CHO_XU_LY→HOAN_TAT)
  + `sale_items` + `sale_item_batch_allocations`, giảm `on_hand_quantity`, ghi
  `inventory_movements` BAN_TAI_QUAY, refresh batch status
- Model: `src/java/model/PosCartItem.java` (session cart, không phải bảng),
  `SaleTransaction.java`, `SaleItem.java`,
  `SaleItemBatchAllocation.java`, `Prescription.java`
- Views: `web/WEB-INF/views/pos/dashboard.jsp` (search + cart + checkout; giỏ
  Rx thì form checkout chứa `prescriber` + `healthcareFacility` +
  checkbox `prescriptionChecked`), `pos-detail.jsp` (receipt + khối xác nhận
  đơn thuốc), `pos-history.jsp` (cột Đơn thuốc = Đã kiểm tra/—)
- Session keys: `posCart`, `posCheckoutToken`
- Sidebar: block "Sales → Point of Sale" (NHAN_VIEN only); `inventory-history.jsp`
  nhánh `BAN_TAI_QUAY` link `/pos?action=detail&id=`
- Lưu ý: `sale_transactions.staff_id` → `staff_profiles.staff_id` (resolve qua
  `user_id`); `inventory_movements.performed_by` → `users.user_id`. Giỏ chỉ
  có KHONG_KE_DON thì `prescription_id` NULL, không ghi row `prescriptions`.
  HAN_CHE hiện bị chặn (`RESTRICTED_NOT_ALLOWED`) — chưa có rule approve.

## Online Ordering (customer)

- Servlet: `src/java/controller/OnlineOrderServlet.java` (`/orders`,
  KHACH_HANG only; GET `checkout`/`detail`, POST `place`/`cancel`)
- DAO: `src/java/dao/OnlineOrderDAO.java` — `placeOrder` / `cancelOrder`
  transactions (FEFO reservation, GIU_HANG_ONLINE / GIAI_PHONG_GIU_HANG
  movements), `resolveCustomerId`
- Model: `src/java/model/OnlineOrder.java`, `OnlineOrderItem.java`,
  `InventoryReservation.java`, `Cart.java`, `CartItem.java`
- Cart: `src/java/controller/CartServlet.java` (`/cart`),
  `src/java/dao/CartDAO.java`
- Views: `web/WEB-INF/views/customer/cart.jsp`, `checkout.jsp`,
  `order-list.jsp`, `order-detail.jsp`
- Header: `web/WEB-INF/jspf/header.jspf` — cart + orders icons for KHACH_HANG

## Online Order Fulfillment (staff)

- Route: `/fulfillment`
- Role: `NHAN_VIEN` only (redirect KHACH_HANG / NHAN_VIEN_GIAO_HANG /
  CHU_QUAN_QUAN_TRI → /home; guest → /authen?action=login)
- Servlet: `src/java/controller/OnlineFulfillmentServlet.java` — GET `list`
  (default), `detail?id=`; POST `confirm`, `prepare`, `ready`, `reject`
- DAO: `src/java/dao/OnlineFulfillmentDAO.java` — `FulfillmentResult`,
  staff list/detail queries, `findOrderReservations` (batch picking JOIN),
  `transition` (confirm/prepare/ready), `rejectOrder` (release tx)
- Models reused: `OnlineOrder`, `OnlineOrderItem`, `InventoryReservation`
  (extended with display-only batchNumber/expiryDate/storageLocation/
  productName/sku)
- Views: `web/WEB-INF/views/staff/online-order-list.jsp`,
  `web/WEB-INF/views/staff/online-order-detail.jsp`
- Transition matrix (enforced in DAO under `online_orders` row lock):
  `CHO_XU_LY→DA_XAC_NHAN` (confirm), `DA_XAC_NHAN→DANG_CHUAN_BI` (prepare),
  `DANG_CHUAN_BI→SAN_SANG` (ready), `CHO_XU_LY→TU_CHOI` (reject)
- Reservation behaviour: confirm/prepare/ready move status only — no stock
  or reservation change; `DANG_GIU` stays through `SAN_SANG`. Reject is the
  only inventory write: `reserved_quantity` down, reservations
  `DANG_GIU→DA_GIAI_PHONG` + `released_at`, one `GIAI_PHONG_GIU_HANG`
  movement per reservation (`performed_by` = staff `users.user_id`,
  `reference_type='DON_HANG_ONLINE'`). `on_hand` never changes anywhere in
  this module.
- Module ends at `SAN_SANG`: `DANG_GIAO` / `HOAN_TAT` / `BAN_ONLINE` /
  `DA_HOAN_TAT` are NOT implemented (shipper module).
- Sidebar: `adminNav == 'online-orders'` entry in the NHAN_VIEN "Bán hàng"
  section of `admin-sidebar.jspf`
- `inventory-history.jsp`: `reference_type='DON_HANG_ONLINE'` → link
  `/fulfillment?action=detail&id=` labelled "Đơn online #N"

## Delivery / Shipper

- Route: `/delivery`
- Role: `NHAN_VIEN_GIAO_HANG` only (redirect KHACH_HANG / NHAN_VIEN /
  CHU_QUAN_QUAN_TRI → /home; guest → /authen?action=login)
- Servlet: `src/java/controller/DeliveryServlet.java` — GET `list` (default),
  `detail?id=`; POST `start`, `complete`
- DAO: `src/java/dao/DeliveryDAO.java` — `DeliveryResult`, queue queries
  (status whitelist SAN_SANG/DANG_GIAO/HOAN_TAT, ordering
  DANG_GIAO→SAN_SANG→HOAN_TAT newest first), `startDelivery` stock-out tx,
  `completeDelivery` status-only tx
- Transition matrix (enforced in DAO under `online_orders` row lock):
  `SAN_SANG→DANG_GIAO` (start), `DANG_GIAO→HOAN_TAT` (complete)
- Stock-out happens ONLY at `SAN_SANG→DANG_GIAO`: per `DANG_GIU` reservation
  the batch loses `on_hand_quantity` AND `reserved_quantity` together
  (saleable stock unchanged), reservation → `DA_HOAN_TAT` (`released_at`
  untouched), one `BAN_ONLINE` movement per reservation
  (`performed_by` = shipper `users.user_id`,
  `reference_type='DON_HANG_ONLINE'`). Batch status refreshed: on_hand 0 →
  `HET_HANG`, else `InventoryDAO.statusForExpiry`. `HOAN_TAT` never touches
  inventory — it only verifies `DA_HOAN_TAT` sums (FULFILLMENT_MISMATCH).
- Shared queue: no shipment/assignment table — first tx to lock the order row
  wins, a racing second click fails INVALID_STATUS. Shipper never picks
  batches; the customer's FEFO reservations are authoritative and never
  re-run. Refuses to dispatch expired (`expiry_date <= CURDATE()`), HET_HAN
  or BI_KHOA batches (BATCH_NOT_DISPATCHABLE) and reservations whose batch
  no longer covers the qty (BATCH_INSUFFICIENT / BATCH_NOT_FOUND).
- Views: `web/WEB-INF/views/shipper/delivery-list.jsp`,
  `web/WEB-INF/views/shipper/delivery-detail.jsp`
- Landing: `AuthenController.targetFor` + `ProfileServlet.targetFor` map
  `NHAN_VIEN_GIAO_HANG` → `/delivery`; `admin-nav.jspf` shows brand link
  `/delivery` + tag "Giao hàng"; `admin-sidebar.jspf` has a shipper-only
  "Giao hàng" section and the "Kho" section is scoped to
  CHU_QUAN_QUAN_TRI / NHAN_VIEN.

## Shared / cross-cutting

- `src/java/db/DBContext.java` — JDBC entry point dùng chung mọi DAO
- `web/WEB-INF/jspf/admin-head.jspf`, `admin-nav.jspf`, `admin-sidebar.jspf`,
  `admin-footer.jspf` — shell admin dùng chung
- `web/css/main.css` — toàn bộ style; thêm class theo section comment
  (`/* ---------- X ---------- */`)
- `db.sql` — schema + seed; đổi schema thì báo trước, không sửa lặng
- `rule.md` — coding contract (đọc trước khi code)

## Admin Dashboard / Reports

- Route `/admin` (dashboard) + `/reports` (chi tiết) — CHU_QUAN_QUAN_TRI only.
- Servlet: `src/java/controller/ReportServlet.java` (GET only, ?from/?to
  yyyy-MM-dd, default tháng hiện tại), `AdminServlet.handleDashboard` nạp
  metric cards + top-5 tháng + workflow counts.
- DAO: `src/java/dao/ReportDAO.java` — read-only, nested projections
  `SalesSummary`/`DailySalesRow`/`TopProductRow`/`OrderStatusSummary`/
  `InventorySummary`; alerts qua `InventoryAlertDAO.getAlertSummary`.
- Revenue: POS = `sale_transactions` HOAN_TAT theo `sale_datetime`; Online =
  `online_orders` HOAN_TAT theo `updated_at` (schema không có `completed_at`,
  HOAN_TAT là terminal nên `updated_at` là mốc hoàn thành). Không cộng
  `inventory_movements` (audit, không phải doanh thu). Top products dùng
  `subtotal` lưu trên item — không nhân `products.selling_price` hiện tại.
- Saleable = `SUM(on_hand - reserved)` chỉ trên `InventoryDAO.ALLOCATABLE`
  (CO_SAN/SAP_HET_HAN + expiry > CURDATE()); BI_KHOA/HET_HAN/HET_HANG/expired
  không tính. Expiry counts đọc `expiry_date` trực tiếp (authoritative).
- Half-open date filter: `>= from` và `< to + 1 day` qua PreparedStatement.
- Views: `web/WEB-INF/views/admin/reports.jsp`, `dashboard.jsp` (metric
  cards `.stat-card` + share-bar CSS trong `main.css`); sidebar "Hệ thống →
  Báo cáo" chỉ CHU_QUAN_QUAN_TRI.
- Read-only: không INSERT/UPDATE/DELETE, không refresh batch status, không
  tạo alert rows, không đổi schema.

## Staff dashboard

- Servlet: `src/java/controller/StaffServlet.java`
- View: `web/WEB-INF/views/staff/dashboard.jsp`
