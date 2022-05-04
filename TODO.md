# TO DO

1. Introduce synchronization in data access. **Mariusz** ✅
2. Fix crash detection. **Lorenzo** ✅
3. Change unreliable UDP communication to TCP when necessary. **Mariusz** ✅
4. Implement package loss handling (especially during slide sharing). **Lorenzo**
5. Delete all the *sleeps* from the code. (This should be done after a proper implementation of the package loss handling.) **Lorenzo** ✅
6. What if multiple participants join during `agreeOnSlidesSender` process? **Mariusz** ✅
7. Make sure that after the leader rejoins after a crash, he regains his role (related to crash detection). **Lorenzo** ✅
8. Periodically send the `HELLO` message. Every time we receive the responses, we should verify if our list of active sessions corresponds to the received responses. **Mariusz** ✅
