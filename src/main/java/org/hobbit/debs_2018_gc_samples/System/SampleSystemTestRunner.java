package org.hobbit.debs_2018_gc_samples.System;

import org.hobbit.core.Constants;
import org.hobbit.core.components.Component;
import org.hobbit.debs_2018_gc_samples.Benchmark.TaskGenerator;
import org.hobbit.debs_2018_gc_samples.SampleDockersBuilder;
import org.hobbit.sdk.EnvironmentVariablesWrapper;
import org.hobbit.sdk.JenaKeyValue;
import org.hobbit.sdk.docker.RabbitMqDockerizer;
import org.hobbit.sdk.docker.builders.*;
import org.hobbit.sdk.docker.builders.hobbit.*;
import org.hobbit.sdk.utils.CommandQueueListener;
import org.hobbit.sdk.utils.ComponentsExecutor;
import org.hobbit.sdk.utils.commandreactions.MultipleCommandsReaction;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;

import static org.hobbit.debs_2018_gc_samples.Constants.*;
import static org.hobbit.sdk.CommonConstants.EXPERIMENT_URI;

/**
 * @author Pavel Smirnov
 */

public class SampleSystemTestRunner extends EnvironmentVariablesWrapper {

    private RabbitMqDockerizer rabbitMqDockerizer;
    private ComponentsExecutor componentsExecutor;
    private CommandQueueListener commandQueueListener;

    //Should not be changed, because there images will be called from benchmark-controller
    String benchmarkImageName = "git.project-hobbit.eu:4567/debs_2018_gc/benchmark-controller";
    String dataGeneratorImageName = "git.project-hobbit.eu:4567/debs_2018_gc/data-generator";
    String taskGeneratorImageName = "git.project-hobbit.eu:4567/debs_2018_gc/task-generator";
    String evalStorageImageName = "git.project-hobbit.eu:4567/debs_2018_gc/eval-storage";

    SystemAdapterDockerBuilder systemAdapterBuilder;
    BenchmarkDockerBuilder benchmarkDockerBuilder;
    DataGenDockerBuilder dataGenDockerBuilder;
    EvalStorageDockerBuilder evalStorageDockerBuilder;

    String systemImageName;
    String sessionId;


    public static void main(String[] args) throws Exception {
        SampleSystemTestRunner test = new SampleSystemTestRunner(args[0], args[1]);
        test.checkHealth();
    }


    public SampleSystemTestRunner(String systemImageName, String sessionId){
        this.systemImageName = systemImageName;
        this.sessionId = sessionId;
    }

    public void init(Boolean useCachedImages) throws Exception {

        rabbitMqDockerizer = RabbitMqDockerizer.builder().build();

        setupCommunicationEnvironmentVariables(rabbitMqDockerizer.getHostName(), sessionId);
        setupBenchmarkEnvironmentVariables(EXPERIMENT_URI, createBenchmarkParameters());
        setupGeneratorEnvironmentVariables(1,1);
        setupSystemEnvironmentVariables(SYSTEM_URI, createSystemParameters());

        Boolean skipLogsReading = false;
        benchmarkDockerBuilder = new BenchmarkDockerBuilder(new PullBasedDockersBuilder(benchmarkImageName).skipLogsReading(skipLogsReading));
        dataGenDockerBuilder = new  DataGenDockerBuilder(new PullBasedDockersBuilder(dataGeneratorImageName).skipLogsReading(skipLogsReading));
        evalStorageDockerBuilder = new EvalStorageDockerBuilder(new PullBasedDockersBuilder(evalStorageImageName).skipLogsReading(skipLogsReading));

        systemAdapterBuilder = new SystemAdapterDockerBuilder(new SampleDockersBuilder(SystemAdapter.class).imageName(SYSTEM_IMAGE_NAME).useCachedImage(useCachedImages));

    }

    @Test
    @Ignore
    public void buildImages() throws Exception {
        init(false);

        //pull images from remote repo
        benchmarkDockerBuilder.build().prepareImage();
        dataGenDockerBuilder.build().prepareImage();
        evalStorageDockerBuilder.build().prepareImage();

        //build image of you system
        systemAdapterBuilder.build().prepareImage();
    }

    @Test
    public void checkHealth() throws Exception {
        checkHealth(false);
    }

    @Test
    public void checkHealthDockerized() throws Exception {
        checkHealth(true);
    }

    private void checkHealth(boolean dockerized) throws Exception {

        Boolean useCachedImages = true;

        init(useCachedImages);


        Component benchmarkController = benchmarkDockerBuilder.build();
        Component dataGen = dataGenDockerBuilder.build();
        Component taskGen  = new TaskGenerator();
        Component evalStorage = evalStorageDockerBuilder.build();

        Component systemAdapter = new SystemAdapter();
        if(dockerized)
            systemAdapter = systemAdapterBuilder.build();

        commandQueueListener = new CommandQueueListener();
        componentsExecutor = new ComponentsExecutor();

        rabbitMqDockerizer.run();

        //comment the .systemAdapter(systemAdapter) line below to use the code for running from python
        commandQueueListener.setCommandReactions(
                new MultipleCommandsReaction.Builder(componentsExecutor, commandQueueListener)
                        .benchmarkController(benchmarkController).benchmarkControllerImageName(benchmarkImageName)
                        .dataGenerator(dataGen).dataGeneratorImageName(dataGeneratorImageName)
                        .taskGenerator(taskGen).taskGeneratorImageName(taskGeneratorImageName)
                        .evalStorage(evalStorage).evalStorageImageName(evalStorageImageName)
                        .systemAdapter(systemAdapter)
                        .systemAdapterImageName(SYSTEM_IMAGE_NAME)
                        .build()

        );

        componentsExecutor.submit(commandQueueListener);
        commandQueueListener.waitForInitialisation();

        commandQueueListener.submit(benchmarkImageName, new String[]{ Constants.BENCHMARK_PARAMETERS_MODEL_KEY+"="+ System.getenv().get(Constants.BENCHMARK_PARAMETERS_MODEL_KEY ) });
        commandQueueListener.submit(systemImageName, new String[]{ Constants.SYSTEM_PARAMETERS_MODEL_KEY+"="+ System.getenv().get(Constants.SYSTEM_PARAMETERS_MODEL_KEY ) });

        commandQueueListener.waitForTermination();

        rabbitMqDockerizer.stop();

        Assert.assertFalse(componentsExecutor.anyExceptions());
    }


    private static int QUERY_TYPE = 2;

    public static JenaKeyValue createBenchmarkParameters(){
        JenaKeyValue kv = new JenaKeyValue(EXPERIMENT_URI);
        kv.setValue(GENERATOR_LIMIT, 0);
        kv.setValue(GENERATOR_TIMEOUT, 60);
        kv.setValue(QUERY_TYPE_KEY, QUERY_TYPE);
        return kv;
    }

    public static JenaKeyValue createSystemParameters(){
        JenaKeyValue kv = new JenaKeyValue();
        kv.setValue(QUERY_TYPE_KEY, QUERY_TYPE);
        return kv;
    }



}
