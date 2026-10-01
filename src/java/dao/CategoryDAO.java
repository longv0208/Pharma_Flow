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
 * Read-only access to `categories` table.
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
}
