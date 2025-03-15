package hepker.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles all interaction between Agent and SQLite.
 */
final class DataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataManager.class);
    private static final String SQLKEY = "jdbc:sqlite:src/main/resources/data/q_values.db";
    private static final double FAILURE_RETURN_VALUE = 0.0;

    private final ConcurrentHashMap<String, double[]> updatedQValues;
    private final QValueRepository db;

    /**
     * Constructor which is exclusively called by Agent's static instantiation. Safely instantiates a
     * QValueRepository object, which contains SQLite access logic. Handles errors thrown by SQLite
     */
    DataManager() {
        QValueRepository tempDb;
        try {
            tempDb = new QValueRepository(SQLKEY);
            LOGGER.info("Initialized QValueRepository with URL: {}", SQLKEY);
        } catch (SQLException e) {
            LOGGER.error("Failed to initialize QValueRepository with URL: {}", SQLKEY, e);
            throw new RuntimeException("Database initialization failed", e);
        }
        this.db = tempDb;
        this.updatedQValues = new ConcurrentHashMap<>();
    }

    /**
     * Retrieves the actionInt of the best possible action to take given state
     *
     * @param serialKey User-defined String representation of Agent's state
     * @return Index of the best possible action to take in state represented by serialKey
     * argument. If none is found, returns 0.0
     */
    int getMaxQIndex(String serialKey) {
        try {
            return db.getMaxQAction(serialKey);
        } catch (SQLException e) {
            LOGGER.error("Failed to get max Q index for serialKey: {}", serialKey, e);
            return (int) FAILURE_RETURN_VALUE;
        }
    }

    /**
     * Retrieves, if it exists, the learned Q-value of the chosen actionInt given state serialKey
     *
     * @param serialKey User defined String representation of Agent's state
     * @param actionInt The index of the action taken in state String serialKey. Typically, the return
     *                  value of agentObject.getActionInt()
     * @return  Q-value of actionInt given state String serialKey, else 0.0
     */
    double queryQTableForValue(String serialKey, int actionInt) {
        try {
            return db.getQValueFromTable(serialKey, actionInt);
        } catch (SQLException e) {
            LOGGER.error("Failed to query Q value for serialKey: {}, decision: {}", serialKey, actionInt, e);
            return FAILURE_RETURN_VALUE;
        }
    }

    /**
     * From SQLite retrieves the maximum possible q-value given Agent's state
     *
     * @param serialKey User-defined String representation of Agent's state
     * @return  Maximum learnt Q-value of any action taken by Agent in state serialKey
     */
    double getMaxQValue(String serialKey) {
        try {
            return db.getMaxQValue(serialKey);
        } catch (SQLException e) {
            LOGGER.error("Failed to get max Q value for serialKey: {}", serialKey, e);
            return FAILURE_RETURN_VALUE;
        }
    }

    void putUpdatedValue(String serialKey, int index, double inputQ) {
        updatedQValues.compute(serialKey, (key, existingArray) -> {
            double[] resultArray;
            if (existingArray == null) {
                resultArray = new double[index + 1];
            } else if (index >= existingArray.length) {
                resultArray = Arrays.copyOf(existingArray, index + 1);
            } else {
                resultArray = existingArray.clone();
            }
            resultArray[index] = inputQ;
            return resultArray;
        });
    }

    void updateData() {
        try {
            Map<String, double[]> snapshot = new ConcurrentHashMap<>(updatedQValues);
            if (!snapshot.isEmpty()) {
                db.updateQTable(snapshot);
                LOGGER.info("Updated QTable with {} entries", updatedQValues.size());
                updatedQValues.clear();
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to update QTable with {} entries", updatedQValues.size(), e);
        }
    }

    void close() {
        try {
            db.close();
            LOGGER.info("Database connection closed");
        } catch (SQLException e) {
            LOGGER.error("Failed to close database connection", e);
        }
    }
}
