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
    private final Map<String, String> taskLabels = new HashMap<>();
    private final Map<String, String> tupleTimestamps = new HashMap<>();
    private final Map<String, String> taskShipMap = new HashMap<>();
    private final Map<String, List<List<String>>> shipTasks = new HashMap<>();
    private Map<String, List<List<DataPoint>>> shipTrips;
    private final Map<String, Long> taskSentTimestamps = new HashMap<>();

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

    long expectationsSent = 0;
    String encryptionKey="encryptionKey";
    String tupleName="tuples";
    Timer timer;
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
            return "";
        };

    }

    private void initData() throws Exception {

        Utils.logger = this.logger;
        //String[] lines = Utils.readFile(Paths.get("data","1000rowspublic_fixed.csv"), recordsLimit);
        String[] lines = Utils.readFile(Paths.get("data","debs2018_training_labeled.csv"), recordsLimit);

        shipTrips = Utils.getTripsPerShips(lines);

        for(String shipId : shipTrips.keySet()){
            shipsToSend.add(shipId);
            List<DataPoint> shipPoints = shipTrips.get(shipId).stream().flatMap(l-> l.stream()).collect(Collectors.toList());
            allPointsCount+=shipPoints.size();
            shipTuplesCount.put(shipId, shipPoints.size());
            shipTasks.put(shipId, new ArrayList<>());
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

        initData();

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
                long valDiff = recordsSent - lastReportedValue;
                logger.debug("{} {} sent. Curr: {} tuples/s",tupleName, recordsSent, valDiff);
                lastReportedValue = recordsSent;
            }
        }, 1000, 1000);
    }

    private Boolean sendData(String shipId) throws Exception {
        Boolean ret = false;
        if(sequental)
            sendSequental();
        else
            sendParallel(shipId);

        if (shipsToSend.size()==0)
            ret=true;

        return ret;
    }

    private void sendSequental() throws Exception {
        DataPoint dataPoint = allPoints.get(recordsSent);
        sendPoint(dataPoint, "groupId", recordsSent);
    }

    private void sendParallel(String signleShipId) throws Exception {

        String[] shipIdsToSend = (signleShipId!=null ? new String[]{ signleShipId }: shipsToSend.toArray(new String[0]));

        int shipIndex=0;
        for(String shipId : shipIdsToSend){
            int tripIndex = 0;
            if(tripIndexes.containsKey(shipId))
                tripIndex = tripIndexes.get(shipId);

            int pointIndex = 0;
            if(pointIndexes.containsKey(shipId))
                pointIndex = pointIndexes.get(shipId);

            if(pointIndex == shipTrips.get(shipId).get(tripIndex).size()){
                tripIndex++;
                tripIndexes.put(shipId, tripIndex);
                pointIndex=0;
            }


            if(tripIndex < shipTrips.get(shipId).size()){
                DataPoint dataPoint = shipTrips.get(shipId).get(tripIndex).get(pointIndex);
                String encryptedTaskId = sendPoint(dataPoint, shipId+"_"+tripIndex, pointIndex);

                List<String> tripTasks = new ArrayList<>();
                if(shipTasks.get(shipId).size()==tripIndex)
                    shipTasks.get(shipId).add(tripTasks);

                tripTasks = shipTasks.get(shipId).get(tripIndex);
                tripTasks.add(encryptedTaskId);

                pointIndex++;
                pointIndexes.put(shipId, pointIndex);
            }else{
                shipsToSend.remove(shipId);
            }
            shipIndex++;
        }

    }

    private String sendPoint(DataPoint dataPoint, String tripId, int orderingIndex) throws Exception {

        String taskId = "task_"+String.valueOf(recordsSent);
        String shipId = dataPoint.getValue("ship_id").toString();
        String raw = dataPoint.getStringValueFor("raw");

        String sendToSystem = raw;
        logger.trace("sendTaskToSystemAdapter({})->{}", taskId, sendToSystem);
        long sentTimestamp = System.currentTimeMillis();
        sendTaskToSystemAdapter(taskId, sendToSystem.getBytes());

        String encryptedTaskId = Utils.encryptString(taskId, encryptionKey);
        taskShipMap.put(encryptedTaskId, shipId);
        taskSentTimestamps.put(encryptedTaskId, sentTimestamp);

        String label = dataPoint.get("arrival_port_calc");
        String timestamp = dataPoint.get("timestamp");

        if(label!=null) {
            if (queryType == 2)
                label += "," + dataPoint.get("arrival_calc");
            taskLabels.put(encryptedTaskId, label);
            tupleTimestamps.put(encryptedTaskId, timestamp);
        }

        sendExpectation(encryptedTaskId, sentTimestamp, tripId, orderingIndex, timestamp, label);

        recordsSent++;
        return encryptedTaskId;
    }

    private void sendExpectation(String encTaskId, long taskSentTimestamp, String tripId, int orderingIndex, String tupleTimestamp, String label) throws IOException {
        String sendToStorage = (queryType == 1 ? tripId+","+orderingIndex+ ","+ tupleTimestamp+",": "")  + label;
        logger.trace("sendTaskToEvalStorage({})=>{}", encTaskId, sendToStorage.getBytes());
        sendTaskToEvalStorage(encTaskId, taskSentTimestamp, sendToStorage.getBytes());
    }

}