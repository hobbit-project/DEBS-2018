package org.hobbit.debs_2018_gc_samples;

import org.hobbit.core.components.Component;
import org.hobbit.debs_2018_gc_samples.Benchmark.DataGenerator;
import org.hobbit.debs_2018_gc_samples.Benchmark.EvalStorage;
import org.hobbit.debs_2018_gc_samples.System.SystemAdapter;
import org.hobbit.sdk.ComponentsExecutor;
import org.hobbit.sdk.EnvironmentVariablesWrapper;
import org.hobbit.sdk.JenaKeyValue;
import org.hobbit.sdk.docker.AbstractDockerizer;
import org.hobbit.sdk.docker.RabbitMqDockerizer;
import org.hobbit.sdk.docker.builders.*;
import org.hobbit.sdk.docker.builders.common.PullBasedDockersBuilder;
import org.hobbit.sdk.utils.CommandQueueListener;
import org.hobbit.sdk.utils.commandreactions.MultipleCommandsReaction;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;

import static org.hobbit.sdk.CommonConstants.*;
import static org.hobbit.debs_2018_gc_samples.SampleDockersBuilder.*;

/**
 * @author Pavel Smirnov
 */

public class SampleSystemTest extends EnvironmentVariablesWrapper {

    private RabbitMqDockerizer rabbitMqDockerizer;
    private ComponentsExecutor componentsExecutor;
    private CommandQueueListener commandQueueListener;

    //Should not be changed, because there images will be called from benchmark-controller
    String benchmarkImageName = "git.project-hobbit.eu:4567/debs_2018_gc/benchmark-controller";
    String dataGeneratorImageName = "git.project-hobbit.eu:4567/debs_2018_gc/data-generator";
    String taskGeneratorImageName = "git.project-hobbit.eu:4567/debs_2018_gc/task-generator";
    String evalStorageImageName = "git.project-hobbit.eu:4567/debs_2018_gc/eval-storage";
    String evalModuleImageName = "git.project-hobbit.eu:4567/debs_2018_gc/eval-module";

    SystemAdapterDockerBuilder systemAdapterBuilder;


    Component benchmarkController;
    Component dataGen;
    Component taskGen;
    Component evalStorage;
    Component evalModule;
    Component systemAdapter;

    public void init(Boolean useCachedImages) throws Exception {

        rabbitMqDockerizer = RabbitMqDockerizer.builder().build();

        setupCommunicationEnvironmentVariables(rabbitMqDockerizer.getHostName(), "session_"+String.valueOf(new Date().getTime()));
        setupBenchmarkEnvironmentVariables(EXPERIMENT_URI, createBenchmarkParameters());
        setupGeneratorEnvironmentVariables(1,1);
        setupSystemEnvironmentVariables(SYSTEM_URI, createSystemParameters());


        systemAdapterBuilder = new SystemAdapterDockerBuilder(new SampleDockersBuilder(SystemAdapter.class).imageName(SYSTEM_IMAGE_NAME).useCachedImage(useCachedImages).init());

        benchmarkController = new BenchmarkDockerBuilder(new PullBasedDockersBuilder(benchmarkImageName)).build();
        dataGen = new DataGenerator();
        taskGen = new TaskGenDockerBuilder(new PullBasedDockersBuilder(taskGeneratorImageName)).build();

        evalStorage = new EvalStorage();
        evalModule = new EvalModuleDockerBuilder(new PullBasedDockersBuilder(evalModuleImageName)).build();

        //Here you can switch between pure java code (new SystemAdapter())
        //systemAdapter = new SystemAdapter();
        systemAdapter = systemAdapterBuilder.build();
    }

    @Test
    @Ignore
    public void buildImages() throws Exception {
        init(false);
        ((AbstractDockerizer)systemAdapter).prepareImage();
    }

    @Test
    public void checkHealth() throws Exception {

        Boolean useCachedImages = true;

        init(useCachedImages);

        commandQueueListener = new CommandQueueListener();
        componentsExecutor = new ComponentsExecutor(commandQueueListener, environmentVariables);

        rabbitMqDockerizer.run();

        commandQueueListener.setCommandReactions(
                new MultipleCommandsReaction(componentsExecutor, commandQueueListener)
                        .dataGenerator(dataGen).dataGeneratorImageName(dataGeneratorImageName)
                        .taskGenerator(taskGen).taskGeneratorImageName(taskGeneratorImageName)
                        .evalStorage(evalStorage).evalStorageImageName(evalStorageImageName)
                        .evalModule(evalModule).evalModuleImageName(evalModuleImageName)
                        .systemContainerId(systemAdapterBuilder.getImageName())
        );

        componentsExecutor.submit(commandQueueListener);
        commandQueueListener.waitForInitialisation();

        componentsExecutor.submit(benchmarkController);

        componentsExecutor.submit(systemAdapter, systemAdapterBuilder.getImageName());

        commandQueueListener.waitForTermination();
        commandQueueListener.terminate();
        componentsExecutor.shutdown();

        rabbitMqDockerizer.stop();

        Assert.assertFalse(componentsExecutor.anyExceptions());
    }



    public JenaKeyValue createBenchmarkParameters(){
        JenaKeyValue kv = new JenaKeyValue(EXPERIMENT_URI);
        //kv.setValue(GENERATOR_LIMIT_KEY, 123);
        return kv;
    }

    public JenaKeyValue createSystemParameters(){
        JenaKeyValue kv = new JenaKeyValue();
        kv.setValue(SYSTEM_URI+"systemParam1", 123);
        return kv;
    }


}
