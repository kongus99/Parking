# Parking
### Summary
This is a simple implementation of the parking functionality. It allows the cars to enter and leave the parking, 
as well as check currently owed payment for your ticket. In its current state, supports only single method of payment, 
and a single parking, but that can be easily adjusted, just as the parking capacity and amount of parings. 
It also includes simple Spring REST-ful API and a server to run it. This one contains a small default configuration 
for the parking service, using **dev** as default.
### Prerequisites
To run this project you need Java 11 SDK installed on your computer. You can find it, for example, on 
[AdoptOpenJDK](https://adoptopenjdk.net) site. You also need to have [Git](https://git-scm.com/) installed or download and
extract zipped version of [this repository](https://github.com/kongus99/Parking).
### Installation 
Checkout the project:
```bash
git clone https://github.com/kongus99/Parking.git
```
From the main project folder build the project using Gradle:
```bash
gradlew build
```
Finally, you can run the server using **bootRun** Gradle target:
```bash
gradlew bootRun
```
For deployment, you can always build jar file with Gradle and run it directly.
```bash
gradlew bootJar
```
### Running
Depending on method of installation either run 
[ParkingApplication](https://github.com/kongus99/Parking/blob/master/src/main/java/org/parking/ParkingApplication.java)
main method, or the generated jar file(should be located in **build/libs** subfolder after gradle finishes its build).
### Configuration
The application comes with a default pricing configuration, as well as a simple parking slots config - see 
[ParkingConfiguration](https://github.com/kongus99/Parking/blob/master/src/main/java/org/parking/conf/ParkingConfiguration.java). 
This means it can run out of the box as shown above, however this can be adjusted. In order for that you need to provide your
own [ParkingConfiguration](https://github.com/kongus99/Parking/blob/master/src/main/java/org/parking/conf/ParkingConfiguration.java) implementation,
as well as change the default/active [Spring profile](https://www.baeldung.com/spring-profiles) and provide your configuration bean with that profile.
### Further development
This project in its state should be fully functional, but there are still areas in which it can be improved. Currently, there is no logging
except for standard Spring logging, as well as there is no persistence layer that would allow the server to be restarted without
losing data. The payment method is extremely simplistic and should be replaced by some third party payment provider. There are also other things that can be improved, but for the given scope it should be sufficient.

#### Licence
Parking has MIT [licence](https://github.com/kongus99/Parking/blob/master/LICENCE)
 
