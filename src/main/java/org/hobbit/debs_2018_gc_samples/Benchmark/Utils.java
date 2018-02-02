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

import static org.hobbit.debs_2018_gc_samples.Benchmark.DataGenerator.CHARSET;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(DataGenerator.class);

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

    public static Map<String, List<DataPoint>> processLines(String[] lines) throws IOException, ParseException {
        //for multiple threads sending
        Map<String, List<DataPoint>> ret = new LinkedHashMap<>();

        Map<String, Map> uniqueDeptsPerShip = new HashMap<>();

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

//        for(int i=0; i<headings.length; i++)
//            headings[i]=headings[i].replace("_","");


        logger.debug("Processing {} lines", lines.length);

        for(int i=1; i<lines.length; i++){
            try {
                DataPoint point = new DataPoint(lines[i], headings, separator);
                //DataPoint point = null;
                String shipId = point.getValue("ship_id").toString();
                //String shipId = point.getValue("vessel_id").toString();

                List<DataPoint> shipPoints = new ArrayList<DataPoint>();
                if (ret.containsKey(shipId))
                    shipPoints = ret.get(shipId);
                shipPoints.add(point);
                ret.put(shipId, shipPoints);

                Map shipDepartures = new HashMap();
                if (uniqueDeptsPerShip.containsKey(shipId))
                    shipDepartures = uniqueDeptsPerShip.get(shipId);
                String depPortName = point.getValue("departure_port_name").toString();
                shipDepartures.put(depPortName, null);
                uniqueDeptsPerShip.put(shipId, shipDepartures);
            }
            catch (Exception e){
                logger.error(e.getMessage());
            }

        }
        logger.debug("Processing finished");
        return ret;
    }


    public static String encryptString(String string, String encryptionKey){
        return string+"_"+encryptionKey;
    }
}
