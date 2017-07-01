package com.swisscom.fabric.config;

/**
 * Created by shaun on 13.01.17.
 */

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class ApplicationStatus extends JsonObject {
    @JsonProperty("errors")
    private final TreeMap<String, String> errorMap=new TreeMap<>();

    @JsonProperty
    private final TreeSet<String> checked=new TreeSet<>();

    public void addError(String label, String error) {
        checked.add(label);
        if(error!=null) {
            errorMap.put(label, error);
        }
    }
    public boolean hasErrors() {
        return errorMap.size()>0;
    }

    public Map<String, String> getErrorMap() {
        return Collections.unmodifiableMap(errorMap);
    }
}