# Distributed system Project
The project for Distributed Systems (University of Trento 2024/2025).

## Objective
The objective was to implement a distributed system using the Akka Java distributed
library. Such distributed system handle a distributed, simplified, database; and it 
must handle surviving joining, leaving, crash and recover of the node, while mantaining
consistency and the right amount of replication.

## Implementation
Our system is modeled as a finite state automaton, where there are the following states:
INITIAL, NORMAL, CRASHED, RECOVERING, JOINING, LEAVING, and LEFT and two sub-states
SUB, respectively GET and UPDATE.
<img width="987" height="468" alt="image" src="https://github.com/user-attachments/assets/522e8f18-6c7a-44e0-8f09-07ae033c00cc" />



## Complete report
[Complete report pdf](CattoniDiNoia.pdf)
