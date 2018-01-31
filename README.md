# DEBS Grand Challenge 2018

Here you can find helpful sourcecodes which should ease your integration into the HOBBIT platform and participation in the DEBS Grand Challenge 2018. 

The sourcecodes are based around the [HOBBIT Java SDK](https://github.com/hobbit-project/java-sdk-example) and should allow challenge participants to debug their systems using benchmark workload locally without having a running platform instance. 
The benchmarking system can be tested as pure java code or/and being packed (automatically, by demand) into docker image. 
Finally the docker image of the system may be uploaded into the [HOBBIT online platform](http://master.project-hobbit.eu) (as described [here](https://github.com/hobbit-project/platform/wiki/Push-a-docker-image) and [here](https://github.com/hobbit-project/platform/wiki/System-meta-data-file)) and executed there under the online DEBS GC 2018 benchmark, which will be used for the challenge.

# Benchmark description
The reposity containts the following components:
- Data generator  
- Evaluation storage 
- Sample system
- Sample system test

The DEBS GC 2018 benchmark will send a stream of tuples to the benchmarking system and expects responces (predictions) from the system to be ranked by [defined KPIs](http://www.cs.otago.ac.nz/debs2018/calls/gc.html). Each next data tuple for each particular ship can be received by the system only if the responce (prediction) for the previous tuple for this ship was sent from the system to the evaluation storage. Evaluation storage acknowledges the data generator, simplified versions of both components are included into 
the repository to be used during local debugging. Any other components (benchmark controller, task generator, evaluation module) required for local debugging will be automatically downloaded as docker images by the docker software. You may provide training dataset  to the data generator just putting .csv file under the data folder and modifying path to it in DataGenerator.java. 

The online DEBS GC 2018 benchmark will use another implementations (docker images) of Data generator and Evaluation storage, where acknowledgements will be encrypted. 

# Usage
## Before you start
1) Make sure that docker (v17 and later) is installed (or install it by `sudo curl -sSL https://get.docker.com/ | sh`)
2) Make sure that maven (v3 and later) is installed (or install it by `sudo apt-get install maven`)
3) Clone this repository (`https://github.com/hobbit-project/DEBS-GC-2018.git`)
4) Make sure that hobbit-java-sdk dependency (declared in [pom.xml](https://github.com/hobbit-project/java-sdk-example/blob/master/pom.xml)) is installed into your local maven repository (or install it by executing the `mvn validate` command)

## How to create a system for existing benchmark
1) Open the cloned project in any IDE you like. 
2) Put the training data set under the data folder (if required) and modify path to it in `DataGenerator.java`.
3) Use `SampleSystem.java` as a basic HOBBIT-compatible implementation of your future system. Run the `checkHealth()` method from the `SampleSystemTest.java` to test/debug your system as pure java code. More details about the design of HOBBIT-compatible system adapters can be found [here](https://github.com/hobbit-project/platform/wiki/Develop-a-system-adapter-in-Java) and [here](https://github.com/hobbit-project/platform/wiki/Develop-a-system-adapter).
4) To build docker image you for the system you have to configure values in the `SamplesDockersBuilder.java`, package your code into jar file (`mvn package -DskipTests=true`) and execute the `buildImages()` from the `SampleSystemTest.java`. Image building is automatic, but  is on-demand, i.e. you have to check the actuality and rebuild images (inc. rebuilding jar file) by your own.
5) To run the docker image of your system you have to switch the value of the (`systemAdapter`) variable in `SampleSystemTest.java`. All internal logs from containers will be provided.
6) To upload your image of the system into the HOBBIT platform please follow the standard procedure (decribed [here](https://github.com/hobbit-project/platform/wiki/Push-a-docker-image)] and [here](https://github.com/hobbit-project/platform/wiki/System-meta-data-file)).

## Benchmark-sensitive information
Please check this section later to find the latest sensitive information about the online DEBS GC 2018 benchmark: 
1) URI of the benchmarkAPI to be placed into your system.ttl file
2) URIs of input parameters (like QueryType), which will be send to benchmarking system from the benchmark. 

## FAQ
Feel free to ask any questions regading the DEBS Grand Challenge 2018 under the [Issues](https://github.com/hobbit-project/DEBS-GC-2018/issues) tab.
