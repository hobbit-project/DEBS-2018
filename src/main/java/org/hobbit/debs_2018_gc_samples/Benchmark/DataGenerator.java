package org.hobbit.debs_2018_gc_samples.Benchmark;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import org.hobbit.core.components.AbstractDataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeoutException;


/**
 * @author Pavel Smirnov
 */

public class DataGenerator extends AbstractDataGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DataGenerator.class);
    private final Map<String, Integer> sentIndexes = new HashMap<>();
    private final Map<String, String> encryptedTaskIds = new HashMap<>();
    private List<String> shipIds;
    Map<String, List<DataPoint>> pointsPerShip;
    public List<DataPoint> allPoints;

    Channel exchangeChannel;
    QueueingConsumer exchangeQueueConsumer;
    int recordsSent = 0;
    int recordsLimit = 0;
    long lastSendReportTime =0;

    public static final String ACKNOWLEDGE_QUEUE_NAME_KEY = "hobbit.sml2.ack";
    public static final String GENERATOR_LIMIT_KEY = "http://project-hobbit.eu/sml-benchmark-v2/generatorLimit";
    public static final Charset CHARSET = Charset.forName("UTF-8");

    @Override
    public void init() throws Exception {
        super.init();

        if(System.getenv().containsKey(GENERATOR_LIMIT_KEY))
           recordsLimit = Integer.parseInt(System.getenv().get(GENERATOR_LIMIT_KEY));


        logger.debug("Init(recordsLimit="+String.valueOf(recordsLimit)+")");

        String exchangeQueueName = this.generateSessionQueueName(ACKNOWLEDGE_QUEUE_NAME_KEY);

        exchangeChannel = this.cmdQueueFactory.getConnection().createChannel();
        exchangeChannel.queueDeclare(exchangeQueueName, false, false, true, null);

        exchangeQueueConsumer = new QueueingConsumer(exchangeChannel);
        exchangeChannel.basicConsume(exchangeQueueName, true, exchangeQueueConsumer);


        //String[] lines = readFile(Paths.get("data","debs2018_training_fixed_5.csv"));
        String[] lines = readFile(Paths.get("data","1000rowspublic_fixed.csv"));

        pointsPerShip = processLines(lines, recordsLimit);
        shipIds = new ArrayList<>(pointsPerShip.keySet());
    }


    public String[] readFile(Path filepath) throws IOException{
        logger.debug("Reading "+filepath);
        List<String> lines = Files.readAllLines(filepath, CHARSET);
        logger.debug("File reading finished");
        return lines.toArray(new String[0]);
    }

    public Map<String, List<DataPoint>> processLines(String[] lines, int linesLimit) throws ParseException {
        //for multiple threads sending
        Map<String, List<DataPoint>> ret = new LinkedHashMap<>();
        //for single thread sending
        allPoints = new ArrayList<>();

        String headLine = lines[0].replace("\uFEFF","").toLowerCase();
        String[] separators = new String[]{ "\t", ";", "," };
        int sepIndex = 0;
        String[] splitted = headLine.split(separators[sepIndex]);
        while(splitted.length==1){
            sepIndex++;
            splitted = headLine.split(separators[sepIndex]);
        }

        List<String> headings = Arrays.asList(splitted);
        String separator = separators[sepIndex];

        int limit = linesLimit;
        if(limit<=0)
            limit = lines.length;
        logger.debug("Processing {} lines", limit);

        for(int i=1; i<limit; i++){
            try {
                DataPoint point = new DataPoint(lines[i], headings, separator);

                String shipId = point.getValue("ship_id").toString();

                allPoints.add(point);

                List<DataPoint> shipPoints = new ArrayList<DataPoint>();
                if (ret.containsKey(shipId))
                    shipPoints = ret.get(shipId);
                shipPoints.add(point);
                ret.put(shipId, shipPoints);


            }
            catch (Exception e){
                logger.error(e.getMessage());
            }

        }
        logger.debug("Processing finished");
        return ret;
    }


    @Override
    public void generateData() throws Exception {
        logger.debug("generateData()");

        logger.debug("Start sending tuples("+pointsPerShip.size()+" ships)");

        Boolean stop = sendData(null);
        while (!stop){
            QueueingConsumer.Delivery delivery = exchangeQueueConsumer.nextDelivery();
            byte[] body = delivery.getBody();

            if(body.length>0){
                String encryptedTaskId = new String(body);
                if(encryptedTaskIds.containsKey(encryptedTaskId)) {
                    String shipId = encryptedTaskIds.get(encryptedTaskId);
                    encryptedTaskIds.remove(encryptedTaskId);
                    stop = sendData(shipId);
                }

            }
        }
        logger.debug("Finished after {} tuples sent", recordsSent);
        String test="";
    }

    private Boolean sendData(String shipId) throws Exception {
        if (recordsSent==allPoints.size())
            return true;
        //Boolean ret = sendSequental();
        Boolean ret = sendParallel(shipId);

        if(new Date().getTime()- lastSendReportTime>1000) {
            logger.debug(String.valueOf(recordsSent) + " tuples sent...");
            lastSendReportTime = new Date().getTime();
        }
        return ret;
    }

    private Boolean sendSequental() throws Exception {

        DataPoint dataPoint = allPoints.get(recordsSent);
        return sendPoint(dataPoint);
    }

    private Boolean sendPoint(DataPoint dataPoint) throws Exception {
        if (recordsSent==allPoints.size())
            return true;

        String taskId = "task_"+String.valueOf(recordsSent);
        dataPoint.setValue("taskId",taskId);

        String stringToSend = dataPoint.toJSONString();
        logger.trace("Sending to taskgen:" + stringToSend);
        sendDataToTaskGenerator(stringToSend.getBytes());

        String encryptedTaskId = encryptString(taskId);
        String shipId = dataPoint.getValue("ship_id").toString();
        encryptedTaskIds.put(encryptedTaskId, shipId);
        recordsSent++;

        return false;
    }

    private Boolean sendParallel(String signleShipId) throws Exception {
        Boolean ret = false;
        String[] shipIdsToSend = pointsPerShip.keySet().toArray(new String[0]);

        if(signleShipId!=null)
            shipIdsToSend = new String[]{ signleShipId };

        for(String shipId : shipIdsToSend){
            int sentIndex = 0;
            if(sentIndexes.containsKey(shipId))
                sentIndex = sentIndexes.get(shipId);

            if(sentIndex < pointsPerShip.get(shipId).size()){
                DataPoint dataPoint = pointsPerShip.get(shipId).get(sentIndex);
                ret = sendPoint(dataPoint);
                sentIndexes.put(shipId, sentIndex + 1);
            }
        }
        return ret;
    }

    public static String encryptString(String string){
        return string+"_encrypted";
    }

    @Override
    public void close() throws IOException {
        logger.debug("close()");
        try {
            exchangeChannel.close();
        } catch (TimeoutException e) {
            logger.error(e.getMessage());
        }
        // Always close the super class after yours!
        super.close();
    }

}