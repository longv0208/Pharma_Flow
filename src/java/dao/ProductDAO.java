package dao;

import db.DBContext;
import model.Product;
import model.ProductType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Product queries: read for storefront + CRUD for admin. Extends DBContext per
 * rule.md §20.
 */
public class ProductDAO extends DBContext {

    private static final String BASE_COLS
            = "p.product_id, p.category_id, p.product_name, p.sku, p.barcode, "
            + "p.active_ingredient, p.strength, p.dosage_form, p.manufacturer, "
            + "p.registration_number, p.product_type, p.selling_unit, p.selling_price, "
            + "p.online_sale_allowed, p.status, "
            + "p.short_description, p.indication, p.usage_instruction, p.warnings, p.contraindications, "
            + "COALESCE(SUM(b.on_hand_quantity - b.reserved_quantity), 0) AS available_quantity ";

    private static final Logger LOG = Logger.getLogger(ProductDAO.class.getName());

    /**
     * Maps one ResultSet row to a Product entity. Reused by every query.
     */
    public Product getFromResultSet(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getLong("product_id"));
        p.setCategoryId(rs.getLong("category_id"));
        p.setProductName(rs.getString("product_name"));
        p.setSku(rs.getString("sku"));
        p.setBarcode(rs.getString("barcode"));
        p.setActiveIngredient(rs.getString("active_ingredient"));
        p.setStrength(rs.getString("strength"));
        p.setDosageForm(rs.getString("dosage_form"));
        p.setManufacturer(rs.getString("manufacturer"));
        p.setRegistrationNumber(rs.getString("registration_number"));
        p.setShortDescription(rs.getString("short_description"));
        p.setIndication(rs.getString("indication"));
        p.setUsageInstruction(rs.getString("usage_instruction"));
        p.setWarnings(rs.getString("warnings"));
        p.setContraindications(rs.getString("contraindications"));
        p.setProductType(ProductType.fromString(rs.getString("product_type")));
        p.setSellingUnit(rs.getString("selling_unit"));
        p.setSellingPrice(rs.getBigDecimal("selling_price"));
        p.setOnlineSaleAllowed(rs.getBoolean("online_sale_allowed"));
        p.setStatus(rs.getString("status"));
        p.setAvailableQuantity(rs.getLong("available_quantity"));
        try {
            p.setCategoryName(rs.getString("category_name"));
        } catch (SQLException ignored) {
            /* column absent — query didn't join categories */ }
        return p;
    }

    /**
     * Online-saleable + ACTIVE products with computed available quantity.
     * available_quantity = SUM(on_hand - reserved) over batches that are
     * AVAILABLE or NEAR_EXPIRY and not past expiry_date. Single LEFT JOIN
     * aggregate (no N+1).
     *
     * @param limit clamped upstream (1..200)
     * @param categoryId optional — filter to one category
     */
    public List<Product> findOnlineSaleable(int limit, Long categoryId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(BASE_COLS);
        sql.append("FROM products p ");
        sql.append("LEFT JOIN inventory_batches b ");
        sql.append("  ON b.product_id = p.product_id ");
        sql.append(" AND b.status IN ('AVAILABLE','NEAR_EXPIRY') ");
        sql.append(" AND b.expiry_date > CURDATE() ");
        sql.append("WHERE p.status = 'ACTIVE' ");
        sql.append("  AND p.online_sale_allowed = 1 ");
        if (categoryId != null) {
            sql.append("  AND p.category_id = ? ");
        }
        sql.append("GROUP BY p.product_id ");
        sql.append("ORDER BY p.product_id ASC ");
        sql.append("LIMIT ?");

        List<Product> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                LOG.log(Level.SEVERE, "No database connection available");
                return out;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = 1;
            if (categoryId != null) {
                statement.setLong(i++, categoryId);
            }
            statement.setInt(i, limit);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                out.add(getFromResultSet(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findOnlineSaleable failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /* ==================== Storefront catalog ==================== */
    /**
     * Storefront catalog: ACTIVE + online_sale_allowed, optional keyword (name
     * / active_ingredient / manufacturer) + category + type filter. Same
     * available_quantity aggregate as the admin query.
     */
    public List<Product> findCatalog(String keyword, Long categoryId, String productType,
            int limit, int offset) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(BASE_COLS).append(", c.category_name ");
        sql.append("FROM products p ");
        sql.append("LEFT JOIN categories c ON c.category_id = p.category_id ");
        sql.append("LEFT JOIN inventory_batches b ");
        sql.append("  ON b.product_id = p.product_id ");
        sql.append(" AND b.status IN ('AVAILABLE','NEAR_EXPIRY') AND b.expiry_date > CURDATE() ");
        sql.append("WHERE p.status = 'ACTIVE' AND p.online_sale_allowed = 1 ");
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (p.product_name LIKE ? OR p.active_ingredient LIKE ? OR p.manufacturer LIKE ?) ");
        }
        if (categoryId != null) {
            sql.append("AND p.category_id = ? ");
        }
        if (productType != null) {
            sql.append("AND p.product_type = ? ");
        }
        sql.append("GROUP BY p.product_id ORDER BY p.product_name ASC LIMIT ? OFFSET ?");

        List<Product> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = 1;
            if (keyword != null && !keyword.isEmpty()) {
                String like = "%" + keyword + "%";
                statement.setString(i++, like);
                statement.setString(i++, like);
                statement.setString(i++, like);
            }
            if (categoryId != null) {
                statement.setLong(i++, categoryId);
            }
            if (productType != null) {
                statement.setString(i++, productType);
            }
            statement.setInt(i++, limit);
            statement.setInt(i, offset);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                out.add(getFromResultSet(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findCatalog failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /**
     * Row count for catalog paging (same filters as findCatalog).
     */
    public int countCatalog(String keyword, Long categoryId, String productType) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM products p WHERE p.status='ACTIVE' AND p.online_sale_allowed=1 ");
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (p.product_name LIKE ? OR p.active_ingredient LIKE ? OR p.manufacturer LIKE ?) ");
        }
        if (categoryId != null) {
            sql.append("AND p.category_id = ? ");
        }
        if (productType != null) {
            sql.append("AND p.product_type = ? ");
        }
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = 1;
            if (keyword != null && !keyword.isEmpty()) {
                String like = "%" + keyword + "%";
                statement.setString(i++, like);
                statement.setString(i++, like);
                statement.setString(i++, like);
            }
            if (categoryId != null) {
                statement.setLong(i++, categoryId);
            }
            if (productType != null) {
                statement.setString(i++, productType);
            }
            resultSet = statement.executeQuery();
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "countCatalog failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /**
     * Storefront detail: single ACTIVE + online_sale_allowed product with
     * category name + available quantity.
     */
    public Product findStorefrontById(long productId) {
        String sql = "SELECT " + BASE_COLS + ", c.category_name "
                + "FROM products p "
                + "LEFT JOIN categories c ON c.category_id = p.category_id "
                + "LEFT JOIN inventory_batches b "
                + "  ON b.product_id = p.product_id "
                + " AND b.status IN ('AVAILABLE','NEAR_EXPIRY') AND b.expiry_date > CURDATE() "
                + "WHERE p.product_id = ? AND p.status='ACTIVE' AND p.online_sale_allowed=1 "
                + "GROUP BY p.product_id LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, productId);
            resultSet = statement.executeQuery();
            return resultSet.next() ? getFromResultSet(resultSet) : null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findStorefrontById failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /* ==================== Admin ==================== */
    /**
     * Admin list — all statuses, optional keyword (name/sku/barcode) + category
     * + type + status filter, newest first, LIMIT/OFFSET paging.
     */
    public List<Product> findAll(String keyword, Long categoryId, String productType,
            String status, int limit, int offset) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(BASE_COLS);
        sql.append(", c.category_name ");
        sql.append("FROM products p ");
        sql.append("LEFT JOIN categories c ON c.category_id = p.category_id ");
        sql.append("LEFT JOIN inventory_batches b ");
        sql.append("  ON b.product_id = p.product_id ");
        sql.append(" AND b.status IN ('AVAILABLE','NEAR_EXPIRY') ");
        sql.append(" AND b.expiry_date > CURDATE() ");
        sql.append("WHERE 1=1 ");
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (p.product_name LIKE ? OR p.sku LIKE ? OR p.barcode LIKE ?) ");
        }
        if (categoryId != null) {
            sql.append("AND p.category_id = ? ");
        }
        if (productType != null) {
            sql.append("AND p.product_type = ? ");
        }
        if (status != null) {
            sql.append("AND p.status = ? ");
        }
        sql.append("GROUP BY p.product_id ORDER BY p.product_id DESC LIMIT ? OFFSET ?");

        List<Product> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = 1;
            if (keyword != null && !keyword.isEmpty()) {
                String like = "%" + keyword + "%";
                statement.setString(i++, like);
                statement.setString(i++, like);
                statement.setString(i++, like);
            }
            if (categoryId != null) {
                statement.setLong(i++, categoryId);
            }
            if (productType != null) {
                statement.setString(i++, productType);
            }
            if (status != null) {
                statement.setString(i++, status);
            }
            statement.setInt(i++, limit);
            statement.setInt(i, offset);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                out.add(getFromResultSet(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findAll failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /**
     * Row count for paging (same filters as findAll).
     */
    public int countAll(String keyword, Long categoryId, String productType, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM products p WHERE 1=1 ");
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (p.product_name LIKE ? OR p.sku LIKE ? OR p.barcode LIKE ?) ");
        }
        if (categoryId != null) {
            sql.append("AND p.category_id = ? ");
        }
        if (productType != null) {
            sql.append("AND p.product_type = ? ");
        }
        if (status != null) {
            sql.append("AND p.status = ? ");
        }
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = 1;
            if (keyword != null && !keyword.isEmpty()) {
                String like = "%" + keyword + "%";
                statement.setString(i++, like);
                statement.setString(i++, like);
                statement.setString(i++, like);
            }
            if (categoryId != null) {
                statement.setLong(i++, categoryId);
            }
            if (productType != null) {
                statement.setString(i++, productType);
            }
            if (status != null) {
                statement.setString(i++, status);
            }
            resultSet = statement.executeQuery();
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "countAll failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /**
     * Single product by PK — includes available_quantity aggregate for stock
     * info.
     */
    public Product findById(long productId) {
        String sql = "SELECT " + BASE_COLS
                + "FROM products p LEFT JOIN inventory_batches b "
                + "  ON b.product_id = p.product_id "
                + " AND b.status IN ('AVAILABLE','NEAR_EXPIRY') AND b.expiry_date > CURDATE() "
                + "WHERE p.product_id = ? GROUP BY p.product_id LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, productId);
            resultSet = statement.executeQuery();
            return resultSet.next() ? getFromResultSet(resultSet) : null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findById failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /**
     * Unique checks — sku / barcode (exclude self when editing).
     */
    public boolean existsBySku(String sku, long excludeId) {
        return existsWhere("SELECT 1 FROM products WHERE sku = ? AND product_id <> ? LIMIT 1", sku, excludeId);
    }

    public boolean existsByBarcode(String barcode, long excludeId) {
        if (barcode == null || barcode.isEmpty()) {
            return false;   // NULL barcode allowed
        }
        return existsWhere("SELECT 1 FROM products WHERE barcode = ? AND product_id <> ? LIMIT 1", barcode, excludeId);
    }

    private boolean existsWhere(String sql, String value, long excludeId) {
        try {
            connection = getConnection();
            if (connection == null) {
                return false;
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, value);
            statement.setLong(2, excludeId);
            resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "existsWhere failed", ex);
            return false;
        } finally {
            closeResources();
        }
    }

    /**
     * INSERT new product. Returns generated id or -1.
     */
    public long create(Product p) {
        String sql = "INSERT INTO products (category_id, product_name, sku, barcode, "
                + "active_ingredient, strength, dosage_form, manufacturer, registration_number, "
                + "short_description, indication, usage_instruction, warnings, contraindications, "
                + "product_type, selling_unit, selling_price, online_sale_allowed, status) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            connection = getConnection();
            if (connection == null) {
                return -1;
            }
            statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            bindProduct(statement, p);
            statement.executeUpdate();
            resultSet = statement.getGeneratedKeys();
            return resultSet.next() ? resultSet.getLong(1) : -1;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "create failed", ex);
            return -1;
        } finally {
            closeResources();
        }
    }

    public int update(Product p) {
        String sql = "UPDATE products SET category_id=?, product_name=?, sku=?, barcode=?, "
                + "active_ingredient=?, strength=?, dosage_form=?, manufacturer=?, registration_number=?, "
                + "short_description=?, indication=?, usage_instruction=?, warnings=?, contraindications=?, "
                + "product_type=?, selling_unit=?, selling_price=?, online_sale_allowed=?, status=? "
                + "WHERE product_id=?";
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql);
            int i = bindProduct(statement, p);
            statement.setLong(i, p.getProductId());
            return statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "update failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /**
     * Soft delete → INACTIVE (kept for order/batch history).
     */
    public int deactivate(long productId) {
        String sql = "UPDATE products SET status='INACTIVE' WHERE product_id=?";
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, productId);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "deactivate failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /**
     * Restore → ACTIVE.
     */
    public int activate(long productId) {
        String sql = "UPDATE products SET status='ACTIVE' WHERE product_id=?";
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, productId);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "activate failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /**
     * Shared bind for INSERT/UPDATE — 14 product columns, returns next index.
     */
    private int bindProduct(java.sql.PreparedStatement st, Product p) throws SQLException {
        st.setLong(1, p.getCategoryId());
        st.setString(2, p.getProductName());
        st.setString(3, p.getSku());
        st.setString(4, p.getBarcode());
        st.setString(5, p.getActiveIngredient());
        st.setString(6, p.getStrength());
        st.setString(7, p.getDosageForm());
        st.setString(8, p.getManufacturer());
        st.setString(9, p.getRegistrationNumber());
        st.setString(10, p.getShortDescription());
        st.setString(11, p.getIndication());
        st.setString(12, p.getUsageInstruction());
        st.setString(13, p.getWarnings());
        st.setString(14, p.getContraindications());
        st.setString(15, p.getProductType() == null ? "OTC" : p.getProductType().name());
        st.setString(16, p.getSellingUnit());
        st.setBigDecimal(17, p.getSellingPrice());
        st.setBoolean(18, Boolean.TRUE.equals(p.getOnlineSaleAllowed()));
        st.setString(19, p.getStatus() == null ? "ACTIVE" : p.getStatus());
        return 20;
    }
}
