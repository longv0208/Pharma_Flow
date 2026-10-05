package dao;

import db.DBContext;
import model.Supplier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access to `suppliers` — admin CRUD only (no storefront usage). Soft delete
 * via status='INACTIVE': supplier_id is FK-target of purchase_orders,
 * goods_receipts, inventory_batches, supplier_products.
 */
public class SupplierDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(SupplierDAO.class.getName());

    public Supplier getFromResultSet(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setSupplierId(rs.getLong("supplier_id"));
        s.setSupplierName(rs.getString("supplier_name"));
        s.setContactPerson(rs.getString("contact_person"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setAddress(rs.getString("address"));
        s.setTaxBusinessInfo(rs.getString("tax_business_info"));
        s.setStatus(rs.getString("status"));
        return s;
    }

    /**
     * All suppliers (ACTIVE + INACTIVE), newest first.
     */
    public List<Supplier> findAll() {
        String sql = "SELECT supplier_id, supplier_name, contact_person, phone, email, "
                + "address, tax_business_info, status FROM suppliers ORDER BY supplier_id DESC";
        List<Supplier> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql);
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

    public Supplier findById(long supplierId) {
        String sql = "SELECT supplier_id, supplier_name, contact_person, phone, email, "
                + "address, tax_business_info, status FROM suppliers WHERE supplier_id = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, supplierId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return getFromResultSet(resultSet);
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findById failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /**
     * INSERT new ACTIVE supplier. Returns generated id or -1.
     */
    public long create(Supplier s) {
        String sql = "INSERT INTO suppliers (supplier_name, contact_person, phone, email, "
                + "address, tax_business_info, status) VALUES (?,?,?,?,?,?,'ACTIVE')";
        try {
            connection = getConnection();
            if (connection == null) {
                return -1;
            }
            statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, s.getSupplierName());
            statement.setString(2, s.getContactPerson());
            statement.setString(3, s.getPhone());
            statement.setString(4, s.getEmail());
            statement.setString(5, s.getAddress());
            statement.setString(6, s.getTaxBusinessInfo());
            statement.executeUpdate();
            resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            return -1;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "create failed", ex);
            return -1;
        } finally {
            closeResources();
        }
    }

    public int update(Supplier s) {
        String sql = "UPDATE suppliers SET supplier_name=?, contact_person=?, phone=?, email=?, "
                + "address=?, tax_business_info=?, status=? WHERE supplier_id=?";
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, s.getSupplierName());
            statement.setString(2, s.getContactPerson());
            statement.setString(3, s.getPhone());
            statement.setString(4, s.getEmail());
            statement.setString(5, s.getAddress());
            statement.setString(6, s.getTaxBusinessInfo());
            statement.setString(7, s.getStatus());
            statement.setLong(8, s.getSupplierId());
            return statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "update failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /**
     * Soft delete → INACTIVE (FK references must be preserved).
     */
    public int deactivate(long supplierId) {
        String sql = "UPDATE suppliers SET status='INACTIVE' WHERE supplier_id=?";
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, supplierId);
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
    public int activate(long supplierId) {
        String sql = "UPDATE suppliers SET status='ACTIVE' WHERE supplier_id=?";
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, supplierId);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "activate failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }
}
