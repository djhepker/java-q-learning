# Java Q-Learning

[What do I do to use this in my code?](https://github.com/djhepker/java-q-learning/wiki## What do I do?)

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
