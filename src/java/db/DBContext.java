package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared JDBC entry point for PharmaFlow (MySQL).
 *
 * Pattern per rule.md §21: protected fields so subclasses (DAOs) can reuse
 * connection/statement/resultSet, hardcoded credentials in fields, driver
 * loaded in ctor, getConnection() returns a fresh Connection per call,
 * closeResources() closes rs → stmt → conn.
 */
public class DBContext {

    private static final Logger LOG = Logger.getLogger(DBContext.class.getName());

    protected Connection connection;
    protected PreparedStatement statement;
    protected ResultSet resultSet;

    private final String DB_URL
            = "jdbc:mysql://127.0.0.1:3306/pharmaflow?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
    private final String DB_USER = "root";
    private final String DB_PWD = "123456";

    public DBContext() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PWD);
        } catch (ClassNotFoundException | SQLException ex) {
            LOG.log(Level.SEVERE, "DBContext init failed", ex);
        }
    }

    /**
     * @return a fresh JDBC Connection, or null if connection fails.
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PWD);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Could not open DB connection", ex);
            return null;
        }
    }

    /**
     * Closes resultSet → statement → connection, ignoring nulls.
     */
    public void closeResources() {
        try {
            if (resultSet != null && !resultSet.isClosed()) {
                resultSet.close();
            }
            if (statement != null && !statement.isClosed()) {
                statement.close();
            }
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "closeResources failed", ex);
        }
    }
}
