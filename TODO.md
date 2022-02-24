# TO DO

1. Another multicast just for the group inside the presentation. **Lorenzo**

   The leader gets all used ports and creates a new one just for the session! That happens before the session is created. >= 1024

   The port should be the session's attribute.

2. What if messages do not arrive in order? How do we handle that?

   Send a NACK when identified a miss (**identify packet miss** from _Remarks.md_)

3. Bully election (or any other) - identifiers for each user.

4. Users (nodes) may join while the presentation is ongoing. In such case they choose the node from which to download the presentation in a way to share the load.

5. During a session the leader may pass its role to another user, which subsequently becomes the new leader.

6. Look at the Docker for testing purposes. (How to display the app running inside the docker. **Mariusz**)

7. Think of creating and cleaning the sockets. Maybe use the same sockets for sending and deleting. **Mariusz**
