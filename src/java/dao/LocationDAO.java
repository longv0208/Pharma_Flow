package dao;

import db.DBContext;
import model.InventoryBatchLocation;
import model.StorageBin;
import model.StorageRack;
import model.StorageZone;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Zone / Rack / Bin CRUD and dual-write qty helpers for
 * inventory_batch_locations + inventory_batches totals.
 */
public class LocationDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(LocationDAO.class.getName());

    public static class MutateResult {
        public final boolean ok;
        public final String error;

        private MutateResult(boolean ok, String error) {
            this.ok = ok;
            this.error = error;
        }

        public static MutateResult success() {
            return new MutateResult(true, null);
        }

        public static MutateResult fail(String error) {
            return new MutateResult(false, error);
        }
    }

    /** One FEFO pick candidate: batch at a specific bin with saleable qty. */
    public static class BinStockRow {
        public long batchId;
        public long binId;
        public int onHand;
        public int reserved;
        public java.sql.Date expiryDate;
        public String batchNumber;
        public String status;

        public int saleable() {
            return onHand - reserved;
        }
    }

    /* ==================== Zones ==================== */

    public List<StorageZone> findZones(String q, String status) {
        List<StorageZone> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT z.*, ");
        sql.append("(SELECT COUNT(*) FROM storage_racks r WHERE r.zone_id = z.zone_id) AS rack_count, ");
        sql.append("COALESCE((SELECT SUM(l.on_hand_quantity) FROM inventory_batch_locations l ");
        sql.append(" JOIN storage_bins bn ON bn.bin_id = l.bin_id ");
        sql.append(" JOIN storage_racks r ON r.rack_id = bn.rack_id ");
        sql.append(" WHERE r.zone_id = z.zone_id),0) AS on_hand_total ");
        sql.append("FROM storage_zones z WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (q != null && !q.trim().isEmpty()) {
            sql.append("AND (z.zone_code LIKE ? OR z.zone_name LIKE ?) ");
            String like = "%" + q.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (status != null && !status.isEmpty()) {
            sql.append("AND z.status = ? ");
            params.add(status);
        }
        sql.append("ORDER BY z.zone_code ASC");
        try {
            connection = getConnection();
            if (connection == null) {
                return list;
            }
            statement = connection.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                list.add(mapZone(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findZones failed", ex);
        } finally {
            closeResources();
        }
        return list;
    }

    public StorageZone findZoneById(long zoneId) {
        String sql = "SELECT z.*, "
                + "(SELECT COUNT(*) FROM storage_racks r WHERE r.zone_id = z.zone_id) AS rack_count, "
                + "0 AS on_hand_total FROM storage_zones z WHERE z.zone_id = ?";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, zoneId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return mapZone(resultSet);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findZoneById failed", ex);
        } finally {
            closeResources();
        }
        return null;
    }

    public MutateResult createZone(String code, String name) {
        if (isBlank(code) || isBlank(name)) {
            return MutateResult.fail("required");
        }
        String sql = "INSERT INTO storage_zones (zone_code, zone_name, status) VALUES (?,?, 'HOAT_DONG')";
        try {
            connection = getConnection();
            if (connection == null) {
                return MutateResult.fail("db");
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, code.trim().toUpperCase());
            statement.setString(2, name.trim());
            statement.executeUpdate();
            return MutateResult.success();
        } catch (SQLException ex) {
            if (isDuplicate(ex)) {
                return MutateResult.fail("duplicate");
            }
            LOG.log(Level.SEVERE, "createZone failed", ex);
            return MutateResult.fail("db");
        } finally {
            closeResources();
        }
    }

    public MutateResult updateZone(long zoneId, String code, String name) {
        if (isBlank(code) || isBlank(name)) {
            return MutateResult.fail("required");
        }
        String sql = "UPDATE storage_zones SET zone_code = ?, zone_name = ? WHERE zone_id = ?";
        try {
            connection = getConnection();
            if (connection == null) {
                return MutateResult.fail("db");
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, code.trim().toUpperCase());
            statement.setString(2, name.trim());
            statement.setLong(3, zoneId);
            int n = statement.executeUpdate();
            return n > 0 ? MutateResult.success() : MutateResult.fail("notfound");
        } catch (SQLException ex) {
            if (isDuplicate(ex)) {
                return MutateResult.fail("duplicate");
            }
            LOG.log(Level.SEVERE, "updateZone failed", ex);
            return MutateResult.fail("db");
        } finally {
            closeResources();
        }
    }

    public MutateResult setZoneStatus(long zoneId, String status) {
        if (!"HOAT_DONG".equals(status) && !"NGUNG_HOAT_DONG".equals(status)) {
            return MutateResult.fail("badstatus");
        }
        if ("NGUNG_HOAT_DONG".equals(status) && zoneHasStock(zoneId)) {
            return MutateResult.fail("hasstock");
        }
        return updateStatus("UPDATE storage_zones SET status = ? WHERE zone_id = ?", status, zoneId);
    }

    /* ==================== Racks ==================== */

    public List<StorageRack> findRacks(Long zoneId, String q, String status) {
        List<StorageRack> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT r.*, z.zone_code, z.zone_name, ");
        sql.append("(SELECT COUNT(*) FROM storage_bins b WHERE b.rack_id = r.rack_id) AS bin_count, ");
        sql.append("COALESCE((SELECT SUM(l.on_hand_quantity) FROM inventory_batch_locations l ");
        sql.append(" JOIN storage_bins b ON b.bin_id = l.bin_id WHERE b.rack_id = r.rack_id),0) AS on_hand_total ");
        sql.append("FROM storage_racks r JOIN storage_zones z ON z.zone_id = r.zone_id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (zoneId != null && zoneId > 0) {
            sql.append("AND r.zone_id = ? ");
            params.add(zoneId);
        }
        if (q != null && !q.trim().isEmpty()) {
            sql.append("AND (r.rack_code LIKE ? OR r.rack_name LIKE ? OR z.zone_code LIKE ?) ");
            String like = "%" + q.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (status != null && !status.isEmpty()) {
            sql.append("AND r.status = ? ");
            params.add(status);
        }
        sql.append("ORDER BY z.zone_code, r.rack_code");
        try {
            connection = getConnection();
            if (connection == null) {
                return list;
            }
            statement = connection.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                list.add(mapRack(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findRacks failed", ex);
        } finally {
            closeResources();
        }
        return list;
    }

    public StorageRack findRackById(long rackId) {
        String sql = "SELECT r.*, z.zone_code, z.zone_name, 0 AS bin_count, 0 AS on_hand_total "
                + "FROM storage_racks r JOIN storage_zones z ON z.zone_id = r.zone_id "
                + "WHERE r.rack_id = ?";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, rackId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return mapRack(resultSet);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findRackById failed", ex);
        } finally {
            closeResources();
        }
        return null;
    }

    public MutateResult createRack(long zoneId, String code, String name) {
        if (zoneId <= 0 || isBlank(code) || isBlank(name)) {
            return MutateResult.fail("required");
        }
        String sql = "INSERT INTO storage_racks (zone_id, rack_code, rack_name, status) "
                + "VALUES (?,?,?, 'HOAT_DONG')";
        try {
            connection = getConnection();
            if (connection == null) {
                return MutateResult.fail("db");
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, zoneId);
            statement.setString(2, code.trim().toUpperCase());
            statement.setString(3, name.trim());
            statement.executeUpdate();
            return MutateResult.success();
        } catch (SQLException ex) {
            if (isDuplicate(ex)) {
                return MutateResult.fail("duplicate");
            }
            LOG.log(Level.SEVERE, "createRack failed", ex);
            return MutateResult.fail("db");
        } finally {
            closeResources();
        }
    }

    public MutateResult updateRack(long rackId, String code, String name) {
        if (isBlank(code) || isBlank(name)) {
            return MutateResult.fail("required");
        }
        String sql = "UPDATE storage_racks SET rack_code = ?, rack_name = ? WHERE rack_id = ?";
        try {
            connection = getConnection();
            if (connection == null) {
                return MutateResult.fail("db");
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, code.trim().toUpperCase());
            statement.setString(2, name.trim());
            statement.setLong(3, rackId);
            int n = statement.executeUpdate();
            return n > 0 ? MutateResult.success() : MutateResult.fail("notfound");
        } catch (SQLException ex) {
            if (isDuplicate(ex)) {
                return MutateResult.fail("duplicate");
            }
            LOG.log(Level.SEVERE, "updateRack failed", ex);
            return MutateResult.fail("db");
        } finally {
            closeResources();
        }
    }

    public MutateResult setRackStatus(long rackId, String status) {
        if (!"HOAT_DONG".equals(status) && !"NGUNG_HOAT_DONG".equals(status)) {
            return MutateResult.fail("badstatus");
        }
        if ("NGUNG_HOAT_DONG".equals(status) && rackHasStock(rackId)) {
            return MutateResult.fail("hasstock");
        }
        return updateStatus("UPDATE storage_racks SET status = ? WHERE rack_id = ?", status, rackId);
    }

    /* ==================== Bins ==================== */

    public List<StorageBin> findBins(Long rackId, Long zoneId, String q, String status) {
        List<StorageBin> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT bn.*, r.rack_code, r.rack_name, r.zone_id, z.zone_code, z.zone_name, ");
        sql.append("COALESCE((SELECT SUM(l.on_hand_quantity) FROM inventory_batch_locations l ");
        sql.append(" WHERE l.bin_id = bn.bin_id),0) AS on_hand_total, ");
        sql.append("COALESCE((SELECT SUM(l.reserved_quantity) FROM inventory_batch_locations l ");
        sql.append(" WHERE l.bin_id = bn.bin_id),0) AS reserved_total ");
        sql.append("FROM storage_bins bn ");
        sql.append("JOIN storage_racks r ON r.rack_id = bn.rack_id ");
        sql.append("JOIN storage_zones z ON z.zone_id = r.zone_id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (rackId != null && rackId > 0) {
            sql.append("AND bn.rack_id = ? ");
            params.add(rackId);
        }
        if (zoneId != null && zoneId > 0) {
            sql.append("AND r.zone_id = ? ");
            params.add(zoneId);
        }
        if (q != null && !q.trim().isEmpty()) {
            sql.append("AND (bn.bin_code LIKE ? OR r.rack_code LIKE ? OR z.zone_code LIKE ?) ");
            String like = "%" + q.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (status != null && !status.isEmpty()) {
            sql.append("AND bn.status = ? ");
            params.add(status);
        }
        sql.append("ORDER BY z.zone_code, r.rack_code, bn.bin_code");
        try {
            connection = getConnection();
            if (connection == null) {
                return list;
            }
            statement = connection.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                list.add(mapBin(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findBins failed", ex);
        } finally {
            closeResources();
        }
        return list;
    }

    public StorageBin findBinById(long binId) {
        String sql = "SELECT bn.*, r.rack_code, r.rack_name, r.zone_id, z.zone_code, z.zone_name, "
                + "0 AS on_hand_total, 0 AS reserved_total "
                + "FROM storage_bins bn "
                + "JOIN storage_racks r ON r.rack_id = bn.rack_id "
                + "JOIN storage_zones z ON z.zone_id = r.zone_id "
                + "WHERE bn.bin_id = ?";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, binId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return mapBin(resultSet);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findBinById failed", ex);
        } finally {
            closeResources();
        }
        return null;
    }

    /** Active bin only — used by GR confirm / transfer / JSON cascade. */
    public StorageBin findActiveBin(Connection conn, long binId) throws SQLException {
        String sql = "SELECT bn.*, r.rack_code, r.rack_name, r.zone_id, z.zone_code, z.zone_name, "
                + "0 AS on_hand_total, 0 AS reserved_total "
                + "FROM storage_bins bn "
                + "JOIN storage_racks r ON r.rack_id = bn.rack_id "
                + "JOIN storage_zones z ON z.zone_id = r.zone_id "
                + "WHERE bn.bin_id = ? AND bn.status = 'HOAT_DONG' "
                + "AND r.status = 'HOAT_DONG' AND z.status = 'HOAT_DONG'";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, binId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapBin(rs);
            }
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    public MutateResult createBin(long rackId, String code) {
        if (rackId <= 0 || isBlank(code)) {
            return MutateResult.fail("required");
        }
        String sql = "INSERT INTO storage_bins (rack_id, bin_code, status) VALUES (?,?,'HOAT_DONG')";
        try {
            connection = getConnection();
            if (connection == null) {
                return MutateResult.fail("db");
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, rackId);
            statement.setString(2, code.trim().toUpperCase());
            statement.executeUpdate();
            return MutateResult.success();
        } catch (SQLException ex) {
            if (isDuplicate(ex)) {
                return MutateResult.fail("duplicate");
            }
            LOG.log(Level.SEVERE, "createBin failed", ex);
            return MutateResult.fail("db");
        } finally {
            closeResources();
        }
    }

    public MutateResult updateBin(long binId, String code) {
        if (isBlank(code)) {
            return MutateResult.fail("required");
        }
        String sql = "UPDATE storage_bins SET bin_code = ? WHERE bin_id = ?";
        try {
            connection = getConnection();
            if (connection == null) {
                return MutateResult.fail("db");
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, code.trim().toUpperCase());
            statement.setLong(2, binId);
            int n = statement.executeUpdate();
            return n > 0 ? MutateResult.success() : MutateResult.fail("notfound");
        } catch (SQLException ex) {
            if (isDuplicate(ex)) {
                return MutateResult.fail("duplicate");
            }
            LOG.log(Level.SEVERE, "updateBin failed", ex);
            return MutateResult.fail("db");
        } finally {
            closeResources();
        }
    }

    public MutateResult setBinStatus(long binId, String status) {
        if (!"HOAT_DONG".equals(status) && !"NGUNG_HOAT_DONG".equals(status)) {
            return MutateResult.fail("badstatus");
        }
        if ("NGUNG_HOAT_DONG".equals(status) && binHasStock(binId)) {
            return MutateResult.fail("hasstock");
        }
        return updateStatus("UPDATE storage_bins SET status = ? WHERE bin_id = ?", status, binId);
    }

    /* ==================== Batch locations (read) ==================== */

    public List<InventoryBatchLocation> findLocationsForBatch(long batchId) {
        List<InventoryBatchLocation> list = new ArrayList<>();
        String sql = "SELECT l.*, b.batch_number, b.expiry_date, b.status AS batch_status, "
                + "b.product_id, p.product_name, p.sku, "
                + "z.zone_id, z.zone_code, r.rack_id, r.rack_code, bn.bin_code "
                + "FROM inventory_batch_locations l "
                + "JOIN inventory_batches b ON b.batch_id = l.batch_id "
                + "JOIN products p ON p.product_id = b.product_id "
                + "JOIN storage_bins bn ON bn.bin_id = l.bin_id "
                + "JOIN storage_racks r ON r.rack_id = bn.rack_id "
                + "JOIN storage_zones z ON z.zone_id = r.zone_id "
                + "WHERE l.batch_id = ? ORDER BY z.zone_code, r.rack_code, bn.bin_code";
        try {
            connection = getConnection();
            if (connection == null) {
                return list;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, batchId);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                list.add(mapBatchLocation(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findLocationsForBatch failed", ex);
        } finally {
            closeResources();
        }
        return list;
    }

    public List<InventoryBatchLocation> searchLocations(Long zoneId, Long rackId, Long binId,
            Long productId, Long batchId, String q) {
        List<InventoryBatchLocation> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT l.*, b.batch_number, b.expiry_date, b.status AS batch_status, ");
        sql.append("b.product_id, p.product_name, p.sku, ");
        sql.append("z.zone_id, z.zone_code, r.rack_id, r.rack_code, bn.bin_code ");
        sql.append("FROM inventory_batch_locations l ");
        sql.append("JOIN inventory_batches b ON b.batch_id = l.batch_id ");
        sql.append("JOIN products p ON p.product_id = b.product_id ");
        sql.append("JOIN storage_bins bn ON bn.bin_id = l.bin_id ");
        sql.append("JOIN storage_racks r ON r.rack_id = bn.rack_id ");
        sql.append("JOIN storage_zones z ON z.zone_id = r.zone_id WHERE l.on_hand_quantity > 0 ");
        List<Object> params = new ArrayList<>();
        if (zoneId != null && zoneId > 0) {
            sql.append("AND z.zone_id = ? ");
            params.add(zoneId);
        }
        if (rackId != null && rackId > 0) {
            sql.append("AND r.rack_id = ? ");
            params.add(rackId);
        }
        if (binId != null && binId > 0) {
            sql.append("AND bn.bin_id = ? ");
            params.add(binId);
        }
        if (productId != null && productId > 0) {
            sql.append("AND b.product_id = ? ");
            params.add(productId);
        }
        if (batchId != null && batchId > 0) {
            sql.append("AND b.batch_id = ? ");
            params.add(batchId);
        }
        if (q != null && !q.trim().isEmpty()) {
            sql.append("AND (p.product_name LIKE ? OR p.sku LIKE ? OR b.batch_number LIKE ? ");
            sql.append("OR z.zone_code LIKE ? OR r.rack_code LIKE ? OR bn.bin_code LIKE ?) ");
            String like = "%" + q.trim() + "%";
            for (int i = 0; i < 6; i++) {
                params.add(like);
            }
        }
        sql.append("ORDER BY z.zone_code, r.rack_code, bn.bin_code, p.product_name");
        try {
            connection = getConnection();
            if (connection == null) {
                return list;
            }
            statement = connection.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                list.add(mapBatchLocation(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "searchLocations failed", ex);
        } finally {
            closeResources();
        }
        return list;
    }

    /**
     * FEFO + location: allocatable batches for a product, split by bin,
     * ordered by expiry ASC, then bin_id ASC. Caller locks with FOR UPDATE.
     */
    public List<BinStockRow> loadFefoBinStockForUpdate(Connection conn, long productId)
            throws SQLException {
        String sql = "SELECT l.batch_id, l.bin_id, l.on_hand_quantity, l.reserved_quantity, "
                + "b.expiry_date, b.batch_number, b.status "
                + "FROM inventory_batch_locations l "
                + "JOIN inventory_batches b ON b.batch_id = l.batch_id "
                + "JOIN storage_bins bn ON bn.bin_id = l.bin_id "
                + "JOIN storage_racks r ON r.rack_id = bn.rack_id "
                + "JOIN storage_zones z ON z.zone_id = r.zone_id "
                + "WHERE b.product_id = ? AND " + InventoryDAO.ALLOCATABLE + " "
                + "AND bn.status = 'HOAT_DONG' AND r.status = 'HOAT_DONG' AND z.status = 'HOAT_DONG' "
                + "AND l.on_hand_quantity - l.reserved_quantity > 0 "
                + "ORDER BY b.expiry_date ASC, b.batch_id ASC, l.bin_id ASC "
                + "FOR UPDATE";
        List<BinStockRow> rows = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, productId);
            rs = ps.executeQuery();
            while (rs.next()) {
                BinStockRow row = new BinStockRow();
                row.batchId = rs.getLong("batch_id");
                row.binId = rs.getLong("bin_id");
                row.onHand = rs.getInt("on_hand_quantity");
                row.reserved = rs.getInt("reserved_quantity");
                row.expiryDate = rs.getDate("expiry_date");
                row.batchNumber = rs.getString("batch_number");
                row.status = rs.getString("status");
                rows.add(row);
            }
            return rows;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /* ==================== Dual-write mutations (require open TX) ==================== */

    /** Add physical stock at bin; sync batch totals. */
    public void addOnHandAtBin(Connection conn, long batchId, long binId, int qty)
            throws SQLException {
        if (qty <= 0) {
            throw new SQLException("qty must be > 0");
        }
        upsertAdd(conn, batchId, binId, qty, 0);
        syncBatchTotals(conn, batchId);
    }

    /**
     * Reduce physical + reserved together (dispatch / POS sale from a reserved
     * or free bin). available check: on_hand - reserved >= freeQty needed for
     * unreserved sales; for reserved release+ship use decreaseReservedThenOnHand.
     */
    public void decreaseOnHandAtBin(Connection conn, long batchId, long binId, int qty)
            throws SQLException {
        if (qty <= 0) {
            throw new SQLException("qty must be > 0");
        }
        String sql = "UPDATE inventory_batch_locations "
                + "SET on_hand_quantity = on_hand_quantity - ? "
                + "WHERE batch_id = ? AND bin_id = ? "
                + "AND on_hand_quantity - reserved_quantity >= ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, qty);
            ps.setLong(2, batchId);
            ps.setLong(3, binId);
            ps.setInt(4, qty);
            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("insufficient saleable at bin " + binId);
            }
        } finally {
            closeQuietly(ps);
        }
        syncBatchTotals(conn, batchId);
    }

    /** Raise reserved at bin (online hold). Requires saleable >= qty. */
    public void increaseReservedAtBin(Connection conn, long batchId, long binId, int qty)
            throws SQLException {
        if (qty <= 0) {
            throw new SQLException("qty must be > 0");
        }
        String sql = "UPDATE inventory_batch_locations "
                + "SET reserved_quantity = reserved_quantity + ? "
                + "WHERE batch_id = ? AND bin_id = ? "
                + "AND on_hand_quantity - reserved_quantity >= ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, qty);
            ps.setLong(2, batchId);
            ps.setLong(3, binId);
            ps.setInt(4, qty);
            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("insufficient saleable to reserve at bin " + binId);
            }
        } finally {
            closeQuietly(ps);
        }
        syncBatchTotals(conn, batchId);
    }

    /** Release reserved only (cancel / reject). */
    public void decreaseReservedAtBin(Connection conn, long batchId, long binId, int qty)
            throws SQLException {
        if (qty <= 0) {
            throw new SQLException("qty must be > 0");
        }
        String sql = "UPDATE inventory_batch_locations "
                + "SET reserved_quantity = reserved_quantity - ? "
                + "WHERE batch_id = ? AND bin_id = ? AND reserved_quantity >= ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, qty);
            ps.setLong(2, batchId);
            ps.setLong(3, binId);
            ps.setInt(4, qty);
            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("insufficient reserved at bin " + binId);
            }
        } finally {
            closeQuietly(ps);
        }
        syncBatchTotals(conn, batchId);
    }

    /**
     * Dispatch reserved stock: drop on_hand and reserved by the same qty.
     */
    public void consumeReservedAtBin(Connection conn, long batchId, long binId, int qty)
            throws SQLException {
        if (qty <= 0) {
            throw new SQLException("qty must be > 0");
        }
        String sql = "UPDATE inventory_batch_locations "
                + "SET on_hand_quantity = on_hand_quantity - ?, "
                + "    reserved_quantity = reserved_quantity - ? "
                + "WHERE batch_id = ? AND bin_id = ? "
                + "AND on_hand_quantity >= ? AND reserved_quantity >= ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, qty);
            ps.setInt(2, qty);
            ps.setLong(3, batchId);
            ps.setLong(4, binId);
            ps.setInt(5, qty);
            ps.setInt(6, qty);
            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("cannot consume reserved at bin " + binId);
            }
        } finally {
            closeQuietly(ps);
        }
        syncBatchTotals(conn, batchId);
    }

    /** Set absolute on_hand at bin (stocktake). Must keep reserved &lt;= on_hand. */
    public void setOnHandAtBin(Connection conn, long batchId, long binId, int newOnHand)
            throws SQLException {
        if (newOnHand < 0) {
            throw new SQLException("on_hand cannot be negative");
        }
        String sql = "UPDATE inventory_batch_locations "
                + "SET on_hand_quantity = ? "
                + "WHERE batch_id = ? AND bin_id = ? AND reserved_quantity <= ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, newOnHand);
            ps.setLong(2, batchId);
            ps.setLong(3, binId);
            ps.setInt(4, newOnHand);
            int n = ps.executeUpdate();
            if (n != 1) {
                // Row may not exist yet (count found stock where system had 0)
                if (newOnHand == 0) {
                    return;
                }
                upsertSet(conn, batchId, binId, newOnHand, 0);
            }
        } finally {
            closeQuietly(ps);
        }
        syncBatchTotals(conn, batchId);
    }

    /**
     * Internal transfer: move free saleable qty from source bin to dest bin.
     * Batch totals unchanged.
     */
    public void transferBetweenBins(Connection conn, long batchId,
            long fromBinId, long toBinId, int qty) throws SQLException {
        if (fromBinId == toBinId) {
            throw new SQLException("same bin");
        }
        if (qty <= 0) {
            throw new SQLException("qty must be > 0");
        }
        decreaseOnHandAtBin(conn, batchId, fromBinId, qty);
        addOnHandAtBin(conn, batchId, toBinId, qty);
    }

    public void syncBatchTotals(Connection conn, long batchId) throws SQLException {
        String sql = "UPDATE inventory_batches b SET "
                + "on_hand_quantity = ("
                + "  SELECT COALESCE(SUM(l.on_hand_quantity),0) FROM inventory_batch_locations l "
                + "  WHERE l.batch_id = b.batch_id"
                + "), "
                + "reserved_quantity = ("
                + "  SELECT COALESCE(SUM(l.reserved_quantity),0) FROM inventory_batch_locations l "
                + "  WHERE l.batch_id = b.batch_id"
                + ") "
                + "WHERE b.batch_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    public void insertMovementWithBins(Connection conn, long batchId, Long fromBinId, Long toBinId,
            long performedBy, String movementType, int onHandChange, int reservedChange,
            int onHandBefore, int onHandAfter, int reservedBefore, int reservedAfter,
            String referenceType, Long referenceId, String reason) throws SQLException {
        String sql = "INSERT INTO inventory_movements "
                + "(batch_id, from_bin_id, to_bin_id, performed_by, movement_type, "
                + " on_hand_change, reserved_change, on_hand_before, on_hand_after, "
                + " reserved_before, reserved_after, reference_type, reference_id, reason) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            if (fromBinId == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, fromBinId);
            }
            if (toBinId == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, toBinId);
            }
            ps.setLong(4, performedBy);
            ps.setString(5, movementType);
            ps.setInt(6, onHandChange);
            ps.setInt(7, reservedChange);
            ps.setInt(8, onHandBefore);
            ps.setInt(9, onHandAfter);
            ps.setInt(10, reservedBefore);
            ps.setInt(11, reservedAfter);
            ps.setString(12, referenceType);
            if (referenceId == null) {
                ps.setNull(13, java.sql.Types.BIGINT);
            } else {
                ps.setLong(13, referenceId);
            }
            ps.setString(14, reason);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    public int getBatchOnHand(Connection conn, long batchId) throws SQLException {
        String sql = "SELECT on_hand_quantity FROM inventory_batches WHERE batch_id = ? FOR UPDATE";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("batch not found " + batchId);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    public int getLocationOnHand(Connection conn, long batchId, long binId) throws SQLException {
        String sql = "SELECT on_hand_quantity, reserved_quantity FROM inventory_batch_locations "
                + "WHERE batch_id = ? AND bin_id = ? FOR UPDATE";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            ps.setLong(2, binId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("on_hand_quantity");
            }
            return 0;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    public int getLocationReserved(Connection conn, long batchId, long binId) throws SQLException {
        String sql = "SELECT reserved_quantity FROM inventory_batch_locations "
                + "WHERE batch_id = ? AND bin_id = ? FOR UPDATE";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            ps.setLong(2, binId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /* ==================== helpers ==================== */

    private void upsertAdd(Connection conn, long batchId, long binId, int onHandAdd, int reservedAdd)
            throws SQLException {
        String sql = "INSERT INTO inventory_batch_locations "
                + "(batch_id, bin_id, on_hand_quantity, reserved_quantity) VALUES (?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE "
                + "on_hand_quantity = on_hand_quantity + VALUES(on_hand_quantity), "
                + "reserved_quantity = reserved_quantity + VALUES(reserved_quantity)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            ps.setLong(2, binId);
            ps.setInt(3, onHandAdd);
            ps.setInt(4, reservedAdd);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    private void upsertSet(Connection conn, long batchId, long binId, int onHand, int reserved)
            throws SQLException {
        String sql = "INSERT INTO inventory_batch_locations "
                + "(batch_id, bin_id, on_hand_quantity, reserved_quantity) VALUES (?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE on_hand_quantity = VALUES(on_hand_quantity), "
                + "reserved_quantity = VALUES(reserved_quantity)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            ps.setLong(2, binId);
            ps.setInt(3, onHand);
            ps.setInt(4, reserved);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    private boolean zoneHasStock(long zoneId) {
        String sql = "SELECT 1 FROM inventory_batch_locations l "
                + "JOIN storage_bins bn ON bn.bin_id = l.bin_id "
                + "JOIN storage_racks r ON r.rack_id = bn.rack_id "
                + "WHERE r.zone_id = ? AND l.on_hand_quantity > 0 LIMIT 1";
        return exists(sql, zoneId);
    }

    private boolean rackHasStock(long rackId) {
        String sql = "SELECT 1 FROM inventory_batch_locations l "
                + "JOIN storage_bins bn ON bn.bin_id = l.bin_id "
                + "WHERE bn.rack_id = ? AND l.on_hand_quantity > 0 LIMIT 1";
        return exists(sql, rackId);
    }

    private boolean binHasStock(long binId) {
        String sql = "SELECT 1 FROM inventory_batch_locations l "
                + "WHERE l.bin_id = ? AND l.on_hand_quantity > 0 LIMIT 1";
        return exists(sql, binId);
    }

    private boolean exists(String sql, long id) {
        try {
            connection = getConnection();
            if (connection == null) {
                return true;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, id);
            resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "exists check failed", ex);
            return true;
        } finally {
            closeResources();
        }
    }

    private MutateResult updateStatus(String sql, String status, long id) {
        try {
            connection = getConnection();
            if (connection == null) {
                return MutateResult.fail("db");
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, status);
            statement.setLong(2, id);
            int n = statement.executeUpdate();
            return n > 0 ? MutateResult.success() : MutateResult.fail("notfound");
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "updateStatus failed", ex);
            return MutateResult.fail("db");
        } finally {
            closeResources();
        }
    }

    private static StorageZone mapZone(ResultSet rs) throws SQLException {
        StorageZone z = new StorageZone();
        z.setZoneId(rs.getLong("zone_id"));
        z.setZoneCode(rs.getString("zone_code"));
        z.setZoneName(rs.getString("zone_name"));
        z.setStatus(rs.getString("status"));
        z.setCreatedAt(rs.getTimestamp("created_at"));
        z.setUpdatedAt(rs.getTimestamp("updated_at"));
        z.setRackCount(rs.getInt("rack_count"));
        z.setOnHandTotal(rs.getInt("on_hand_total"));
        return z;
    }

    private static StorageRack mapRack(ResultSet rs) throws SQLException {
        StorageRack r = new StorageRack();
        r.setRackId(rs.getLong("rack_id"));
        r.setZoneId(rs.getLong("zone_id"));
        r.setRackCode(rs.getString("rack_code"));
        r.setRackName(rs.getString("rack_name"));
        r.setStatus(rs.getString("status"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        r.setUpdatedAt(rs.getTimestamp("updated_at"));
        r.setZoneCode(rs.getString("zone_code"));
        r.setZoneName(rs.getString("zone_name"));
        r.setBinCount(rs.getInt("bin_count"));
        r.setOnHandTotal(rs.getInt("on_hand_total"));
        return r;
    }

    private static StorageBin mapBin(ResultSet rs) throws SQLException {
        StorageBin b = new StorageBin();
        b.setBinId(rs.getLong("bin_id"));
        b.setRackId(rs.getLong("rack_id"));
        b.setBinCode(rs.getString("bin_code"));
        b.setStatus(rs.getString("status"));
        b.setCreatedAt(rs.getTimestamp("created_at"));
        b.setUpdatedAt(rs.getTimestamp("updated_at"));
        b.setZoneId(rs.getLong("zone_id"));
        b.setZoneCode(rs.getString("zone_code"));
        b.setZoneName(rs.getString("zone_name"));
        b.setRackCode(rs.getString("rack_code"));
        b.setRackName(rs.getString("rack_name"));
        b.setOnHandTotal(rs.getInt("on_hand_total"));
        b.setReservedTotal(rs.getInt("reserved_total"));
        return b;
    }

    private static InventoryBatchLocation mapBatchLocation(ResultSet rs) throws SQLException {
        InventoryBatchLocation l = new InventoryBatchLocation();
        l.setBatchLocationId(rs.getLong("batch_location_id"));
        l.setBatchId(rs.getLong("batch_id"));
        l.setBinId(rs.getLong("bin_id"));
        l.setOnHandQuantity(rs.getInt("on_hand_quantity"));
        l.setReservedQuantity(rs.getInt("reserved_quantity"));
        l.setCreatedAt(rs.getTimestamp("created_at"));
        l.setUpdatedAt(rs.getTimestamp("updated_at"));
        l.setBatchNumber(rs.getString("batch_number"));
        l.setExpiryDate(rs.getDate("expiry_date"));
        l.setBatchStatus(rs.getString("batch_status"));
        l.setProductId(rs.getLong("product_id"));
        l.setProductName(rs.getString("product_name"));
        l.setSku(rs.getString("sku"));
        l.setZoneId(rs.getLong("zone_id"));
        l.setZoneCode(rs.getString("zone_code"));
        l.setRackId(rs.getLong("rack_id"));
        l.setRackCode(rs.getString("rack_code"));
        l.setBinCode(rs.getString("bin_code"));
        return l;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean isDuplicate(SQLException ex) {
        return ex.getErrorCode() == 1062;
    }

    private static void closeQuietly(AutoCloseable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
                // ignore
            }
        }
    }
}
