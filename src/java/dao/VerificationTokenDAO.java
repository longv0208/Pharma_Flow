package dao;

import db.DBContext;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access to `verification_tokens` — one-time OTP codes for email verification
 * (XAC_THUC_EMAIL) and password reset (DAT_LAI_MAT_KHAU). Codes are stored
 * SHA-256-hashed; a row is single-use via used_at.
 */
public class VerificationTokenDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(VerificationTokenDAO.class.getName());

    /**
     * Minutes a code stays valid — matches the "15 minutes" copy in the
     * email/JSPs.
     */
    private static final int TTL_MINUTES = 15;

    /**
     * Minimum seconds between two resends — enforced server-side.
     */
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    /**
     * Seconds since the latest token of this type was issued for the user.
     * Returns -1 when the user has no token of this type yet.
     */
    public long secondsSinceLastIssue(long userId, String type) {
        String sql = "SELECT TIMESTAMPDIFF(SECOND, MAX(created_at), NOW()) "
                + "FROM verification_tokens WHERE user_id = ? AND token_type = ?";
        try {
            connection = getConnection();
            if (connection == null) {
                return -1;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, userId);
            statement.setString(2, type);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                return -1;
            }
            long seconds = resultSet.getLong(1);
            // MAX(created_at) is NULL when the user has no token of this type.
            if (resultSet.wasNull()) {
                return -1;
            }
            return seconds;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "secondsSinceLastIssue failed", ex);
            return -1;
        } finally {
            closeResources();
        }
    }

    /**
     * Seconds remaining before another resend is allowed (0 = allowed now).
     */
    public long resendCooldownLeft(long userId, String type) {
        long since = secondsSinceLastIssue(userId, type);
        if (since < 0) {
            return 0;
        }
        long left = RESEND_COOLDOWN_SECONDS - since;
        return Math.max(0, left);
    }

    /**
     * Invalidate all outstanding codes of this type for the user (resend flow).
     */
    public void invalidatePrevious(long userId, String type) {
        String sql = "UPDATE verification_tokens SET used_at = NOW() "
                + "WHERE user_id = ? AND token_type = ? AND used_at IS NULL";
        try {
            connection = getConnection();
            if (connection == null) {
                return;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, userId);
            statement.setString(2, type);
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "invalidatePrevious failed", ex);
        } finally {
            closeResources();
        }
    }

    /**
     * Persist a fresh code (already hashed) with a 15-minute expiry. expires_at
     * uses DB NOW()+INTERVAL so it lives in the same clock as the NOW() checks
     * in consume/existsLive — no JVM↔DB timezone skew.
     */
    public boolean insert(long userId, String type, String tokenHash) {
        String sql = "INSERT INTO verification_tokens (user_id, token_hash, token_type, expires_at) "
                + "VALUES (?, ?, ?, NOW() + INTERVAL " + TTL_MINUTES + " MINUTE)";
        try {
            connection = getConnection();
            if (connection == null) {
                return false;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, userId);
            statement.setString(2, tokenHash);
            statement.setString(3, type);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "insert failed", ex);
            return false;
        } finally {
            closeResources();
        }
    }

    /**
     * Consume a code: marks the matching live row as used and returns true.
     * Returns false when the code is wrong, expired, or already used — the
     * caller never learns which (prevents oracle probing).
     */
    public boolean consume(long userId, String type, String tokenHash) {
        String sql = "UPDATE verification_tokens SET used_at = NOW() "
                + "WHERE user_id = ? AND token_type = ? AND token_hash = ? "
                + "AND used_at IS NULL AND expires_at > NOW()";
        try {
            connection = getConnection();
            if (connection == null) {
                return false;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, userId);
            statement.setString(2, type);
            statement.setString(3, tokenHash);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "consume failed", ex);
            return false;
        } finally {
            closeResources();
        }
    }

    /**
     * Check a code WITHOUT consuming it — used on the reset flow where the same
     * code must survive from the "enter OTP" step to the "new password" submit.
     * Call {@link #consume} after the password is actually updated.
     */
    public boolean existsLive(long userId, String type, String tokenHash) {
        String sql = "SELECT 1 FROM verification_tokens "
                + "WHERE user_id = ? AND token_type = ? AND token_hash = ? "
                + "AND used_at IS NULL AND expires_at > NOW() LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return false;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, userId);
            statement.setString(2, type);
            statement.setString(3, tokenHash);
            resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "existsLive failed", ex);
            return false;
        } finally {
            closeResources();
        }
    }
}
