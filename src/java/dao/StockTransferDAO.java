package dao;

import db.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal stock transfer between bins — one JDBC transaction per transfer.
 */
public class StockTransferDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(StockTransferDAO.class.getName());

    public static class TransferResult {
        public final boolean ok;
        public final String error;

        private TransferResult(boolean ok, String error) {
            this.ok = ok;
            this.error = error;
        }

        public static TransferResult success() {
            return new TransferResult(true, null);
        }

        public static TransferResult fail(String error) {
            return new TransferResult(false, error);
        }
    }

    public TransferResult transfer(long batchId, long fromBinId, long toBinId, int qty,
            long performedBy, String reason) {
        if (batchId <= 0 || fromBinId <= 0 || toBinId <= 0 || qty <= 0) {
            return TransferResult.fail("badinput");
        }
        if (fromBinId == toBinId) {
            return TransferResult.fail("samebin");
        }
        String cleanReason = reason == null ? "" : reason.trim();
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return TransferResult.fail("db");
            }
            conn.setAutoCommit(false);
            LocationDAO locationDao = new LocationDAO();
            if (locationDao.findActiveBin(conn, fromBinId) == null
                    || locationDao.findActiveBin(conn, toBinId) == null) {
                conn.rollback();
                return TransferResult.fail("bininactive");
            }
            lockBatch(conn, batchId);
            int batchOnHandBefore = locationDao.getBatchOnHand(conn, batchId);
            locationDao.transferBetweenBins(conn, batchId, fromBinId, toBinId, qty);
            int batchOnHandAfter = locationDao.getBatchOnHand(conn, batchId);
            String movementReason = "Chuyển " + qty + " đv";
            if (!cleanReason.isEmpty()) {
                movementReason = movementReason + " — " + cleanReason;
            }
            locationDao.insertMovementWithBins(conn, batchId, fromBinId, toBinId,
                    performedBy, "CHUYEN_VI_TRI", 0, 0,
                    batchOnHandBefore, batchOnHandAfter, 0, 0,
                    "CHUYEN_VI_TRI", null, movementReason);
            conn.commit();
            return TransferResult.success();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "transfer failed — rolled back", ex);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                    // ignore
                }
            }
            if (ex.getMessage() != null && ex.getMessage().contains("insufficient")) {
                return TransferResult.fail("insufficient");
            }
            return TransferResult.fail("db");
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

    private void lockBatch(Connection conn, long batchId) throws SQLException {
        String sql = "SELECT batch_id FROM inventory_batches WHERE batch_id = ? FOR UPDATE";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new SQLException("batch not found");
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
        }
    }
}
