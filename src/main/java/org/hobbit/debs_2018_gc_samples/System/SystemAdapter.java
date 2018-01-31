package org.hobbit.debs_2018_gc_samples.System;

import org.hobbit.core.components.AbstractSystemAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class SystemAdapter extends AbstractSystemAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SystemAdapter.class);

    @Override
    public void init() throws Exception {
        super.init();
        logger.debug("Init()");
        // Your initialization code comes here...

        // You can access the RDF model this.systemParamModel to retrieve meta data about this system adapter
    }

    @Override
    public void receiveGeneratedData(byte[] data) {
        // handle the incoming data as described in the benchmark description
        logger.trace("receiveGeneratedData("+new String(data)+"): "+new String(data));
    }

    @Override
    public void receiveGeneratedTask(String taskId, byte[] data) {
        // handle the incoming task and create a result
        String input = new String(data);
        logger.trace("receiveGeneratedTask({})->{}",taskId, input);

        try {
            // Send the result to the evaluation storage
            String result = "somePredictionResult_"+input.substring(0, 15);
            logger.trace("sendResultToEvalStorage({})->{}", taskId, result);
            sendResultToEvalStorage(taskId, result.getBytes());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }



    @Override
    public void close() throws IOException {
        // Free the resources you requested here
        logger.debug("close()");

        // Always close the super class after yours!
        super.close();
    }

}

