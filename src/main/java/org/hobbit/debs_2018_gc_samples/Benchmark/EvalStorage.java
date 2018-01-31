package org.hobbit.debs_2018_gc_samples.Benchmark;

import com.rabbitmq.client.Channel;
import org.hobbit.core.components.AbstractEvaluationStorage;
import org.hobbit.core.data.Result;
import org.hobbit.core.data.ResultPair;
import org.hobbit.sdk.ResultPairImpl;
import org.hobbit.sdk.SerializableResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;


/**
 * @author Pavel Smirnov
 */

public class EvalStorage extends AbstractEvaluationStorage {
    private static final Logger logger = LoggerFactory.getLogger(EvalStorage.class);
    protected Exception exception;

    private String exchangeQueueName;

    private final List<Result> actualResponses = new ArrayList<>();
    private final List<Result> expectedResponses = new ArrayList<>();

    private Channel exchangeChannel;


    @Override
    public void init() throws Exception {
        super.init();
        logger.debug("Init()");

        exchangeQueueName = this.generateSessionQueueName(DataGenerator.ACKNOWLEDGE_QUEUE_NAME_KEY);
        exchangeChannel = this.cmdQueueFactory.getConnection().createChannel();
        exchangeChannel.queueDeclare(exchangeQueueName, false, false, true, null);
    }

    @Override
    public void receiveExpectedResponseData(String taskId, long timestamp, byte[] result) {
        String resultStr = new String(result);
        logger.trace("receiveExpectedResponseData()->{}", resultStr);
        int actualSize = result.length / 1024;
        if(!taskId.equals(""))
            expectedResponses.add(new SerializableResult(timestamp,result));
    }

    @Override
    public void receiveResponseData(String taskId, long timestamp, byte[] bytes) {
        int actualSize = bytes.length / 1024;
        logger.trace("receiveResponseData()->{}",new String(bytes));
        actualResponses.add(new SerializableResult(timestamp,bytes));
        String encryptedTaskId = DataGenerator.encryptString(taskId);
        try {
            notifyGenerator(encryptedTaskId);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void notifyGenerator(String encryptedTaskId) throws IOException {
        logger.trace("notifyGenerator({})", encryptedTaskId);
        exchangeChannel.basicPublish("", exchangeQueueName, null, encryptedTaskId.getBytes());
    }


    @Override
    protected Iterator<ResultPair> createIterator(){
        logger.debug("createIterator()");
        String test="123";

        List<ResultPair> ret = new ArrayList<>();
        for(int i = 0; i<expectedResponses.size(); i++){
            Result actual = (actualResponses.size()>i?actualResponses.get(i):null);
            ret.add(new ResultPairImpl(expectedResponses.get(i), actual));
        }

        return ret.iterator();
    }

    @Override
    public void close() throws IOException {

        try {
            exchangeChannel.close();
        } catch (TimeoutException e) {
            logger.error(e.getMessage());
        }
        super.close();
    }

}
