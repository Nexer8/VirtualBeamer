FROM maven:3.8.1-openjdk-17-slim

COPY . /home/app/

WORKDIR /home/app

ENV DISPLAY=host.docker.internal:0.0

RUN \
  apt-get update && \
  apt-get install -y libx11-dev libgl-dev libgtk-3-dev

CMD ["mvn", "clean", "javafx:run"]