package hepker.utils;

/**
 * Outlines logic required by Agent for AI integration
 * */
public interface DecisionHandler {

    /**
     * Logic to retrieve the stateKey for categorizing Agent's learning
     *
     * @return Unique String representation of the state
     */
    String generateStateKey();

    /**
     * Updates your custom container for actions/decisions. Could be an array, a list, or otherwise. All possible
     * decisions/actions the Agent may make in this particular state.
     * */
    void updateDecisionContainer();

    /**
     * Logic to employ the actionInt obtained from agentObject.getActionInt()
     *
     * @param actionInt Value return of agentObject.getActionInt()
     */
    void performAction(int actionInt);

    /**
     * Implement logic to return the outcome of the Agent's decision. Reward is placed into learning calculation
     * for future use. Output should be an argument to agentObject.setQValue()
     *
     * @return The reward the Agent receives for choosing this action in this state.
     */
    double getDecisionReward();
}
