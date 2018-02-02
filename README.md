# DEBS Grand Challenge 2018

Here you can find some helpful sourcecodes which should ease your integration into the HOBBIT platform and participation in the [DEBS Grand Challenge 2018](http://www.cs.otago.ac.nz/debs2018/calls/gc.html). 

The sourcecodes use the [HOBBIT Java SDK](https://github.com/hobbit-project/java-sdk-example) and should allow challenge participants to debug their systems using benchmark workload locally without having a running platform instance. 
The benchmarking system can be tested as pure java code or/and being packed (automatically, by demand) into docker image. 
Finally the docker image of the system may be uploaded into the [HOBBIT online platform](http://master.project-hobbit.eu) (as described [here](https://github.com/hobbit-project/platform/wiki/Push-a-docker-image) and [here](https://github.com/hobbit-project/platform/wiki/System-meta-data-file)) and executed there under the online DEBS GC 2018 benchmark, which will be used for the challenge.

# Benchmark description
The reposity containts the following components:
- Data generator  
- Sample system
- Sample system test

The DEBS GC 2018 benchmark will send a stream of tuples to the benchmarking system and expects responces (predictions) from the system to be ranked by [defined KPIs](http://www.cs.otago.ac.nz/debs2018/calls/gc.html). Each next data tuple for each particular ship can be received by the system only if the responce (prediction) for the previous tuple for this ship was sent from the system to the evaluation storage, which acknowdleges data generator. A simplified version of data generator is included to the repository to allow participants to get the stream of training data directly to the sample system. A simplified versions of any other benchmark components (benchmark controller, task generator, evaluatuion storage, evaluation module) required for local debugging will be automatically downloaded as docker images by the docker software. 

The online DEBS GC 2018 benchmark will use another implementations (docker images) of components (data generator, task generator, evaluation storage) where acknowledgement messages will be encrypted. 

# Usage
## Before you start
1) Make sure that docker (v17 and later) is installed (or install it by `sudo curl -sSL https://get.docker.com/ | sh`)
2) Make sure that maven (v3 and later) is installed (or install it by `sudo apt-get install maven`)
3) Clone this repository (`https://github.com/hobbit-project/DEBS-GC-2018.git`)
4) Open the cloned repository in any IDE you like. 
5) Make sure that hobbit-java-sdk dependency (declared in [pom.xml](https://github.com/hobbit-project/java-sdk-example/blob/master/pom.xml)) is installed into your local maven repository (or install it by executing the `mvn validate` command)

## How to create a system for existing benchmark
1) Please find the basic HOBBIT-compatible system implementation in `SampleSystem.java`. You may extend it with the logic of your future system. More details about the design of HOBBIT-compatible system adapters can be found [here](https://github.com/hobbit-project/platform/wiki/Develop-a-system-adapter-in-Java) and [here](https://github.com/hobbit-project/platform/wiki/Develop-a-system-adapter). 
2) Put the training data set under the data folder (if required) and modify path to it in `DataGenerator.java`. 
3) Run the `checkHealth()` method from the `SampleSystemTest.java` to test/debug your system as pure java code.
3) Once your system correctly tested as pure java code you may test it being packed into docker container. To build docker image you for the system you have to configure values in the `SamplesDockersBuilder.java`, package your code into jar file (`mvn package -DskipTests=true`) and execute the `buildImages()` from the `SampleSystemTest.java`. Image building is automatic, but  is on-demand, i.e. you have to check the actuality and rebuild images (inc. rebuilding jar file) by your own.
3) Run the `checkHealthDockerized()` method from the `SampleSystemTest.java` to test/debug your system as docker container. All internal logs from containers will be provided. You may skip logs output from other components via `skipLogsReadingProperty()`.
5) Once you have tested docker image if your system you may it into the HOBBIT platform. Please follow the instructions of the standard procedure (decribed [here](https://github.com/hobbit-project/platform/wiki/Push-a-docker-image) and [here](https://github.com/hobbit-project/platform/wiki/System-meta-data-file)).

## Benchmark-sensitive information
Please check this section later to find the latest sensitive information about the online DEBS GC 2018 benchmark (to be announced later): 
1) URI of the benchmarkAPI to be placed into your system.ttl file
2) URIs of input parameters (like QueryType), which will be send to benchmarking system from the benchmark. 

## FAQ
Feel free to ask any questions regading the DEBS Grand Challenge 2018 under the [Issues](https://github.com/hobbit-project/DEBS-GC-2018/issues) tab.

Please [leave a request](https://github.com/hobbit-project/DEBS-GC-2018/issues/5) if you need a python-based sample system example.
