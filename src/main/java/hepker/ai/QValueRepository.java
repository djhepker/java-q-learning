package hepker.ai;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Employs SQLite connections to read and write database
 */
final class QValueRepository {
    private static final int BATCH_SIZE = 1000;
    private final AtomicBoolean isInitialized;
    private final HikariDataSource dataSource;

    /**
     * Constructor argument for setting the location of our database
     *
     * @param url String url locating the database being modified
     * @throws SQLException Thrown by HikariConfig or HikariDataSource(config)
     */
    QValueRepository(String url) throws SQLException {
        this.isInitialized = new AtomicBoolean(false);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        config.setIdleTimeout(120000); // 2 minutes
        config.setConnectionTimeout(30000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
        initializeDatabase();
    }

    /**
     * Queries Q-table for the action int corresponding to the maximum Q-value given state serialKey
     *
     * @param serialKey String representation of Agent's state
     * @return Maximum possible Q-value given Agent's state OR 0
     * @throws SQLException Thrown by improper argument or interrupted connection
     */
    int getMaxQAction(String serialKey) throws SQLException {
        final String sql = "SELECT Action FROM QTable WHERE HexKey = ? ORDER BY QValue DESC LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, serialKey);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt("Action") : 0;
            }
        }
    }

    /**
     * Getter retrieves Q-value corresponding to action taken in state serialKey
     *
     * @param serialKey String representation of Agent's state
     * @param action int index of the action taken in state serialKey
     * @return Q-value corresponding to action taken in state serialKey
     * @throws SQLException Thrown by improper arguments or interrupted connection
     */
    double getQValueFromTable(String serialKey, int action) throws SQLException {
        final String sql = "SELECT QValue FROM QTable WHERE HexKey = ? AND Action = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, serialKey);
            pstmt.setInt(2, action);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getDouble("QValue") : 0.0;
            }
        }
    }

    /**
     * Getter for retrieving the maximum Q-value in state serialKey
     *
     * @param serialKey String representation of Agent's state
     * @return Maximum Q-value possible given Agent's state serialKey
     * @throws SQLException Thrown by improper arguments or interrupted connection
     */
    double getMaxQValue(String serialKey) throws SQLException {
        final String sql = "SELECT MAX(QValue) AS maxQValue FROM QTable WHERE HexKey = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, serialKey);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getDouble("maxQValue") : 0.0;
            }
        }
    }

    /**
     * Safely places all cached Q-values into database
     *
     * @param cachedQValues Container of cached Q-values
     * @throws SQLException Thrown by improper arguments or interrupted connection
     */
    void updateQTable(Map<String, double[]> cachedQValues) throws SQLException {
        final String sql = "INSERT OR REPLACE INTO QTable (HexKey, Action, QValue) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ppdStmt = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            int batchCount = 0;

            for (Map.Entry<String, double[]> entry : cachedQValues.entrySet()) {
                String key = entry.getKey();
                double[] qValues = entry.getValue();
                for (int i = 0; i < qValues.length; i++) {
                    if (Double.isNaN(qValues[i])) {
                        continue;
                    }
                    ppdStmt.setString(1, key);
                    ppdStmt.setInt(2, i);
                    ppdStmt.setDouble(3, qValues[i]);
                    ppdStmt.addBatch();

                    if (++batchCount >= BATCH_SIZE) {
                        ppdStmt.executeBatch();
                        batchCount = 0;
                    }
                }
            }
            if (batchCount > 0) {
                ppdStmt.executeBatch();
            }
            connection.commit();
        }
    }

    /**
     * Closes database connection. Only used once training has finished
     *
     * @throws SQLException Thrown when unable to close database
     */
    void close() throws SQLException {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Helper which creates the database if we do not already have it
     *
     * @throws SQLException Thrown when unable to create database
     */
    private void initializeDatabase() throws SQLException {
        if (isInitialized.compareAndSet(false, true)) {
            createTable();
        }
    }

    /**
     * Creates query and executes creation of database where Q-values will be stored
     *
     * @throws SQLException Creation was interrupted
     */
    private void createTable() throws SQLException {
        final String sqlCreateTable = """
                CREATE TABLE IF NOT EXISTS QTable (
                    HexKey TEXT NOT NULL,
                    Action INTEGER NOT NULL,
                    QValue REAL NOT NULL,
                    PRIMARY KEY (HexKey, Action)
                ) WITHOUT ROWID""";

        final String sqlCreateIndex = "CREATE INDEX IF NOT EXISTS idx_qvalue ON QTable (HexKey, QValue DESC);";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(false);
            stmt.execute(sqlCreateTable);
            stmt.execute(sqlCreateIndex);
            conn.commit();
        }
    }
}