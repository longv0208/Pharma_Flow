package dao;

import db.DBContext;
import model.PosCartItem;
import model.Product;
import model.ProductType;
import model.Prescription;
import model.SaleItem;
import model.SaleItemBatchAllocation;
import model.SaleTransaction;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access to everything the POS checkout touches: `products` (search + price),
 * `inventory_batches` (saleable stock + FEFO allocation),
 * `sale_transactions` + `sale_items` + `sale_item_batch_allocations` (the sale
 * itself), `inventory_movements` (POS_SALE audit rows), `staff_profiles`
 * (staff_id resolution) and `prescriptions` (Rx audit record).
 *
 * completeSale(...) runs in ONE JDBC transaction — a completed sale can never
 * exist without its items, its batch allocations, the stock decrement and the
 * movement rows (rule.md §28). Nothing the browser sent is trusted: prices and
 * stock are re-read inside the transaction. An RX sale also writes one
 * `prescriptions` audit row (staff manually checked an external paper Rx) in
 * the same transaction.
 */
public class PosDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(PosDAO.class.getName());

    /** Payment methods the till accepts — mirror of the DB enum. */
    public static final String[] PAYMENT_METHODS = {"CASH", "BANK_TRANSFER", "CARD"};

    /**
     * Result of a completeSale attempt — a machine-readable error code the
     * servlet maps to ?err= plus the new sale id on success.
     */
    public static class CheckoutResult {
        public final boolean ok;
        public final String error;
        public final String detail;
        public final long saleId;

        private CheckoutResult(boolean ok, String error, String detail, long saleId) {
            this.ok = ok;
            this.error = error;
            this.detail = detail;
            this.saleId = saleId;
        }

        public static CheckoutResult success(long saleId) {
            return new CheckoutResult(true, null, null, saleId);
        }

        public static CheckoutResult fail(String error) {
            return new CheckoutResult(false, error, null, -1);
        }

        public static CheckoutResult fail(String error, String detail) {
            return new CheckoutResult(false, error, detail, -1);
        }
    }

    /* ==================== product search ==================== */
    /**
     * POS product search — name / SKU / barcode LIKE, ACTIVE products only.
     * `online_sale_allowed` is deliberately ignored: that flag gates the
     * customer storefront, not the counter. Each row carries its saleable
     * quantity (SUM of on_hand - reserved over allocatable batches).
     */
    public List<Product> findProductsForPos(String keyword) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.product_id, p.product_name, p.sku, p.barcode, ");
        sql.append("p.product_type, p.selling_unit, p.selling_price, p.status, ");
        sql.append("COALESCE(SUM(CASE WHEN ").append(InventoryDAO.ALLOCATABLE);
        sql.append(" THEN b.on_hand_quantity - b.reserved_quantity ELSE 0 END),0) ");
        sql.append("AS saleable ");
        sql.append("FROM products p ");
        sql.append("LEFT JOIN inventory_batches b ON b.product_id = p.product_id ");
        sql.append("WHERE p.status = 'ACTIVE' ");
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (p.product_name LIKE ? OR p.sku LIKE ? OR p.barcode LIKE ?) ");
        }
        sql.append("GROUP BY p.product_id, p.product_name, p.sku, p.barcode, ");
        sql.append("p.product_type, p.selling_unit, p.selling_price, p.status ");
        sql.append("ORDER BY p.product_name ASC LIMIT 30");

        List<Product> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql.toString());
            if (keyword != null && !keyword.isEmpty()) {
                String like = "%" + keyword + "%";
                statement.setString(1, like);
                statement.setString(2, like);
                statement.setString(3, like);
            }
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Product p = new Product();
                p.setProductId(resultSet.getLong("product_id"));
                p.setProductName(resultSet.getString("product_name"));
                p.setSku(resultSet.getString("sku"));
                p.setBarcode(resultSet.getString("barcode"));
                p.setProductType(ProductType.fromString(resultSet.getString("product_type")));
                p.setSellingUnit(resultSet.getString("selling_unit"));
                p.setSellingPrice(resultSet.getBigDecimal("selling_price"));
                p.setStatus(resultSet.getString("status"));
                p.setAvailableQuantity(resultSet.getLong("saleable"));
                out.add(p);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findProductsForPos failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /**
     * One product by PK for the add-to-cart check — the same field set as the
     * search rows plus its saleable quantity, or null when the id is unknown.
     * INACTIVE products are returned too so the caller can report
     * PRODUCT_INACTIVE instead of a bare "not found".
     */
    public Product findPosProduct(long productId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.product_id, p.product_name, p.sku, p.barcode, ");
        sql.append("p.product_type, p.selling_unit, p.selling_price, p.status, ");
        sql.append("COALESCE(SUM(CASE WHEN ").append(InventoryDAO.ALLOCATABLE);
        sql.append(" THEN b.on_hand_quantity - b.reserved_quantity ELSE 0 END),0) ");
        sql.append("AS saleable ");
        sql.append("FROM products p ");
        sql.append("LEFT JOIN inventory_batches b ON b.product_id = p.product_id ");
        sql.append("WHERE p.product_id = ? ");
        sql.append("GROUP BY p.product_id, p.product_name, p.sku, p.barcode, ");
        sql.append("p.product_type, p.selling_unit, p.selling_price, p.status");
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql.toString());
            statement.setLong(1, productId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                Product p = new Product();
                p.setProductId(resultSet.getLong("product_id"));
                p.setProductName(resultSet.getString("product_name"));
                p.setSku(resultSet.getString("sku"));
                p.setBarcode(resultSet.getString("barcode"));
                p.setProductType(ProductType.fromString(resultSet.getString("product_type")));
                p.setSellingUnit(resultSet.getString("selling_unit"));
                p.setSellingPrice(resultSet.getBigDecimal("selling_price"));
                p.setStatus(resultSet.getString("status"));
                p.setAvailableQuantity(resultSet.getLong("saleable"));
                return p;
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findPosProduct failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /**
     * Live saleable quantity of one product — SUM(on_hand - reserved) over
     * allocatable batches. Used to validate cart quantity edits outside the
     * checkout transaction; checkout re-checks under lock anyway.
     */
    public long getSaleableQuantity(long productId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(SUM(b.on_hand_quantity - b.reserved_quantity),0) ");
        sql.append("AS saleable FROM inventory_batches b ");
        sql.append("WHERE b.product_id = ? AND ").append(InventoryDAO.ALLOCATABLE);
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql.toString());
            statement.setLong(1, productId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong("saleable");
            }
            return 0;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "getSaleableQuantity failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /**
     * staff_profiles.staff_id for a login — sale_transactions.staff_id points
     * at the staff profile, NOT users.user_id. Returns null when the account
     * has no staff profile (a setup error — never faked).
     */
    public Long resolveStaffId(long userId) {
        String sql = "SELECT staff_id FROM staff_profiles WHERE user_id = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, userId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong("staff_id");
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "resolveStaffId failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /* ==================== checkout (the critical transaction) ==================== */
    /**
     * Complete a counter sale in ONE JDBC transaction.
     *
     * Steps, all inside the tx:
     * 1) Resolve staff_profiles.staff_id from the logged-in user.
     * 2) Lock the cart products by product_id ASC (SELECT ... FOR UPDATE) and
     *    re-read status/type/price from the DB.
     * 3) RESTRICTED products refuse — no override rule exists yet.
     * 4) If any RX line: validate the manual-prescription input (non-blank
     *    prescriber + facility, max 200 chars, confirmation checkbox ticked).
     * 5) FEFO-allocate each line over allocatable batches locked FOR UPDATE
     *    (expiry ASC, batch_id ASC); short stock anywhere fails everything.
     * 6) For RX carts, insert ONE `prescriptions` audit row — after allocation
     *    so a stock failure leaves no orphan row.
     * 7) Insert sale_transactions (PENDING) + sale_items + allocations,
     *    decrement on_hand (reserved untouched), write POS_SALE movements,
     *    refresh each touched batch status.
     * 8) Flip the sale to COMPLETED and commit.
     */
    public CheckoutResult completeSale(long userId, List<PosCartItem> cart,
            String paymentMethod, String prescriber, String healthcareFacility,
            boolean prescriptionChecked) {
        if (cart == null || cart.isEmpty()) {
            return CheckoutResult.fail("EMPTY_CART");
        }
        boolean validPayment = false;
        for (String m : PAYMENT_METHODS) {
            if (m.equals(paymentMethod)) {
                validPayment = true;
            }
        }
        if (!validPayment) {
            return CheckoutResult.fail("INVALID_PAYMENT_METHOD");
        }

        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return CheckoutResult.fail("DB_ERROR");
            }
            conn.setAutoCommit(false);

            // 1) staff_id — the sale belongs to a staff profile, not a user id.
            Long staffId = resolveStaffId(conn, userId);
            if (staffId == null) {
                conn.rollback();
                return CheckoutResult.fail("STAFF_PROFILE_MISSING");
            }

            // 2) Lock products in id order — consistent lock order avoids
            //    deadlocks between two tills selling the same items.
            List<Long> ids = new ArrayList<>();
            for (PosCartItem line : cart) {
                if (line.getProductId() != null && line.getQuantity() != null
                        && line.getQuantity() > 0) {
                    ids.add(line.getProductId());
                }
            }
            if (ids.isEmpty()) {
                conn.rollback();
                return CheckoutResult.fail("EMPTY_CART");
            }
            java.util.Collections.sort(ids);
            Map<Long, ProductRow> products = lockProducts(conn, ids);

            BigDecimal total = BigDecimal.ZERO;
            boolean needsPrescription = false;
            for (PosCartItem line : cart) {
                ProductRow p = products.get(line.getProductId());
                if (p == null) {
                    conn.rollback();
                    return CheckoutResult.fail("PRODUCT_NOT_FOUND",
                            String.valueOf(line.getProductId()));
                }
                if (!"ACTIVE".equals(p.status)) {
                    conn.rollback();
                    return CheckoutResult.fail("PRODUCT_INACTIVE", p.productName);
                }
                if ("RESTRICTED".equals(p.productType)) {
                    conn.rollback();
                    return CheckoutResult.fail("RESTRICTED_NOT_ALLOWED", p.productName);
                }
                if ("RX".equals(p.productType)) {
                    needsPrescription = true;
                }
                // Live DB price — the browser copy is ignored entirely.
                line.setUnitPrice(p.sellingPrice);
                total = total.add(p.sellingPrice.multiply(
                        BigDecimal.valueOf(line.getQuantity())));
            }

            // 4) Manual prescription check — required iff the cart has RX
            //    lines. Staff tick a checkbox confirming they inspected a
            //    valid external paper Rx; the doctor + facility names they
            //    typed become the audit record.
            if (needsPrescription) {
                boolean detailsOk = prescriber != null && !prescriber.isEmpty()
                        && prescriber.length() <= 200
                        && healthcareFacility != null && !healthcareFacility.isEmpty()
                        && healthcareFacility.length() <= 200;
                if (!detailsOk) {
                    conn.rollback();
                    return CheckoutResult.fail("PRESCRIPTION_DETAILS_REQUIRED");
                }
                if (!prescriptionChecked) {
                    conn.rollback();
                    return CheckoutResult.fail("PRESCRIPTION_CONFIRMATION_REQUIRED");
                }
            }

            // 5) FEFO allocation per line — batches locked in a fixed order.
            //    planned[batchId][lineIndex] = qty so one batch serving two
            //    lines still gets a single movement and a single UPDATE.
            List<Map<Long, Integer>> allocations = new ArrayList<>();
            Map<Long, BatchRow> touchedBatches = new HashMap<>();
            for (int i = 0; i < cart.size(); i++) {
                allocations.add(new HashMap<>());
            }
            for (int i = 0; i < cart.size(); i++) {
                PosCartItem line = cart.get(i);
                int remaining = line.getQuantity();
                List<BatchRow> batches = lockAllocatableBatches(conn, line.getProductId());
                for (BatchRow b : batches) {
                    if (remaining <= 0) {
                        break;
                    }
                    int free = b.onHandQuantity - b.reservedQuantity;
                    if (free <= 0) {
                        continue;
                    }
                    int take = Math.min(free, remaining);
                    // accumulate across lines sharing a batch
                    Integer planned = allocations.get(i).get(b.batchId);
                    if (planned == null) {
                        planned = 0;
                    }
                    allocations.get(i).put(b.batchId, planned + take);
                    BatchRow shared = touchedBatches.get(b.batchId);
                    if (shared == null) {
                        touchedBatches.put(b.batchId, b);
                        shared = b;
                    }
                    shared.plannedOut = shared.plannedOut + take;
                    remaining = remaining - take;
                }
                if (remaining > 0) {
                    conn.rollback();
                    ProductRow p = products.get(line.getProductId());
                    return CheckoutResult.fail("INSUFFICIENT_STOCK",
                            p.productName + " (short " + remaining + ")");
                }
            }

            // 6) RX carts: write the audit row AFTER allocation succeeds — a
            //    stock failure must not leave an orphan prescription.
            Long prescriptionId = null;
            if (needsPrescription) {
                prescriptionId = insertPrescription(conn, healthcareFacility,
                        prescriber, userId);
                if (prescriptionId == null) {
                    conn.rollback();
                    return CheckoutResult.fail("DB_ERROR");
                }
            }

            // 7) Sale header PENDING → items → allocations → stock → movements.
            long saleId = insertSale(conn, staffId, prescriptionId,
                    paymentMethod, total);
            if (saleId <= 0) {
                conn.rollback();
                return CheckoutResult.fail("DB_ERROR");
            }

            for (int i = 0; i < cart.size(); i++) {
                PosCartItem line = cart.get(i);
                BigDecimal subtotal = line.getUnitPrice().multiply(
                        BigDecimal.valueOf(line.getQuantity()));
                long saleItemId = insertSaleItem(conn, saleId,
                        line.getProductId(), line.getQuantity(),
                        line.getUnitPrice(), subtotal);
                if (saleItemId <= 0) {
                    conn.rollback();
                    return CheckoutResult.fail("DB_ERROR");
                }
                for (Map.Entry<Long, Integer> a : allocations.get(i).entrySet()) {
                    insertAllocation(conn, saleItemId, a.getKey(), a.getValue());
                }
            }

            Date today = new Date(System.currentTimeMillis());
            for (Map.Entry<Long, BatchRow> e : touchedBatches.entrySet()) {
                BatchRow b = e.getValue();
                int after = b.onHandQuantity - b.plannedOut;
                decrementOnHand(conn, b.batchId, b.plannedOut, after, b.expiryDate, today);
                insertPosMovement(conn, b.batchId, userId, -b.plannedOut,
                        b.onHandQuantity, after, b.reservedQuantity, saleId);
            }

            updateSaleStatus(conn, saleId, "COMPLETED");
            conn.commit();
            return CheckoutResult.success(saleId);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "completeSale failed — rolled back", ex);
            rollbackQuietly(conn);
            return CheckoutResult.fail("DB_ERROR");
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    LOG.log(Level.WARNING, "closing tx connection failed", ex);
                }
            }
        }
    }

    /** Prescription by PK — used to show the attached Rx on a sale detail. */
    public Prescription findSalePrescription(long prescriptionId) {
        String sql = "SELECT * FROM prescriptions WHERE prescription_id = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, prescriptionId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return mapPrescription(resultSet);
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findSalePrescription failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /* ==================== sale history / detail ==================== */
    /**
     * Sale list — newest first. Optional filters: payment method + sale date
     * range (inclusive days on sale_datetime).
     */
    public int countSales(String paymentMethod, Date from, Date to) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM sale_transactions st WHERE 1=1 ");
        if (paymentMethod != null && !paymentMethod.isEmpty()) {
            sql.append("AND st.payment_method = ? ");
        }
        if (from != null) {
            sql.append("AND st.sale_datetime >= ? ");
        }
        if (to != null) {
            sql.append("AND st.sale_datetime < DATE_ADD(?, INTERVAL 1 DAY) ");
        }
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = 1;
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                statement.setString(i++, paymentMethod);
            }
            if (from != null) {
                statement.setDate(i++, from);
            }
            if (to != null) {
                statement.setDate(i++, to);
            }
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "countSales failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /** One page of the sale list — staff name joined. */
    public List<SaleTransaction> findSales(String paymentMethod, Date from, Date to,
            int offset, int limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT st.*, u.full_name AS staff_name ");
        sql.append("FROM sale_transactions st ");
        sql.append("JOIN staff_profiles sp ON sp.staff_id = st.staff_id ");
        sql.append("JOIN users u ON u.user_id = sp.user_id ");
        sql.append("WHERE 1=1 ");
        if (paymentMethod != null && !paymentMethod.isEmpty()) {
            sql.append("AND st.payment_method = ? ");
        }
        if (from != null) {
            sql.append("AND st.sale_datetime >= ? ");
        }
        if (to != null) {
            sql.append("AND st.sale_datetime < DATE_ADD(?, INTERVAL 1 DAY) ");
        }
        sql.append("ORDER BY st.sale_transaction_id DESC LIMIT ? OFFSET ?");

        List<SaleTransaction> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = 1;
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                statement.setString(i++, paymentMethod);
            }
            if (from != null) {
                statement.setDate(i++, from);
            }
            if (to != null) {
                statement.setDate(i++, to);
            }
            statement.setInt(i++, limit);
            statement.setInt(i++, offset);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                out.add(mapSale(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findSales failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /** One sale by PK with staff name joined. */
    public SaleTransaction findSaleById(long saleId) {
        String sql = "SELECT st.*, u.full_name AS staff_name "
                + "FROM sale_transactions st "
                + "JOIN staff_profiles sp ON sp.staff_id = st.staff_id "
                + "JOIN users u ON u.user_id = sp.user_id "
                + "WHERE st.sale_transaction_id = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, saleId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return mapSale(resultSet);
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findSaleById failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /** All item rows of one sale with product display fields joined. */
    public List<SaleItem> findSaleItems(long saleId) {
        String sql = "SELECT i.*, p.product_name, p.sku, p.product_type, p.selling_unit "
                + "FROM sale_items i "
                + "JOIN products p ON p.product_id = i.product_id "
                + "WHERE i.sale_transaction_id = ? "
                + "ORDER BY i.sale_item_id ASC";
        List<SaleItem> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, saleId);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                SaleItem item = new SaleItem();
                item.setSaleItemId(resultSet.getLong("sale_item_id"));
                item.setSaleTransactionId(resultSet.getLong("sale_transaction_id"));
                item.setProductId(resultSet.getLong("product_id"));
                item.setQuantity(resultSet.getInt("quantity"));
                item.setUnitPrice(resultSet.getBigDecimal("unit_price"));
                item.setSubtotal(resultSet.getBigDecimal("subtotal"));
                item.setProductName(resultSet.getString("product_name"));
                item.setSku(resultSet.getString("sku"));
                item.setProductType(resultSet.getString("product_type"));
                item.setSellingUnit(resultSet.getString("selling_unit"));
                out.add(item);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findSaleItems failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /**
     * All batch allocations of one sale — traceability view on the receipt:
     * which batch (and expiry) each unit actually came from.
     */
    public List<SaleItemBatchAllocation> findSaleAllocations(long saleId) {
        String sql = "SELECT a.*, b.batch_number, b.expiry_date, p.product_name "
                + "FROM sale_item_batch_allocations a "
                + "JOIN sale_items i ON i.sale_item_id = a.sale_item_id "
                + "JOIN inventory_batches b ON b.batch_id = a.batch_id "
                + "JOIN products p ON p.product_id = i.product_id "
                + "WHERE i.sale_transaction_id = ? "
                + "ORDER BY a.sale_item_id ASC, a.allocation_id ASC";
        List<SaleItemBatchAllocation> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, saleId);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                SaleItemBatchAllocation a = new SaleItemBatchAllocation();
                a.setAllocationId(resultSet.getLong("allocation_id"));
                a.setSaleItemId(resultSet.getLong("sale_item_id"));
                a.setBatchId(resultSet.getLong("batch_id"));
                a.setQuantity(resultSet.getInt("quantity"));
                a.setBatchNumber(resultSet.getString("batch_number"));
                a.setExpiryDate(resultSet.getDate("expiry_date"));
                a.setProductName(resultSet.getString("product_name"));
                out.add(a);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findSaleAllocations failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /* ==================== transaction internals ==================== */
    /** Product snapshot locked inside the checkout tx. */
    private static class ProductRow {
        long productId;
        String productName;
        String productType;
        String status;
        BigDecimal sellingPrice;
    }

    /** Batch snapshot locked inside the checkout tx; plannedOut accumulates. */
    private static class BatchRow {
        long batchId;
        Date expiryDate;
        int onHandQuantity;
        int reservedQuantity;
        int plannedOut;
    }

    /** Lock all cart products at once, ordered by PK — the lock-order rule. */
    private Map<Long, ProductRow> lockProducts(Connection conn, List<Long> ids)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT product_id, product_name, product_type, status, selling_price ");
        sql.append("FROM products WHERE product_id IN (");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append("?");
        }
        sql.append(") ORDER BY product_id ASC FOR UPDATE");
        Map<Long, ProductRow> out = new HashMap<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < ids.size(); i++) {
                ps.setLong(i + 1, ids.get(i));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                ProductRow row = new ProductRow();
                row.productId = rs.getLong("product_id");
                row.productName = rs.getString("product_name");
                row.productType = rs.getString("product_type");
                row.status = rs.getString("status");
                row.sellingPrice = rs.getBigDecimal("selling_price");
                out.put(row.productId, row);
            }
            return out;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /**
     * FEFO batch list for one product — allocatable batches with sellable
     * stock, locked FOR UPDATE in expiry/batch_id order so concurrent tills
     * walk the rows in the same order.
     */
    private List<BatchRow> lockAllocatableBatches(Connection conn, long productId)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT b.batch_id, b.expiry_date, b.on_hand_quantity, ");
        sql.append("b.reserved_quantity FROM inventory_batches b ");
        sql.append("WHERE b.product_id = ? AND ").append(InventoryDAO.ALLOCATABLE);
        sql.append(" AND b.on_hand_quantity - b.reserved_quantity > 0 ");
        sql.append("ORDER BY b.expiry_date ASC, b.batch_id ASC FOR UPDATE");
        List<BatchRow> out = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql.toString());
            ps.setLong(1, productId);
            rs = ps.executeQuery();
            while (rs.next()) {
                BatchRow row = new BatchRow();
                row.batchId = rs.getLong("batch_id");
                row.expiryDate = rs.getDate("expiry_date");
                row.onHandQuantity = rs.getInt("on_hand_quantity");
                row.reservedQuantity = rs.getInt("reserved_quantity");
                row.plannedOut = 0;
                out.add(row);
            }
            return out;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** staff_id lookup on the tx connection — same check, inside the tx. */
    private Long resolveStaffId(Connection conn, long userId) throws SQLException {
        String sql = "SELECT staff_id FROM staff_profiles WHERE user_id = ? LIMIT 1";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("staff_id");
            }
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /**
     * One `prescriptions` audit row — the staff member (users.user_id)
     * confirms they manually checked a valid external paper Rx from this
     * prescriber + facility. validated_at is stamped by the DB
     * (CURRENT_TIMESTAMP) so the audit time is server truth.
     */
    private Long insertPrescription(Connection conn, String healthcareFacility,
            String prescriber, long validatedBy) throws SQLException {
        String sql = "INSERT INTO prescriptions "
                + "(healthcare_facility, prescriber, validated_by, validated_at) "
                + "VALUES (?,?,?,CURRENT_TIMESTAMP)";
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, healthcareFacility);
            ps.setString(2, prescriber);
            ps.setLong(3, validatedBy);
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getLong(1);
            }
            return null;
        } finally {
            closeQuietly(keys);
            closeQuietly(ps);
        }
    }

    /** Sale header, status PENDING — flipped to COMPLETED at the very end. */
    private long insertSale(Connection conn, long staffId, Long prescriptionId,
            String paymentMethod, BigDecimal total) throws SQLException {
        String sql = "INSERT INTO sale_transactions "
                + "(staff_id, prescription_id, payment_method, total_amount, status) "
                + "VALUES (?,?,?,?,'PENDING')";
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, staffId);
            if (prescriptionId == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, prescriptionId);
            }
            ps.setString(3, paymentMethod);
            ps.setBigDecimal(4, total);
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getLong(1);
            }
            return -1;
        } finally {
            closeQuietly(keys);
            closeQuietly(ps);
        }
    }

    private long insertSaleItem(Connection conn, long saleId, long productId,
            int quantity, BigDecimal unitPrice, BigDecimal subtotal)
            throws SQLException {
        String sql = "INSERT INTO sale_items "
                + "(sale_transaction_id, product_id, quantity, unit_price, subtotal) "
                + "VALUES (?,?,?,?,?)";
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, saleId);
            ps.setLong(2, productId);
            ps.setInt(3, quantity);
            ps.setBigDecimal(4, unitPrice);
            ps.setBigDecimal(5, subtotal);
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getLong(1);
            }
            return -1;
        } finally {
            closeQuietly(keys);
            closeQuietly(ps);
        }
    }

    private void insertAllocation(Connection conn, long saleItemId, long batchId,
            int quantity) throws SQLException {
        String sql = "INSERT INTO sale_item_batch_allocations "
                + "(sale_item_id, batch_id, quantity) VALUES (?,?,?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, saleItemId);
            ps.setLong(2, batchId);
            ps.setInt(3, quantity);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /**
     * on_hand -= sold qty; reserved is NEVER touched (POS stock was never
     * reserved). Status cache refreshed: empty → OUT_OF_STOCK, otherwise the
     * expiry-derived status. BLOCKED can't be allocatable so it never gets
     * here.
     */
    private void decrementOnHand(Connection conn, long batchId, int quantity,
            int onHandAfter, Date expiryDate, Date today) throws SQLException {
        String sql = "UPDATE inventory_batches "
                + "SET on_hand_quantity = on_hand_quantity - ?, status = ? "
                + "WHERE batch_id = ?";
        String newStatus;
        if (onHandAfter <= 0) {
            newStatus = "OUT_OF_STOCK";
        } else {
            newStatus = InventoryDAO.statusForExpiry(expiryDate, today);
        }
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, quantity);
            ps.setString(2, newStatus);
            ps.setLong(3, batchId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** One POS_SALE movement per batch — on_hand decreases, reserved unchanged. */
    private void insertPosMovement(Connection conn, long batchId, long performedBy,
            int onHandChange, int onHandBefore, int onHandAfter,
            int reserved, long saleId) throws SQLException {
        String sql = "INSERT INTO inventory_movements "
                + "(batch_id, performed_by, movement_type, on_hand_change, "
                + " reserved_change, on_hand_before, on_hand_after, "
                + " reserved_before, reserved_after, reference_type, reference_id, reason) "
                + "VALUES (?,?,'POS_SALE',?,0,?,?,?,?,'POS_SALE',?,?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            ps.setLong(2, performedBy);
            ps.setInt(3, onHandChange);
            ps.setInt(4, onHandBefore);
            ps.setInt(5, onHandAfter);
            ps.setInt(6, reserved);
            ps.setInt(7, reserved);
            ps.setLong(8, saleId);
            ps.setString(9, "POS sale #" + saleId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    private void updateSaleStatus(Connection conn, long saleId, String status)
            throws SQLException {
        String sql = "UPDATE sale_transactions SET status = ? "
                + "WHERE sale_transaction_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setLong(2, saleId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /* ==================== mapping helpers ==================== */
    private Prescription mapPrescription(ResultSet rs) throws SQLException {
        Prescription rx = new Prescription();
        rx.setPrescriptionId(rs.getLong("prescription_id"));
        rx.setHealthcareFacility(rs.getString("healthcare_facility"));
        rx.setPrescriber(rs.getString("prescriber"));
        long validatedBy = rs.getLong("validated_by");
        if (rs.wasNull()) {
            rx.setValidatedBy(null);
        } else {
            rx.setValidatedBy(validatedBy);
        }
        rx.setValidatedAt(rs.getTimestamp("validated_at"));
        rx.setCreatedAt(rs.getTimestamp("created_at"));
        return rx;
    }

    private SaleTransaction mapSale(ResultSet rs) throws SQLException {
        SaleTransaction s = new SaleTransaction();
        s.setSaleTransactionId(rs.getLong("sale_transaction_id"));
        s.setStaffId(rs.getLong("staff_id"));
        long rxId = rs.getLong("prescription_id");
        if (rs.wasNull()) {
            s.setPrescriptionId(null);
        } else {
            s.setPrescriptionId(rxId);
        }
        s.setPaymentMethod(rs.getString("payment_method"));
        s.setTotalAmount(rs.getBigDecimal("total_amount"));
        s.setStatus(rs.getString("status"));
        s.setSaleDatetime(rs.getTimestamp("sale_datetime"));
        s.setStaffName(rs.getString("staff_name"));
        return s;
    }

    /* ==================== small helpers ==================== */
    private static void rollbackQuietly(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "rollback failed", ex);
        }
    }

    private static void closeQuietly(java.lang.AutoCloseable r) {
        if (r == null) {
            return;
        }
        try {
            r.close();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "close failed", ex);
        }
    }
}
