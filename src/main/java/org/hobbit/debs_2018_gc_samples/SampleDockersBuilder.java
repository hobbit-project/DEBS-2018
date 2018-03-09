package org.hobbit.debs_2018_gc_samples;

import org.hobbit.sdk.docker.builders.DynamicDockerFileBuilder;

import static org.hobbit.debs_2018_gc_samples.Benchmark.Constants.PROJECT_NAME;


/**
 * @author Pavel Smirnov
 */

public class SampleDockersBuilder extends DynamicDockerFileBuilder {


    public SampleDockersBuilder(Class runnerClass) throws Exception {
        super("SampleDockersBuilder");
        jarFilePath("target/debs_2018_gc_sample_system-1.0.jar");
        buildDirectory(".");
        dockerWorkDir("/usr/src/"+ PROJECT_NAME);
        containerName(runnerClass.getSimpleName());
        runnerClass(org.hobbit.core.run.ComponentStarter.class, runnerClass);
    }

}
