package com.cleanroommc.flare.api.metadata;

import com.google.gson.JsonElement;

import java.util.Map;
import java.util.TreeMap;

/**
 * Function to export dynamic metadata to be displayed within the spark viewer.
 */
@FunctionalInterface
public interface MetadataProvider {

    /**
     * Produces a map of the metadata.
     *
     * @return the metadata
     */
    Map<String, JsonElement> get();

    default Map<String, String> export() {
        Map<String, String> map = new TreeMap<>();
        get().forEach((key, value) -> map.put(key, value.toString()));
        return map;
    }

}
