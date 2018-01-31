package org.hobbit.debs_2018_gc_samples.Benchmark;


import org.hobbit.sdk.KeyValue;

import java.text.ParseException;
import java.util.List;

public class DataPoint extends KeyValue {

    public DataPoint(String string, List<String> headings, String separator) throws ParseException {
        super();

        String[] splitted = string.split(separator);
        setValue("ship_id", splitted[0]);
        setValue("departure_port_name", splitted[headings.indexOf("departure_port_name")]);
        setValue("raw", string);

    }

    public String get(String propertyName){
        try {
            return getValue(propertyName).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
