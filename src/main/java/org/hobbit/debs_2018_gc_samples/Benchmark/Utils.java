package org.hobbit.debs_2018_gc_samples.Benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;

import static org.hobbit.debs_2018_gc_samples.Constants.CHARSET;


public class Utils {
    public Logger logger = LoggerFactory.getLogger(Utils.class);

    public Utils(Logger logger){
        this.logger = logger;
    }

    public String[] readFile(Path filepath, int linesLimit) throws IOException {
        logger.debug("Reading "+filepath);

        //URL url  = Resources.getResource(filepath.toString());
        //List<String> lines = Resources.readLines(url,CHARSET);


        List<String> lines = Files.readAllLines(filepath, CHARSET);
        if(linesLimit>0 && lines.size()>linesLimit)
            lines = lines.subList(0, linesLimit+1);
        logger.debug("File reading finished ({})",lines.size());
        return lines.toArray(new String[0]);
    }

    public Map<String, Map<String, List<DataPoint>>> getTripsPerShips(String[] lines) throws Exception {
        logger.debug("Processing {} lines", lines.length);
        Map<String, Map<String, List<DataPoint>>> ret = new LinkedHashMap<>();

        Map<String, List<String>> destsPerShip = new HashMap<>();

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

        int tuplesCount=0;
        for(int i=1; i<lines.length; i++){
            //try {
            DataPoint point = new DataPoint(lines[i], headings, separator);
            String shipId = point.getValue("ship_id").toString();

            Map<String, List<DataPoint>> shipTrips = new LinkedHashMap<String, List<DataPoint>>();
            List<DataPoint> lastTrip = new ArrayList<>();
            String lastTripId = "";
            if (ret.containsKey(shipId)) {
                shipTrips = ret.get(shipId);
                lastTripId = shipTrips.keySet().toArray(new String[0])[shipTrips.size() - 1];
                lastTrip = shipTrips.get(lastTripId);
            }

            String tripId = point.get("trip_id");
            List<DataPoint> tripToAddPoint = lastTrip;
            if(!tripId.equals(lastTripId)){
                tripToAddPoint = new ArrayList<>();
            }
            tripToAddPoint.add(point);
            shipTrips.put(tripId, tripToAddPoint);

            ret.put(shipId, shipTrips);
            tuplesCount++;
        }

        int tripsCount = ret.values().stream().map(list->list.size()).mapToInt(Integer::intValue).sum();
        logger.debug("Processing finished: {} tuples, {} trips", tuplesCount, tripsCount);
        return ret;
    }
    public static String encryptString(String string, String encryptionKey){
        return string;
    }
}
