1. Resolve TODO with sending to a specific port. Generally think when to send just to one address and understand the multicast. **Mariusz**

   Add a new receiver that is not for multicast messages. Change _sendSessionDetails()_ to use that.

2. Another multicast just for the group inside the presentation. **Lorenzo**

   The leader gets all used ports and creates a new one just for the session! That happens before the session is created. >= 1024

   The port should be the session's attribute.

3. Email to set up a presentation on Friday. **Mariusz**

4. What if messages do not arrive in order? How do we handle that?

   Send a NACK when identified a miss (**identify packet miss** from _Remarks.md_)

5. Bully election (or any other) - identifiers for each user.

6. Users (nodes) may join while the presentation is ongoing. In such case they choose the node from which to download the presentation in a way to share the load.

7. During a session the leader may pass its role to another user, which subsequently becomes the new leader.