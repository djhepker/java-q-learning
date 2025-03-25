package hepker.ai;

import java.util.Random;

/**
 * Class utilizes lombok @Getter, @Setter Member variables all have getters and setters.
 * All values can be mutated and obtained with their respective getters, setters. Member variables should
 * not have values greater than 1.0 or lower than 0.0.<br>
 * See README for instructions.<br>
 *      - stateKey = null (String representation of Agent's state<br>
 *      - gamma = 0.90 (How much the agent values learned information; delayed gratification vs instantaneous).<br>
 *      - alpha = 0.82 (Rate at which Agent learns).<br>
 *      - epsilon = 0.85 (Probability that Agent will select a random action).<br>
 *      - rho = 0.0 (updated throughout training, set to 0.0 unless testing).<br>
 *      - currentQ = 0.0 (updated by loadCurrentQ()).<br>
 *      - maxQPrime = 0.0 (This is updated by calculateMaxQPrime()).<br>
 */
public final class Agent {
    private static final DataManager DATA_MANAGER;

    private double gamma = 0.90;
    private double alpha = 0.82;
    private double epsilon = 0.85;
    private double rho = 0.0;
    private double currentQ = 0.0;
    private double maxQPrime = 0.0;
    private String stateKey;

    static {
        DATA_MANAGER = new DataManager();
    }

    /**
     * Empty default constructor for a new Agent. Member variables are initialized prior.<br>
     */
    public Agent() {

    }

    /**
     * Parameterized constructor for initializing Agent with custom values
     * @param inputGamma Percentage Agent values delayed reward/gratification
     * @param inputAlpha Percentage Agent values learning
     * @param inputEpsilon Percent chance Agent will make a random decision
     */
    public Agent(double inputGamma, double inputAlpha, double inputEpsilon) {
        this.gamma = inputGamma;
        this.alpha = inputAlpha;
        this.epsilon = inputEpsilon;
    }

    /**
     * Specialized constructor for setting the exploration rate, epsilon, at initialization
     *
     * @param inputEpsilon The probability Agent will make a random move opposed to a move based on knowledge
     */
    public Agent(double inputEpsilon) {
        this.epsilon = inputEpsilon;
    }

    /**
     * Queues State Action pair to be placed into SQLite table
     *
     * @param stateKeyPrime The post-action encrypted state. Be sure to update your stateKey, post
     *                      action, before sending it as an argument
     * @param actionChoiceInt The index of the decision which resulted in stateKeyPrime. Should be
     *                        the return value of getActionInt()
     */
    public void processData(String stateKeyPrime, int actionChoiceInt) {
        calculateMaxQPrime(stateKeyPrime);
        updateQValue(actionChoiceInt);
    }

    /**
     * Retrieves the decision of the Agent as an int. Decision will be [0, numDecisions) with
     * the only exception being if you are improperly creating, setting stateKey
     *
     * @param numDecisions The upper limit of the number of decisions the Agent has to choose
     *                     from. Exclusive
     * @return An int value of [0, numDecisions)
     */
    public int getActionInt(int numDecisions) {
        if (Math.random() < epsilon) {
            return explore(numDecisions);
        } else {
            return exploit();
        }
    }

    /**
     * Retrieves Q(S,a) value from memory and sets this.currentQ
     *
     * @param inputStateKey The stateKey used to look up qValue from memory
     * @param actionChoiceInt The int representation of the decision chosen in this state. Typically, the
     *                        return value of getActionInt() unless debugging
     */
    public void loadCurrentQ(String inputStateKey, int actionChoiceInt) {
        this.currentQ = getQValue(inputStateKey, actionChoiceInt);
    }

    /**
     * Static method flags the Agent to push all queued Q-values to memory storage. Must be called
     * at the end of every episode
     */
    public static void pushQTableUpdate() {
        DATA_MANAGER.pushData();
    }

    /**
     * Safely closes the Agent's database. To complete Episode, call after pushQTableUpdate()
     */
    public static void closeDatabase() {
        if (DATA_MANAGER != null) {
            DATA_MANAGER.close();
        }
    }

    /**
     * Getter for retrieving the number of Q-values waiting to be stored into memory by pushQTableUpdate().
     * Size is stored as a member variable int, which is incremented, decremented, and set to zero accordingly
     *
     * @return Number of values cached waiting to be stored
     */
    public static int getDataCacheSize() {
        return DATA_MANAGER.getCacheSize();
    }

    /**
     * Sets the number of DataNodes allowed to be held in memory, waiting to be written to Database. When
     * the number of nodes cached equals the batch size, the cache is automatically written to file
     *
     * @param cacheSize The number of data nodes which will be written to file per batch
     */
    public static void setCacheBatchSize(int cacheSize) {
        DATA_MANAGER.setBatchSize(cacheSize);
    }

    /**
     * Gets the gamma value.
     *
     * @return The gamma value held by Agent
     */
    public double getGamma() {
        return gamma;
    }

    /**
     * Sets the inputGamma value. Gamma is the value Agent has for delayed gratification opposed to
     * instant gratification. At 1.0, Agent fully values learned knowledge. With low values, Agent becomes
     * myopic, and will tend to make decisions that pay off short-term opposed to long-term
     *
     * @param inputGamma the new inputGamma value.
     */
    public void setGamma(double inputGamma) {
        this.gamma = inputGamma;
    }

    /**
     * Gets the alpha value.
     *
     * @return the alpha held by Agent
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * Sets the inputAlpha value. Alpha is the
     *
     * @param inputAlpha the new inputAlpha value.
     */
    public void setAlpha(double inputAlpha) {
        this.alpha = inputAlpha;
    }

    /**
     * Gets the epsilon value.
     *
     * @return the epsilon value held by Agent
     */
    public double getEpsilon() {
        return epsilon;
    }

    /**
     * Sets Epsilon to inputEpsilon. Epsilon is the Probability Agent will make a random decision.
     * Set to 1.0 for a perfectly stochastic Agent, which only makes random decisions
     *
     * @param updatedEpsilon the new updatedEpsilon value.
     */
    public void setEpsilon(double updatedEpsilon) {
        this.epsilon = updatedEpsilon;
    }

    /**
     * Gets the rho value
     *
     * @return The rho value, which is used in learning calculations of Agent
     */
    public double getRho() {
        return rho;
    }

    /**
     * Mutator for rho, the reward given to Agent for performing 'action' in 'state'.
     * Used in learning calculates of Agent
     *
     * @param updatedRho the new updatedRho value.
     */
    public void giveReward(double updatedRho) {
        this.rho = updatedRho;
    }

    /**
     * Mutator for updating Agent's stateKey to a new value. developer may want to ensure values
     * are non-exploitable before sending them to Agent for database storage
     *
     * @param argStateKey The object stateKey will point to on resolution
     */
    public void setStateKey(String argStateKey) {
        this.stateKey = argStateKey;
    }

    /**
     * Getter function for String stateKey, which must be set by the developer beforehand
     *
     * @return String stateKey
     */
    public String getStateKey() {
        return stateKey;
    }

    /**
     * Retrieves from memory the maximum possible Q value in the given state and sets maxQPrime
     *
     * @param stateKeyPrimeString String representation of your post-action state
     */
    private void calculateMaxQPrime(String stateKeyPrimeString) {
        if (epsilon == 0.0) {
            return;
        }
        this.maxQPrime = DATA_MANAGER.getMaxQValue(stateKeyPrimeString);
    }

    /**
     * Retrieves the best-known action to take in the given state. Requires that stateKey be properly set first
     * agentObject.setStateKey(yourString);
     *
     * @return int index of the best-known action in the given state
     */
    private int exploit() {
        return DATA_MANAGER.getMaxQIndex(stateKey);
    }

    /**
     * Helper that retrieves a random decision [0,numDecisions)
     *
     * @param numDecisions Exclusive upper bound for decision-making
     * @return A random int [0,numDecisions)
     */
    private int explore(int numDecisions) {
        return new Random().nextInt(numDecisions);
    }

    /**
     * Retrieves the specific q-value for the given stateKey and action
     *
     * @param inputStateKey String stateKey representing this state
     * @param actionInt int representation of the Agent's decision in the given state
     * @return q-value of the given action in the given state
     */
    private double getQValue(String inputStateKey, int actionInt) {
        return DATA_MANAGER.queryQTableForValue(inputStateKey, actionInt);
    }

    /**
     * Calculates the new q-value after the Agent's decision has been chosen and applied to the state
     *
     * @param actionInt Index of the Agent's most-recent decision
     */
    private void updateQValue(int actionInt) {
        double updatedQ = currentQ + alpha * (rho + gamma * maxQPrime - currentQ);
        if (Math.abs(updatedQ - currentQ) > 0.01) {
            DATA_MANAGER.queueDataToCache(stateKey, actionInt, updatedQ);
        }
    }
}