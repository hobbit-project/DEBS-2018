package org.hobbit.debs_2018_gc_samples.System;

import org.hobbit.core.components.AbstractSystemAdapter;
import org.hobbit.sdk.JenaKeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.hobbit.debs_2018_gc_samples.Benchmark.Constants.*;


public class SystemAdapter extends AbstractSystemAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SystemAdapter.class);
    private static JenaKeyValue parameters;
    private int queryType = -1;

    @Override
    public void init() throws Exception {
        super.init();
        logger.debug("Init()");
        // Your initialization code comes here...
        parameters = new JenaKeyValue.Builder().buildFrom(systemParamModel);
        logger.debug("SystemModel: "+parameters.encodeToString());

        queryType = parameters.getIntValueFor(QUERY_TYPE_KEY);

        if(queryType<=0){
            Exception ex = new Exception("Query type is not specified correctly");
            logger.error(ex.getMessage());
            throw ex;
        }

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

        String[] splitted = input.split(",");
        String result = splitted[8];  //port name
        if(queryType==2)
            result = result+","+splitted[7];  // port name,arrival time
        try {
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

