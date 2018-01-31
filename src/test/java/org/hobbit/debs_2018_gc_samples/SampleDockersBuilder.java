package org.hobbit.debs_2018_gc_samples;

import org.hobbit.sdk.docker.builders.common.DynamicDockerFileBuilder;


/**
 * @author Pavel Smirnov
 */

public class SampleDockersBuilder extends DynamicDockerFileBuilder {
    //public static String ACKNOWLEDGE_QUEUE_NAME_KEY = "hobbit.sml2.ack";
    public static String GIT_REPO_PATH = "git.project-hobbit.eu:4567/yourname/";
    //public static String GIT_REPO_PATH = "";
    public static String PROJECT_NAME = "yourprojectname/";

    public static final String SYSTEM_IMAGE_NAME = GIT_REPO_PATH+ PROJECT_NAME +"system-adapter";
    public static final String SYSTEM_URI = "http://project-hobbit.eu/sdk-dummy-system/";

    public SampleDockersBuilder(Class runnerClass) throws Exception {
        super("SampleDockersBuilder");
        jarFilePath("target/debs_2018_gc_sample_system-1.0.jar");
        buildDirectory(".");
        dockerWorkDir("/usr/src/"+ PROJECT_NAME);
        containerName(runnerClass.getSimpleName());
        runnerClass(org.hobbit.core.run.ComponentStarter.class, runnerClass);
    }

}
