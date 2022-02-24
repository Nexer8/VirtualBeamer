FROM maven:3.8.1-openjdk-17

COPY . /home/app/

WORKDIR /home/app

RUN mvn compile

CMD ["mvn", "javafx:run"]