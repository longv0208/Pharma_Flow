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
 * Read-only product queries for the customer-facing catalog.
 * Extends DBContext per rule.md §20.
 */
public class ProductDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(ProductDAO.class.getName());

    /** Maps one ResultSet row to a Product entity. Reused by every query. */
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
        p.setProductType(ProductType.fromString(rs.getString("product_type")));
        p.setSellingUnit(rs.getString("selling_unit"));
        p.setSellingPrice(rs.getBigDecimal("selling_price"));
        p.setOnlineSaleAllowed(rs.getBoolean("online_sale_allowed"));
        p.setStatus(rs.getString("status"));
        p.setAvailableQuantity(rs.getLong("available_quantity"));
        return p;
    }

    /**
     * Online-saleable + ACTIVE products with computed available quantity.
     * available_quantity = SUM(on_hand - reserved) over batches that are
     * AVAILABLE or NEAR_EXPIRY and not past expiry_date. Single LEFT JOIN
     * aggregate (no N+1).
     *
     * @param limit      clamped upstream (1..200)
     * @param categoryId optional — filter to one category
     */
    public List<Product> findOnlineSaleable(int limit, Long categoryId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.product_id, p.category_id, p.product_name, p.sku, p.barcode, ");
        sql.append("       p.active_ingredient, p.strength, p.dosage_form, p.manufacturer, ");
        sql.append("       p.registration_number, p.product_type, p.selling_unit, p.selling_price, ");
        sql.append("       p.online_sale_allowed, p.status, ");
        sql.append("       COALESCE(SUM(b.on_hand_quantity - b.reserved_quantity), 0) AS available_quantity ");
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
            if (categoryId != null) statement.setLong(i++, categoryId);
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
}
