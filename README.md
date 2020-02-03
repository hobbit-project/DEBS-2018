# DEBS Grand Challenge 2018

Here you can find some helpful sourcecodes which should ease your integration into the HOBBIT platform and participation in the [DEBS Grand Challenge 2018](http://www.cs.otago.ac.nz/debs2018/calls/gc.html). 

The sourcecodes use the [HOBBIT Java SDK](https://github.com/hobbit-project/java-sdk) and should allow challenge participants to debug their systems using benchmark workload locally without having a running platform instance. 
The benchmarking system can be tested as pure java code or/and being packed (automatically, by demand) into docker image. 
Finally the docker image of the system may be uploaded into the [HOBBIT online platform](http://master.project-hobbit.eu) (as described [here](https://github.com/hobbit-project/platform/wiki/Push-a-docker-image) and [here](https://github.com/hobbit-project/platform/wiki/System-meta-data-file)) and executed there under the online [DEBS GC 2018 benchmark](https://github.com/hobbit-project/sml-benchmark-v2), which will be used for the challenge.

# Dataset

The datasets can be requested [here](https://www.marinetraffic.com/research/open-research-maritime-vessel-tracking-dataset/).

# Benchmark description
The reposity containts the following components:
- Task generator  
- Sample system
- Sample system test

The [DEBS GC 2018 benchmark](https://github.com/hobbit-project/sml-benchmark-v2) sends a stream of tuples to the benchmarking system and expects responces (predictions) from the system to be ranked by [defined KPIs](http://www.cs.otago.ac.nz/debs2018/calls/gc.html). Each next data tuple for each particular ship can be received by the system only if the responce (prediction) for the previous tuple for this ship was sent from the system to the evaluation storage, which acknowdleges data generator. A simplified version of data generator is included to the repository to allow participants to get the stream of training data directly to the sample system. A simplified versions of any other benchmark components (benchmark controller, task generator, evaluatuion storage, evaluation module) required for local debugging will be automatically downloaded as docker images by the docker software. 

The online [DEBS GC 2018 benchmark](https://github.com/hobbit-project/sml-benchmark-v2) benchmark will use another implementations (docker images) of components (data generator, task generator, evaluation storage) where acknowledgement messages are be encrypted.

# Usage
## Before you start
1) Make sure that Oracle Java 1.8 (or higher) is installed (check by the `java -version`). Or install it by the `sudo add-apt-repository ppa:webupd8team/java && sudo apt-get update && sudo apt-get install oracle-java8-installer -y`.
2) Make sure that docker (v17 and later) is installed (or install it by `sudo curl -sSL https://get.docker.com/ | sh`)
3) Make sure that maven (v3 and later) is installed (or install it by `sudo apt-get install maven -y`)
4) Add the `127.0.0.1 rabbit` line to `/etc/hosts` (Linux) or `C:\Windows\System32\drivers\etc\hosts` (Windows)
5) Clone this repository (`https://github.com/hobbit-project/DEBS-GC-2018.git`)
6) Open the cloned repository in any IDE you like. 
7) Make sure that hobbit-java-sdk dependency (declared in [pom.xml](https://github.com/hobbit-project/java-sdk-example/blob/master/pom.xml)) is installed into your local maven repository (or install it by executing the `mvn validate` command)

## How to create a system for existing benchmark
1) Please find the basic HOBBIT-compatible system implementation in `SystemAdapter.java`. You may extend it with the logic of your future system. More details about the design of HOBBIT-compatible system adapters can be found [here](https://github.com/hobbit-project/platform/wiki/Develop-a-system-adapter-in-Java) and [here](https://github.com/hobbit-project/platform/wiki/Develop-a-system-adapter). 
2) Put the training data set under the data folder (if required) and modify path to it in `DataGenerator.java`. 
3) Run the `checkHealth()` method from the `SampleSystemTest.java` to test/debug your system as pure java code. Running under sudo may be required, if containers not starting (message such as `Got exception while trying to request the creation of an instance of the ...`). 
3) Once your system correctly tested as pure java code you may test it being packed into docker container. To build docker image you for the system you have to configure values in the `SamplesDockersBuilder.java`, package your code into jar file (`mvn package -DskipTests=true`) and execute the `buildImages()` from the `SampleSystemTest.java`. Image building is automatic, but  is on-demand, i.e. you have to check the actuality and rebuild images (inc. rebuilding jar file) by your own.
3) Run the `checkHealthDockerized()` method from the `SampleSystemTest.java` to test/debug your system as docker container. All internal logs from containers will be provided. You may skip logs output from other components via `skipLogsReadingProperty()`.
5) Once you have tested docker image of your system you may upload it into the HOBBIT platform. Please follow the instructions of the standard procedure (decribed [here](https://github.com/hobbit-project/platform/wiki/Push-a-docker-image) and [here](https://github.com/hobbit-project/platform/wiki/System-meta-data-file)) and skip the image building phase.

## How to upload your system to the platform
1. Create a user account via [GUI of the platform](https://master.project-hobbit.eu).
2. Create a new project at [platform's GitLab](https://git.project-hobbit.eu).
3. Modify the `GIT_REPO_PATH` and `PROJECT_NAME` in [Constants.java](https://github.com/hobbit-project/DEBS-GC-2018/blob/master/src/main/java/org/hobbit/debs_2018_gc_samples/Benchmark/Constants.java) to fit the URL of project you've just created. `SYSTEM_IMAGE_NAME` will be used as URI of your system's image (`<imageUri>`) and should finally look like this `git.project-hobbit.eu:4567/<yourUsername>/<yourProjectName>/system-adapter`. 
4. Build docker image using the `buildImages()` from the [SampleSystemTest.java](https://github.com/hobbit-project/DEBS-GC-2018/blob/master/src/test/java/org/hobbit/debs_2018_gc_samples/SampleSystemTest.java). The full URI of build image (`<imageUri>`) will be shown in console. Don't forget to package/repackage your codes by the `mvn package -DskipTests=true` before building/rebuilding the image.
5. Login to the remote gitlab from console: `sudo docker login git.project-hobbit.eu:4567` using the credentials of an account you've just created.
6. Push your image to remote gitlab by the `docker push <imageUri>`.
7. Put the modified `system.ttl` into you project at GitLab. The details about the file see in the section below.
8. Find your system in the GUI under the Benchmarks Tab after selecting the DEBS GC 2018 Benchmark. The GUI will apply the updated `system.ttl` during 30-60 seconds after it has been changed.
  
## How to register for the online challenge
1. Once your system works well in the online platform you may register to the challenges (the training phace and final execution). Please register your system for the both tasks of the challenges. 
2. Systems registered for the `DEBS Grand Challenge 2018 Training phase` will be executed periodically (till the mid of May) over as some part of unseen data. The results  will be publicly available at the [online leaderboards](https://master.project-hobbit.eu/challenges/http:%2F%2Fw3id.org%2Fhobbit%2Fchallenges%2333578642-2a74-4a95-8f9b-24a6f675937f/leaderboards). Participation in training phase is not mandatory, but it should help participants to solve all the problems before the final execution. 
3. Systems registered for the `DEBS Grand Challenge 2018 Final Execution` will be executed once (after the mid of May) over the rest part of unseen data. The results announcement as well as winner award will be held during the DEBS Conference by 25-29 of June.

## Benchmark-sensitive information
Please find the example of [system.ttl](https://github.com/hobbit-project/DEBS-GC-2018/blob/master/system.ttl). 
For your system you have to modify the following:
- System URI - (in the line `<SystemURL> a hobbit:SystemInstance`) - some unique identifier of your system (used by the platform internally).
- Label and Comment - will be displayed in GUI. Please include something (organization name/team name/email/etc...) in the label/comment to identify your team for organizers.
- ImageName: the URL at which your docker image (`<imageUri>`) was uploaded/pushed.

BenchmarkAPI (the line `hobbit:implementsAPI <http://project-hobbit.eu/sml-benchmark-v2/API>;`) should remain unchanged.

In order to switch your system to the query of the current benchmark run (Query 1 or Query 2) the `SYSTEM_PARAMETERS_MODEL` environment variable will be specified by a benchmark. In case you are not using the provided SampleSystem, you have to implement parsing of the `SYSTEM_PARAMETERS_MODEL` environment variable in the initialization of your system. The `SYSTEM_PARAMETERS_MODEL` has the following format:
```{ "@id" : "http://jenaKeyValue.com/URI", "queryType" : "2", "@context" : { "queryType" : { "@id" : "http://project-hobbit.eu/sml-benchmark-v2/queryType", "@type" : "http://www.w3.org/2001/XMLSchema#int" } } }```. 

## News
1 May 2018: Participants, who submitted working system into the platform please email us with a link to any succesfull run and receive an additional training set.

16 Apr 2018: Sample system implemetaion in Python was [announced](https://github.com/hobbit-project/python-sample-system). 

14 Mar 2018: The DEBS GC 2018 Benchmark is [available online](https://project-hobbit.eu/structured-machine-learning-benchmark-v2/) as well as challenges have been created. Please find the benchmark-sensitive information and helpful instructions above. 

9 Mar 2018: The repository and remote docker images were updated. New code shows evaluation of you results predicted by your system. Data Generator was replaced by Task Generator, which still requires training dataset to be downloaded and placed into the data folder. Please delete old-one docker images from your local cache (using `docker rmi <imageName>`) in order to new images being downloaded by the SDK. The online benchmark will be announced soon.

## FAQ
Feel free to ask any questions regading the DEBS Grand Challenge 2018 under the [Issues](https://github.com/hobbit-project/DEBS-GC-2018/issues) tab.

Please [leave a request](https://github.com/hobbit-project/DEBS-GC-2018/issues/5) if you need a python-based example of the sample system.
