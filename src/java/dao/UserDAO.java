package dao;

import db.DBContext;
import model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Read/write access to `users` + `customer_profiles`. Extends DBContext per
 * rule.md §20.
 */
public class UserDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(UserDAO.class.getName());

    private static final long ROLE_CUSTOMER = 4;   // roles.role_id for KHACH_HANG per schema seed

    /**
     * Maps one ResultSet row (users + LEFT JOIN roles) to a User entity.
     */
    public User getFromResultSet(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getLong("user_id"));
        u.setRoleId(rs.getLong("role_id"));
        try {
            u.setRoleName(rs.getString("role_name"));
        } catch (SQLException ignored) {
            /* column absent */ }
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setPhone(rs.getString("phone"));
        u.setStatus(rs.getString("status"));
        return u;
    }

    /**
     * Find user by email OR username (identifier is a single field on the login
     * form).
     */
    public User findByIdentifier(String identifier) {
        String sql = "SELECT u.user_id, u.role_id, r.role_name, u.full_name, u.email, "
                + "       u.username, u.password_hash, u.phone, u.status "
                + "FROM users u LEFT JOIN roles r ON r.role_id = u.role_id "
                + "WHERE u.email = ? OR u.username = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, identifier);
            statement.setString(2, identifier);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return getFromResultSet(resultSet);
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findByIdentifier failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    public boolean existsByEmail(String email) {
        return existsBy("email", email);
    }

    public boolean existsByUsername(String username) {
        return existsBy("username", username);
    }

    /**
     * Find user by primary key — used when only the session id is known.
     */
    public User findById(long userId) {
        String sql = "SELECT u.user_id, u.role_id, r.role_name, u.full_name, u.email, "
                + "       u.username, u.password_hash, u.phone, u.status "
                + "FROM users u LEFT JOIN roles r ON r.role_id = u.role_id "
                + "WHERE u.user_id = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, userId);
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
     * Find user by exact email — used by the forgot-password flow.
     */
    public User findByEmail(String email) {
        String sql = "SELECT u.user_id, u.role_id, r.role_name, u.full_name, u.email, "
                + "       u.username, u.password_hash, u.phone, u.status "
                + "FROM users u LEFT JOIN roles r ON r.role_id = u.role_id "
                + "WHERE u.email = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, email);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return getFromResultSet(resultSet);
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findByEmail failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /**
     * Flip NGUNG_HOAT_DONG → HOAT_DONG after the email OTP is confirmed.
     */
    public boolean activateUser(long userId) {
        String sql = "UPDATE users SET status = 'HOAT_DONG' WHERE user_id = ?";
        try {
            connection = getConnection();
            if (connection == null) {
                return false;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "activateUser failed", ex);
            return false;
        } finally {
            closeResources();
        }
    }

    /**
     * Replace password hash after a successful OTP reset.
     */
    public boolean updatePassword(long userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try {
            connection = getConnection();
            if (connection == null) {
                return false;
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, passwordHash);
            statement.setLong(2, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "updatePassword failed", ex);
            return false;
        } finally {
            closeResources();
        }
    }

    private boolean existsBy(String column, String value) {
        String sql = "SELECT 1 FROM users WHERE " + column + " = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return false;
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, value);
            resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "existsBy " + column + " failed", ex);
            return false;
        } finally {
            closeResources();
        }
    }

    /**
     * Register a new KHACH_HANG account: insert user + customer_profile
     * atomically. role_id forced to KHACH_HANG (id=4) — public registration
     * cannot pick role. Account starts NGUNG_HOAT_DONG until the email
     * verification code is confirmed.
     *
     * @return new user_id, or -1 on failure.
     */
    public long registerCustomer(User user) {
        String insertUser = "INSERT INTO users (role_id, full_name, email, username, "
                + "password_hash, phone, status) VALUES (?, ?, ?, ?, ?, ?, 'NGUNG_HOAT_DONG')";
        String insertProfile = "INSERT INTO customer_profiles (user_id) VALUES (?)";
        try {
            connection = getConnection();
            if (connection == null) {
                return -1;
            }
            connection.setAutoCommit(false);
            try {
                statement = connection.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, ROLE_CUSTOMER);
                statement.setString(2, user.getFullName());
                statement.setString(3, user.getEmail());
                statement.setString(4, user.getUsername());
                statement.setString(5, user.getPasswordHash());
                statement.setString(6, user.getPhone());
                int rows = statement.executeUpdate();
                if (rows == 0) {
                    connection.rollback();
                    return -1;
                }
                resultSet = statement.getGeneratedKeys();
                if (!resultSet.next()) {
                    connection.rollback();
                    return -1;
                }
                long newUserId = resultSet.getLong(1);
                resultSet.close();
                statement.close();

                statement = connection.prepareStatement(insertProfile);
                statement.setLong(1, newUserId);
                statement.executeUpdate();

                connection.commit();
                return newUserId;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "registerCustomer failed", ex);
            return -1;
        } finally {
            closeResources();
        }
    }
}
