package org.hobbit.debs_2018_gc_samples.Benchmark;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import org.hobbit.core.components.AbstractTaskGenerator;
import org.hobbit.core.rabbit.DataSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.hobbit.debs_2018_gc_samples.Benchmark.Constants.*;



/**
 * @author Pavel Smirnov
 */

public class TaskGenerator extends AbstractTaskGenerator {
    private static final Logger logger = LoggerFactory.getLogger(TaskGenerator.class);

    private final Boolean sequental = false;
    private final Map<String, Integer> pointIndexes = new HashMap<>();
    private final Map<String, Integer> tripIndexes = new HashMap<>();

    private final Map<String, Integer> shipTuplesCount = new HashMap<>();
    private final Map<String, String> taskShipMap = new HashMap<>();
    private Map<String, List<List<DataPoint>>> shipTrips;

    private List<String> shipsToSend = new ArrayList<>();

    int queryType = -1;
    int generationTimeout = 60;
    public List<DataPoint> allPoints;

    long allPointsCount=0;
    Channel evalStorageTaskGenChannel;
    QueueingConsumer exchangeQueueConsumer;
    int recordsSent = 0;
    int recordsLimit = 0;
    long lastReportedValue = 0;
    //long expectationsToStorageTime =0;
    long valDiff = 0;
    long expectationsSent = 0;
    String[] shipIdsToSend;

    String encryptionKey="encryptionKey";
    String tupleName="tuples";
    Timer timer;
    int timerPeriodSeconds = 5;
    ExecutorService threadPool;
    Callable <String> executionLoop;

    Boolean gerenationFinished = false;

    @Override
    public void init() throws Exception {
        super.init();
        logger.debug("Init()");

        if(System.getenv().containsKey(ENCRYPTION_KEY_NAME))
            encryptionKey = System.getenv().get(ENCRYPTION_KEY_NAME);

        if(System.getenv().containsKey("GENERATOR_LIMIT")) {
            logger.debug("GENERATOR_LIMIT={}",System.getenv().get("GENERATOR_LIMIT"));
            recordsLimit = Integer.parseInt(System.getenv().get("GENERATOR_LIMIT"));
        }

        if(System.getenv().containsKey("GENERATOR_TIMEOUT")){
            logger.debug("GENERATOR_TIMEOUT={}",System.getenv().get("GENERATOR_TIMEOUT"));
            int newTimeout = Integer.parseInt(System.getenv().get("GENERATOR_TIMEOUT"));
            if(newTimeout>0)
                generationTimeout = newTimeout;
        }

        if(System.getenv().containsKey("QUERY_TYPE"))
            queryType = Integer.parseInt(System.getenv().get("QUERY_TYPE"));


        if(queryType<=0){
            Exception ex = new Exception("Query type is not specified correctly");
            logger.error(ex.getMessage());
            throw ex;
        }

        int dataGeneratorId = getGeneratorId();
        int numberOfGenerators = getNumberOfGenerators();

        logger.debug("Init (queryType={}, recordsLimit={}, timeout={}",dataGeneratorId, String.valueOf(recordsLimit), String.valueOf(generationTimeout)+")");

        String exchangeQueueName = this.generateSessionQueueName(ACKNOWLEDGE_QUEUE_NAME);

        evalStorageTaskGenChannel = this.cmdQueueFactory.getConnection().createChannel();
        evalStorageTaskGenChannel.queueDeclare(exchangeQueueName, false, false, true, null);

        exchangeQueueConsumer = new QueueingConsumer(evalStorageTaskGenChannel);
        evalStorageTaskGenChannel.basicConsume(exchangeQueueName, true, exchangeQueueConsumer);

        threadPool = Executors.newCachedThreadPool();
        executionLoop = ()->{
            Boolean stop = sendData(null);
            while (!stop){
                QueueingConsumer.Delivery delivery = exchangeQueueConsumer.nextDelivery();
                byte[] body = delivery.getBody();
                if (body.length > 0){
                    String encryptedTaskId = new String(body);
                    if (taskShipMap.containsKey(encryptedTaskId)){
                        String shipId = taskShipMap.get(encryptedTaskId);
                        taskShipMap.remove(encryptedTaskId);
                        try {
                            stop = sendData(shipId);
                        }
                        catch (Exception e){
                            logger.error("Failed to send data: {}", e.getMessage());
                        }
                    }

                }
            }
            timer.cancel();
            return "";
        };

        initData();

    }

    private void initData() throws Exception {

        Utils utils = new Utils(this.logger);

        String[] lines = utils.readFile(Paths.get("data","debs2018_training_labeled.csv"), recordsLimit);
        shipTrips = utils.getTripsPerShips(lines);


        for(String shipId : shipTrips.keySet()){
            shipsToSend.add(shipId);
            List<DataPoint> shipPoints = shipTrips.get(shipId).stream().flatMap(l-> l.stream()).collect(Collectors.toList());
            allPointsCount+=shipPoints.size();
            shipTuplesCount.put(shipId, shipPoints.size());
            if(sequental)
                allPoints.addAll(shipPoints);
        }
    }

    @Override
    public void generateTask(byte[] data) throws Exception {

        logger.debug("generateTask()");
        if(gerenationFinished)
            return;

        gerenationFinished = true;

        startTimer();

        long started = new Date().getTime();

        logger.debug("Start sending tuples(" + shipsToSend.size() + " ships)");
        Future<String> future = threadPool.submit(executionLoop);
        try {
            future.get(generationTimeout, TimeUnit.MINUTES);
       } catch (ExecutionException e) {
            logger.error("RuntimeException: {}", e.getMessage());
            e.getCause();
            System.exit(1);
        } catch (InterruptedException e) {
            logger.error("InterruptedException: {}", e.getMessage());
            Thread.currentThread().interrupt();
            e.getCause();
            System.exit(1);
        } catch (TimeoutException e) {
            Exception e2 = new Exception("Timeout exception: "+ generationTimeout +" min");
            logger.error(e2.getMessage());
            System.exit(1);
        }
        threadPool.shutdown();
        timer.cancel();

        double took = (new Date().getTime() - started)/1000.0;
        logger.debug("Finished after {} tuples sent. Took {} s. Avg: {} tuples/s", recordsSent, took, Math.round(recordsSent/took));

    }


    private void startTimer(){
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                valDiff = (recordsSent - lastReportedValue)/timerPeriodSeconds;
                logger.debug("{} {} sent. Curr: {} tuples/s",tupleName, recordsSent, valDiff);
                lastReportedValue = recordsSent;
            }
        }, 1000, timerPeriodSeconds*1000);
    }

    private Boolean sendData(String shipId) throws Exception {
        Boolean stop = false;
        if(sequental)
            sendSequental();
        else
            sendParallel(shipId);

        if (shipsToSend.size()==0)
            stop=true;

        return stop;
    }

    private void sendSequental() throws Exception {
        DataPoint dataPoint = allPoints.get(recordsSent);
        sendPoint(dataPoint, "groupId", recordsSent);
    }

    private void sendParallel(String signleShipId){

        shipIdsToSend = (signleShipId!=null ? new String[]{ signleShipId }: shipsToSend.toArray(new String[0]));

        for(String shipId : shipIdsToSend){

            try {
                int tripIndex = (tripIndexes.containsKey(shipId)?tripIndexes.get(shipId):0);
                int pointIndex = (pointIndexes.containsKey(shipId)? pointIndexes.get(shipId):0);

                String encryptedTaskId = null;
                while (encryptedTaskId==null && tripIndex < shipTrips.get(shipId).size()){
                    DataPoint dataPoint = shipTrips.get(shipId).get(tripIndex).get(pointIndex);

                    try {
                        encryptedTaskId = sendPoint(dataPoint, shipId + "_" + tripIndex, pointIndex);
                        if(encryptedTaskId==null)
                            pointIndex=99999999; //switch to the next trip of the ship
                    }
                    catch (Exception e){
                        logger.error("Problem with sendPoint(): "+e.getMessage());
                    }

                    pointIndex++;

                    //if all point of the trip have been sent off switch to the next trip
                    if (pointIndex >= shipTrips.get(shipId).get(tripIndex).size()) {
                        tripIndex++;
                        pointIndex=0;
                    }
                }

                //finish without waiting the last notification
                if (tripIndex >= shipTrips.get(shipId).size())
                    shipsToSend.remove(shipId);


                tripIndexes.put(shipId, tripIndex);
                pointIndexes.put(shipId, pointIndex);

            }
            catch (Exception e){
                logger.error("Problem with sendParallel(): "+e.getMessage());
            }
        }

    }

    private String sendPoint(DataPoint dataPoint, String tripId, int orderingIndex) throws Exception {

        String taskId = "task_"+String.valueOf(recordsSent);
        String shipId = dataPoint.getValue("ship_id").toString();
        String raw = dataPoint.getStringValueFor("raw");

        String sendToSystem = raw;
        logger.trace("sendTaskToSystemAdapter({})->{}", taskId, sendToSystem);

        String label = dataPoint.get("arrival_port_calc");
        String ret = null;
        if(label!=null) {
            long sentTimestamp = System.currentTimeMillis();
            sendTaskToSystemAdapter(taskId, sendToSystem.getBytes());

            String encryptedTaskId = Utils.encryptString(taskId, encryptionKey);
            taskShipMap.put(encryptedTaskId, shipId);

            String timestamp = dataPoint.get("timestamp");


            if (queryType == 2)
                label += "," + dataPoint.get("arrival_calc");

            sendExpectation(encryptedTaskId, sentTimestamp, tripId, orderingIndex, timestamp, label);
            recordsSent++;
            ret = encryptedTaskId;
        }


        return ret;
    }



    private void sendExpectation(String encTaskId, long taskSentTimestamp, String tripId, int orderingIndex, String tupleTimestamp, String label) throws IOException {
        String sendToStorage = (queryType == 1 ? tripId+","+orderingIndex+ ","+ tupleTimestamp+",": "")  + label;
        //String sendToStorage = (queryType == 1 ? totalTaskIndex+ ","+ totalTripIndex+","+tupleAward: tripDuration) + "," + label;
        logger.trace("sendTaskToEvalStorage({})=>{}", encTaskId, sendToStorage.getBytes());
        sendTaskToEvalStorage(encTaskId, taskSentTimestamp, sendToStorage.getBytes());
    }

}