# TO DO

1. What if messages do not arrive in order? How do we handle that? **Lorenzo**

   Send a *NACK* when identified a miss (**identify packet miss** from *Remarks.md*)

2. Bully election (or any other) - identifiers for each user. **Mariusz** ✅

3. Users (nodes) may join while the presentation is ongoing. In such case they choose the node from which to download the presentation in a way to share the load. **Mariusz**

   Agreement.

4. During a session the leader may pass its role to another user, which subsequently becomes the new leader. *Remember to multicast leader change to everybody (not just the group).* **Lorenzo** ✅

5. Crash detection. Everyone has a random timer and when the time is over, the first one that finishes, sends a multicast `CRASH_CHECK`. If the receiver is the *VIEWER* then stop the timer. Otherwise, (PRESENTER is the receiver), respond with a multicast saying *I'm alive*. **Lorenzo**

6. Think of creating and cleaning the sockets. Maybe use the same sockets for sending and deleting. **Mariusz** ✅

7. Add username text field. **Mariusz** ✅

8. Add leader name and IP address to the session name (it will make it unique). The desired syntax: `<Session name>: <Leader name>(<IP address>)` **Mariusz** ✅

9. Maybe add a label to see who is the presenter. **Mariusz** ✅
