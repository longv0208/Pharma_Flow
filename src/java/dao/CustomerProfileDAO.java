package dao;

import db.DBContext;
import model.CustomerProfile;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access to `customer_profiles`. Ownership enforced via user_id from session,
 * never from request params (rule: no IDOR).
 */
public class CustomerProfileDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(CustomerProfileDAO.class.getName());

    public CustomerProfile getFromResultSet(ResultSet rs) throws SQLException {
        CustomerProfile p = new CustomerProfile();
        p.setCustomerId(rs.getLong("customer_id"));
        p.setUserId(rs.getLong("user_id"));
        p.setProvinceCity(rs.getString("province_city"));
        p.setDistrict(rs.getString("district"));
        p.setWard(rs.getString("ward"));
        p.setDetailedAddress(rs.getString("detailed_address"));
        return p;
    }

    /**
     * Find profile by users.user_id — NOT by customer_id (avoids IDOR).
     */
    public CustomerProfile findByUserId(long userId) {
        String sql = "SELECT customer_id, user_id, province_city, district, ward, detailed_address "
                + "FROM customer_profiles WHERE user_id = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, userId);
            resultSet = statement.executeQuery();
            return resultSet.next() ? getFromResultSet(resultSet) : null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findByUserId failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /**
     * Upsert delivery address by user_id. Safe even if the customer_profiles
     * row was never created (e.g. legacy user without profile row).
     */
    public int updateAddressByUserId(long userId, String provinceCity, String district,
            String ward, String detailedAddress) {
        String sql = "INSERT INTO customer_profiles (user_id, province_city, district, ward, detailed_address) "
                + "VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                + "province_city=VALUES(province_city), district=VALUES(district), "
                + "ward=VALUES(ward), detailed_address=VALUES(detailed_address)";
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, userId);
            statement.setString(2, provinceCity);
            statement.setString(3, district);
            statement.setString(4, ward);
            statement.setString(5, detailedAddress);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "updateAddressByUserId failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /**
     * Update name + phone on users table (customer-editable subset only).
     */
    public int updateUserContactByUserId(long userId, String fullName, String phone) {
        String sql = "UPDATE users SET full_name=?, phone=? WHERE user_id=?";
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, fullName);
            statement.setString(2, phone);
            statement.setLong(3, userId);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "updateUserContactByUserId failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }
}
