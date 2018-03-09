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
        //setValue("raw", string);
        setValue("raw", String.join(separator, splitted.subList(0, Math.min(splitted.size()-1, 10))));

        String[] valsToTake = new String[]{ "arrival_calc", "arrival_port_calc" };

        for(String valToTake : valsToTake) {
            String value = null;
            if (headings.indexOf(valToTake) < splitted.size() && headings.indexOf(valToTake) < headings.size() )
                value = splitted.get(headings.indexOf(valToTake));
            setValue(valToTake, value);
        }

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
