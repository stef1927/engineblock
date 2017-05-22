/*
 *
 *    Copyright 2016 jshook
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * /
 */
package io.engineblock.extensions;

import io.engineblock.activityapi.cycletracking.TrackerDispenser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Convenient singleton for accessing all loadable CycleMarker instances via
 * CycleMarkerDispensers.
 */
public class CycleMarkerFinder {

    private static final Logger logger = LoggerFactory.getLogger(CycleMarkerFinder.class);
    private static CycleMarkerFinder instance;

    private final Map<String, TrackerDispenser> types = new ConcurrentHashMap<>();

    private CycleMarkerFinder() {
    }

    public synchronized static CycleMarkerFinder instance() {
        if (instance==null) {
            instance = new CycleMarkerFinder();
        }
        return instance;
    };

    /**
     * Return the named activity type, optionally.
     * @param markerType The canonical marker type name.
     * @return an optional ActivityType instance
     */
    public Optional<TrackerDispenser> get(String markerType) {
        markerType=markerType.split(",")[0]; // Strip any ,... from the front
        return Optional.ofNullable(getTypes().get(markerType));
    }

    /**
     * Return the named activity type or throw an error.
     * @param markerType The canonical marker type name.
     * @return an ActivityType instance
     * @throws RuntimeException if the activity type isn't found.
     */
    public TrackerDispenser getOrThrow(String markerType) {
        Optional<TrackerDispenser> at = Optional.ofNullable(getTypes().get(markerType));
        return at.orElseThrow(
                () -> new RuntimeException("CycleMarkerType '" + markerType + "' not found. Available types:" +
                this.getTypes().keySet().stream().collect(Collectors.joining(",")))
        );
    }

    private synchronized Map<String, TrackerDispenser> getTypes() {
        if (types.size()==0) {
            ClassLoader cl = getClass().getClassLoader();
            logger.debug("loading CycleMarkerDispenser types");
            ServiceLoader<TrackerDispenser> sl = ServiceLoader.load(TrackerDispenser.class);
            for (TrackerDispenser cmd : sl) {
                if (types.get(cmd.getName()) != null) {
                    throw new RuntimeException("CycleMarkerDispenser '" + cmd.getName()
                            + "' is already defined.");
                }
                types.put(cmd.getName(),cmd);
            }
        }
        logger.info("Loaded Types:" + types.keySet());
        return types;
    }

    /**
     * Return list of activity types tha have been found by this runtime,
     * in alphabetical order of their type names.
     * @return a list of ActivityType instances.
     */
    public List<TrackerDispenser> getAll() {
        List<TrackerDispenser> types = new ArrayList<>(getTypes().values());
        types.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
        return Collections.unmodifiableList(types);
    }
}
