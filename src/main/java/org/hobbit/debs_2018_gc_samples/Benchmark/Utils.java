package org.hobbit.debs_2018_gc_samples.Benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static org.hobbit.debs_2018_gc_samples.Benchmark.Constants.CHARSET;


public class Utils {
    public static Logger logger = LoggerFactory.getLogger(Utils.class);

    public static String[] readFile(Path filepath, int linesLimit) throws IOException {
        logger.debug("Reading "+filepath);

        //URL url  = Resources.getResource(filepath.toString());
        //List<String> lines = Resources.readLines(url,CHARSET);


        List<String> lines = Files.readAllLines(filepath, CHARSET);
        logger.debug("File reading finished");
        if(linesLimit>0)
            lines = lines.subList(0, linesLimit+1);
        return lines.toArray(new String[0]);
    }

    public static Map<String, List<List<DataPoint>>> getTripsPerShips(String[] lines) throws IOException, ParseException {
        logger.debug("Processing {} lines", lines.length);
        Map<String, List<List<DataPoint>>> ret = new LinkedHashMap<>();

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

        Map<String,String> prevShipDepName= new HashMap<>();
        int tuplesCount=0;
        int tripsCount=0;
        for(int i=1; i<lines.length; i++){
            try {
                DataPoint point = new DataPoint(lines[i], headings, separator);
                //DataPoint point = null;
                String shipId = point.getValue("ship_id").toString();
                //String shipId = point.getValue("vessel_id").toString();

                List<List<DataPoint>> shipTrips = new ArrayList<List<DataPoint>>();
                if (ret.containsKey(shipId))
                    shipTrips = ret.get(shipId);

                String depPortName = point.getValue("departure_port_name").toString();

                List<DataPoint> lastTrip = new ArrayList<>();
                if(!prevShipDepName.containsKey(shipId) || !prevShipDepName.get(shipId).equals(depPortName))
                    shipTrips.add(lastTrip);
                else
                    lastTrip = shipTrips.get(shipTrips.size()-1);
                lastTrip.add(point);
                tuplesCount++;
                ret.put(shipId, shipTrips);

                List shipDepartures = new ArrayList();
                if (destsPerShip.containsKey(shipId)) {
                    shipDepartures = destsPerShip.get(shipId);
                    if(!shipDepartures.get(shipDepartures.size()-1).equals(depPortName)) {
                        shipDepartures.add(depPortName);
                        tripsCount++;
                    }
                }else{
                    shipDepartures.add(depPortName);
                    tripsCount++;
                }

                //shipDepartures.put(depPortName, null);
                destsPerShip.put(shipId, shipDepartures);
                prevShipDepName.put(shipId, depPortName);
            }
            catch (Exception e){
                logger.error(e.getMessage());
            }

        }
        logger.debug("Processing finished: {} tuples, {} trips", tuplesCount, tripsCount);
        return ret;
    }

    public static String encryptString(String string, String encryptionKey){
        return string;
    }
}
