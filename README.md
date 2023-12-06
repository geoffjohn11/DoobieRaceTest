# Doobie Transaction demo

Doobie transaction race demo

### Tested with SDK versions
* sbt 1.9.7
* Java 11.0.18
* Scala 2.13.12

### Installing

* clone repo
* command ```docker compose up``` brings up the database with an accountId 44 with 300 balance
* command ```sbt run``` executes the app

The account balance printed at the end will always be 300, the starting balance
