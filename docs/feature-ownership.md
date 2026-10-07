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

## Shared / cross-cutting

- `src/java/db/DBContext.java` — JDBC entry point dùng chung mọi DAO
- `web/WEB-INF/jspf/admin-head.jspf`, `admin-nav.jspf`, `admin-sidebar.jspf`,
  `admin-footer.jspf` — shell admin dùng chung
- `web/css/main.css` — toàn bộ style; thêm class theo section comment
  (`/* ---------- X ---------- */`)
- `db.sql` — schema + seed; đổi schema thì báo trước, không sửa lặng
- `rule.md` — coding contract (đọc trước khi code)

## Staff dashboard

- Servlet: `src/java/controller/StaffServlet.java`
- View: `web/WEB-INF/views/staff/dashboard.jsp`
