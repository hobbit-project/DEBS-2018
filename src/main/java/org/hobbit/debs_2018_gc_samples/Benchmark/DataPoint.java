package org.hobbit.debs_2018_gc_samples.Benchmark;


import org.hobbit.sdk.KeyValue;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

public class DataPoint extends KeyValue {

    public DataPoint(String string, List<String> headings, String separator){
        super();

        List<String> splitted = Arrays.asList(string.split(separator));
        setValue("ship_id", splitted.get(0));
        setValue("departure_port_name", splitted.get(headings.indexOf("departure_port_name")));
        setValue("timestamp", splitted.get(headings.indexOf("timestamp")));


        String[] valsToExcludeFromRaw = new String[]{ "trip_id", "arrival_calc", "arrival_port_calc" };
        int firstValIndex = 99999;
        for(String valToTake : valsToExcludeFromRaw){
            String value = null;
            if (headings.indexOf(valToTake)>=0 && headings.indexOf(valToTake) < splitted.size() && headings.indexOf(valToTake) < headings.size() ) {
                value = splitted.get(headings.indexOf(valToTake));
                if(headings.indexOf(valToTake)<firstValIndex)
                    firstValIndex = headings.indexOf(valToTake);
            }
            setValue(valToTake, value);
        }
        String rawValue = String.join(separator, splitted.subList(0, Math.min(splitted.size(), firstValIndex)));
        setValue("raw", rawValue);
        rawValue=null;

    }

    public String get(String propertyName){
        try {
            if(getValue(propertyName)!=null)
                return getValue(propertyName).toString();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
