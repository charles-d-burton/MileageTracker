package com.charles.mileagetracker.app.cache;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by charles on 6/11/14.
 */
public class TripSegments implements Serializable {
    public TripSegments() {}

    private HashMap<Integer, HashMap<String, Object>> tripMap = new HashMap<Integer, HashMap<String, Object>>();

}
