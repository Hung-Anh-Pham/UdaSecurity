# The UdaSecurity project

## How to run this project with commandline?

- At the root directory `udasecurity`, open the `Ternimal` and run the command:

```shell
mvn clean package
```

- After the JVM successfully build this project into `.jar` file, run the following command:

```shell
java -jar securityservice/target/securityservice-1.0-SNAPSHOT.jar
```

## How to achieve the SpotBugs report

- Run the following command:

```shell
mvn install site
```

Note: this process take several minutes to complete.

