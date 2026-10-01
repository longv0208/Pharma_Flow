package dao;

import db.DBContext;
import model.Category;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access to `categories` table (read for storefront + CRUD for admin).
 * Extends DBContext per rule.md §20 — uses inherited connection/statement/
 * resultSet fields and closeResources() in finally.
 */
public class CategoryDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(CategoryDAO.class.getName());

    /** Maps one ResultSet row to a Category entity. Reused by every query. */
    public Category getFromResultSet(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setCategoryId(rs.getLong("category_id"));
        c.setCategoryName(rs.getString("category_name"));
        c.setDescription(rs.getString("description"));
        c.setStatus(rs.getString("status"));
        return c;
    }

    /** All ACTIVE categories, alphabetical. */
    public List<Category> findAllActive() {
        String sql = "SELECT category_id, category_name, description, status "
                   + "FROM categories WHERE status = 'ACTIVE' ORDER BY category_name";
        List<Category> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                LOG.log(Level.SEVERE, "No database connection available");
                return out;
            }
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                out.add(getFromResultSet(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findAllActive failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /* ==================== Admin CRUD ==================== */

    /** All categories (ACTIVE + INACTIVE), newest first — admin list view. */
    public List<Category> findAll() {
        String sql = "SELECT category_id, category_name, description, status "
                   + "FROM categories ORDER BY category_id DESC";
        List<Category> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) return out;
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

    /** Single category by PK — for edit form prefill. */
    public Category findById(long categoryId) {
        String sql = "SELECT category_id, category_name, description, status "
                   + "FROM categories WHERE category_id = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) return null;
            statement = connection.prepareStatement(sql);
            statement.setLong(1, categoryId);
            resultSet = statement.executeQuery();
            return resultSet.next() ? getFromResultSet(resultSet) : null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findById failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /** True when another row already uses this name (exclude self when editing). */
    public boolean existsByName(String name, long excludeId) {
        String sql = "SELECT 1 FROM categories WHERE category_name = ? AND category_id <> ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) return false;
            statement = connection.prepareStatement(sql);
            statement.setString(1, name);
            statement.setLong(2, excludeId);
            resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "existsByName failed", ex);
            return false;
        } finally {
            closeResources();
        }
    }

    /** INSERT new ACTIVE category. Returns generated id or -1 on failure. */
    public long create(String name, String description) {
        String sql = "INSERT INTO categories (category_name, description, status) VALUES (?,?,'ACTIVE')";
        try {
            connection = getConnection();
            if (connection == null) return -1;
            statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, name);
            statement.setString(2, description);
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

    /** UPDATE name + description + status. Returns affected rows. */
    public int update(long categoryId, String name, String description, String status) {
        String sql = "UPDATE categories SET category_name=?, description=?, status=? WHERE category_id=?";
        try {
            connection = getConnection();
            if (connection == null) return 0;
            statement = connection.prepareStatement(sql);
            statement.setString(1, name);
            statement.setString(2, description);
            statement.setString(3, status);
            statement.setLong(4, categoryId);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "update failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /**
     * Soft delete: flip status to INACTIVE. Categories cannot be hard-deleted
     * because products.category_id has a plain FK (no cascade) — rule: keep history.
     */
    public int deactivate(long categoryId) {
        String sql = "UPDATE categories SET status='INACTIVE' WHERE category_id=?";
        try {
            connection = getConnection();
            if (connection == null) return 0;
            statement = connection.prepareStatement(sql);
            statement.setLong(1, categoryId);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "deactivate failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /** Restore → ACTIVE. */
    public int activate(long categoryId) {
        String sql = "UPDATE categories SET status='ACTIVE' WHERE category_id=?";
        try {
            connection = getConnection();
            if (connection == null) return 0;
            statement = connection.prepareStatement(sql);
            statement.setLong(1, categoryId);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "activate failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }
}
