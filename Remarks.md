# Remarks

## Creating and joining a session

To avoid introducing network addresses to join a session a centralized server approach may be employed. This server would be responsible for managing all the sessions, hence a presenter would need to contact it to establish a session. Similarly, all the clients would need to contact the server to find a session they want to join.

## Group communication

*TODO*

## Leader election

The nodes (including the leader) may crash. In such case the leader becomes the session creator, or the leader is elected if the session creator also crashed.

### Bully election algorithm

> The most number of messages are exchanged in the group when the process with the lowest ID initiates an election. This process sends $(N−1)$ Election messages, the next higher ID sends $(N−2)$ messages, and so on, resulting in $\Theta(N^2)$ election messages. There are also the $\Theta(N^2)$ Alive messages, and $\Theta(N)$ coordinator messages, thus making the overall number messages exchanged in the worst case be $\Theta(N^2)$.

*Source: [Bully algorithm](https://wikipedia.org/en/Bully_algorithm)*

### Ring-based algorithm

Message complexity is *n* (number of nodes) but the time complexity is huge. If *m* is the minimum (or maximum) identifier it is $\Theta(m \cdot n)$.

*Source: [Synchronous Ring](https://disco.ethz.ch/courses/podc_allstars/lecture/chapter3.pdf)*
