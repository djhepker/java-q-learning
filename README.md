# Java Q-Learning

To use Agent, you must perform the following:<br>
1.    String yourStateString = DecisionHandler.generateStateKey()
2.   agentObject.setStateKey(yourStateString)
3.  DecisionHandler.updateDecisionContainer()
4.   int numDecisions = yourDecisionContainer.getNumDecisions()
5.    int action = agentObject.getActionInt(numDecisions)
6.   DecisionHandler.performAction(actionInt)
7.  DecisionHandler.updateDecisionContainer()
8. String stateKeyPrime = DecisionHandler.generateStateKey()
9.  double yourCustomReward = DecisionHandler.getDecisionReward()
10.   agentObject.setRho(yourCustomReward)
11.    agentObject.learn(stateKeyPrime, actionInt)
12. Repeat [1,11] until Episode is finished
13. Agent.pushQTableUpdate()
14. Agent.closeDataBase()<br>

Optionally, you may create a class to implement DecisionHandler interface, which will lay out the framework for required methods.

Definitions:
* STATE: A programmer-coded String value that is given to Agent to identify its learned values
* ACTION: Moving pieces when it is Agent's turn
* REWARD: Positive, Negative, & Neutral
* EPISODE: An episode is a single training session for Agent. Ends when closeDatabase() is called
* Q-VALUE: Metrics used to evaluate actions at specific states
* MODEL: Q(S,a,S') ─► Model "Q" is action "a" given state "S" results in "S'"<br>└► P(S'|S,a) = Probability of reaching a state "S'" if action "a" is taken in state "S"
* "A" is the set of all possible actions
* "A(s)" defines the set of actions that can be taken while in state "S"
* POLICY: A mapping from "S" to "a"; a solution to the Markov decision process. Indicates action "a" is to be taken while in state "S"
