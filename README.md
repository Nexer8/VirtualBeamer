# Virtual Beamer

A *Java* application to share a set of slides and display them in a synchronous way under control of a single node (the leader).

## Authors

- *Lorenzo Poletti:* lorenzo1.poletti@mail.polimi.it
- *Mariusz Wiśniewski:* mariuszkrzysztof.wisniewski@mail.polimi.it

## Requirements

1. In order to share a presentation the user needs to create a new session.

2. Other nodes join one of the available sessions.

3. There can be more sessions led by different users.

4. When the session creator decides on the presentation to share, the presentation may start.

5. Users (nodes) may join while the presentation is ongoing. In such case they choose the node from which to download the presentation in a way to share the load.

6. The leader (initially the session creator/owner) decides on the slide to be displayed.

7. During a session the leader may pass its role to another user, which subsequently becomes the new leader.

**The solution should focus on minimizing network traffic while sending the presentation from the session creator to the other nodes.**

## Assumptions

- LAN scenario (i.e., link-layer broadcast is available).

- The nodes (including the leader) may crash. In such case the leader becomes the session creator, or the leader is elected if the session creator also crashed.

Privilege a *plug&play* solution that leverages the LAN scenario to avoid the need of entering network addresses and similar information.

## Installation Guide

### Local Build

#### Prerequisites

- Install *OpenJDK 18* from the [official JDK website](https://jdk.java.net/18/).
- Install *Apache Maven 4.0.x* following the [official tutorial](https://maven.apache.org/install.html).

#### Build and Run

```bash
mvn clean javafx:run
```

### Docker

#### Create a network

```bash
docker network create -d bridge --subnet 192.168.1.0/24 --gateway 192.168.1.254 <network_name>
```

#### Build Docker image

```bash
docker build -t <image_name> .
```

#### Run app instance

```bash
docker run --expose 1000-15000 -d --network <network_name> --ip 192.168.1.10 <image_name>
```

It is possible to run multiple instances of the app by executing the above command, but it is important to change the IP address.
