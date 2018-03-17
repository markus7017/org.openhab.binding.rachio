/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * @author Markus Michels (markus7017) - Initial contribution
 */
package org.openhab.binding.rachio.internal.api;

import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.rachio.handler.RachioZoneHandler;
import org.openhab.binding.rachio.internal.RachioEvent;
import org.openhab.binding.rachio.internal.util.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioZone} stores attributes received from the Rachio cloud api and represents a zone..
 *
 * @author Markus Michels (markus7017) - Initial contribution
 */
public class RachioZone {
    private final Logger logger = LoggerFactory.getLogger(RachioZone.class);
    protected ThingUID dev_uid;
    protected ThingUID zone_uid;
    protected RachioZoneHandler thingHandler;
    public String dev_unique_id;

    // RachioApi: Zone (click expand)
    /*
     * {
     * "id":"dd269f3e-abde-4303-8a29-fa49508818a2",
     * "zoneNumber":7,"name":"Zone 7","enabled":false,
     * "customNozzle":
     * {"name":"Fixed Spray Head",
     * "imageUrl":"https://s3-us-west-2.amazonaws.com/rachio-api-icons/nozzle/fixed_spray.png",
     * "category":"FIXED_SPRAY_HEAD",
     * "inchesPerHour":1.5},
     * "customSoil":
     * {"createDate":1494473341401,
     * "lastUpdateDate":1494473341401,
     * "id":"441a1938-68cc-44b2-9669-99975edd773f",
     * "name":"Loam",
     * "imageUrl":"https://s3-us-west-2.amazonaws.com/rachio-api-icons/soil/loam.png",
     * "category":"LOAM",
     * "infiltrationRate":0.35,
     * "editable":false,
     * "percentAvailableWater":0.7
     * },
     * "customSlope":
     * {"name":"Flat",
     * "imageUrl":"https://s3-us-west-2.amazonaws.com/rachio-api-icons/slope/flat.png",
     * "variance":"ZERO_THREE","sortOrder":0
     * },
     * "customCrop":
     * {"name":"Cool Season Grass",
     * "imageUrl":"https://s3-us-west-2.amazonaws.com/rachio-api-icons/crop/cool_season_grass.png",
     * "coefficient":0.8
     * },
     * "customShade":
     * {"name":"Lots of sun",
     * "description":"6-8 hours of sun",
     * "imageUrl":"https://s3-us-west-2.amazonaws.com/rachio-api-icons/shade/lots_of_sun.png",
     * "exposure":1.0
     * },
     * "availableWater":0.17,
     * "rootZoneDepth":6.0,
     * "managementAllowedDepletion":0.5,
     * "efficiency":0.8,
     * "yardAreaSquareFeet":500,
     * "imageUrl":"http://media.rach.io/images/zone/default/default_zone.jpg",
     * "scheduleDataModified":false,
     * "fixedRuntime":0,
     * "saturatedDepthOfWater":0.56,
     * "depthOfWater":0.51,
     * "maxRuntime":10800,
     * "runtimeNoMultiplier":1391,
     * "wateringAdjustmentRuntimes":
     * {"1":2086,
     * "2":1739,
     * "3":1391,
     * "4":1043,
     * "5":695
     * },
     * "runtime":1391
     * }
     */

    protected String zone_id = "";
    protected String zone_name = "";
    protected Integer zone_number = 0;
    protected Boolean zone_enabled = false;
    protected Double zone_avl_water = 0.0;
    protected Integer zone_root_depth = 0;
    protected Double zone_mgtAllowdDeletion = 0.0;
    protected Double zone_efficiency = 0.0;
    protected String zone_imageUrl = "";
    protected Integer zone_yardAreaSqft = 0;
    protected Double zone_water_depth = 0.0;
    protected Integer zone_runtime = 0;
    protected Integer zone_lastTime = 0;
    protected Integer zone_lastDuration = 0;
    protected String nozzle_name = "";
    protected String nozzle_imageUrl = "";
    protected String nozzle_headCat = "";
    protected Double nozzle_inchesPerHour = 0.0;
    protected String zone_event = "";

    protected int zone_startRunTime = 0;

    public RachioZone(String json, String uniqueId) {
        try {
            logger.trace("RachioZonee: initialize, JSON={}", json);

            dev_unique_id = uniqueId;

            // Parse json zone information - see end of this file for a JSON sample
            zone_id = Parse.jsonString(json, "id", "");
            zone_name = Parse.jsonString(json, "name", "");
            zone_number = Parse.jsonInt(json, "zoneNumber", 0);
            zone_enabled = Parse.jsonBoolean(json, "enabled", false);
            zone_avl_water = Parse.jsonDouble(json, "availableWater", 0.0);
            zone_root_depth = Parse.jsonInt(json, "rootZoneDepth", 0);
            zone_mgtAllowdDeletion = Parse.jsonDouble(json, "managementAllowedDepletion", 0.0);
            zone_efficiency = Parse.jsonDouble(json, "availableWater", 0.0);
            zone_imageUrl = Parse.jsonString(json, "imageUrl", "");
            zone_yardAreaSqft = Parse.jsonInt(json, "yardAreaSquareFeet", 0);
            zone_water_depth = Parse.jsonDouble(json, "depthOfWater", 0.0);
            zone_runtime = Parse.jsonInt(json, "runtime", 0);

            zone_lastDuration = Parse.jsonInt(json, "lastWateredDate", 0);
            zone_lastTime = Parse.jsonInt(json, "lastWateredDuration", 0);

            // Parse noizzle information
            String n_json = Parse.jsonSection(json, "customNozzle");
            logger.trace("RachioZone: noizzle JSON={}", json);
            nozzle_name = Parse.jsonString(n_json, "name", "");
            nozzle_imageUrl = Parse.jsonString(n_json, "imageUrl", "");
            nozzle_headCat = Parse.jsonString(n_json, "category", "");
            nozzle_inchesPerHour = Parse.jsonDouble(n_json, "inchesPerHour", 0.0);

            n_json = Parse.jsonSection(json, "customSoil");
            n_json = Parse.jsonSection(json, "customSlope");
            n_json = Parse.jsonSection(json, "customCrop");
            n_json = Parse.jsonSection(json, "customShade");
        } catch (Exception e) {
            logger.error("Unable to parse RachioZone from API ({}) JSON='{}'", e.getMessage(), json);

        }

    } // RachioZone(String json, String uniqueId)

    public void setThingHandler(RachioZoneHandler zoneHandler) {
        thingHandler = zoneHandler;
    }

    public RachioZoneHandler getThingHandler() {
        return thingHandler;
    }

    public boolean compare(RachioZone czone) {
        /*
         * if ((czone == null) || (this.zone_number != czone.zone_number) || (this.zone_enabled != czone.zone_enabled)
         * || (this.zone_avl_water != czone.zone_avl_water) || (this.zone_efficiency != czone.zone_efficiency)
         * || (this.zone_water_depth != czone.zone_water_depth) || (this.zone_runtime != czone.zone_runtime)) {
         */
        if ((czone == null) || !this.zone_number.equals(czone.zone_number)
                || !this.zone_enabled.equals(czone.zone_enabled) || !this.zone_avl_water.equals(czone.zone_avl_water)
                || !this.zone_efficiency.equals(czone.zone_efficiency)
                || !this.zone_water_depth.equals(czone.zone_water_depth)
                || !this.zone_runtime.equals(czone.zone_runtime)) {
            return false;
        }
        return true;
    } // compare()

    public void update(RachioZone updatedZone) {
        if ((updatedZone == null) || !getId().equals(updatedZone.getId())) {
            return;
        }
        zone_number = updatedZone.zone_number;
        zone_enabled = updatedZone.zone_enabled;
        zone_avl_water = updatedZone.zone_avl_water;
        zone_efficiency = updatedZone.zone_efficiency;
        zone_water_depth = updatedZone.zone_water_depth;
        zone_runtime = updatedZone.zone_runtime;
    } // update()

    public void setUID(ThingUID deviceUID, ThingUID zoneUID) {
        dev_uid = deviceUID;
        zone_uid = zoneUID;
    }

    public ThingUID getUID() {
        return zone_uid;
    }

    public ThingUID getDevUID() {
        return dev_uid;
    }

    public String getThingID() {
        return dev_unique_id + "-" + zone_number;
    }

    public Map<String, String> fillProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put(PROPERTY_NAME, getName());
        properties.put(PROPERTY_ID, getId());
        return properties;
    }

    public String getId() {
        return zone_id;
    }

    public String getName() {
        return zone_name;
    }

    public int getZoneNumber() {
        return zone_number;
    }

    public OnOffType getEnabled() {
        return zone_enabled ? OnOffType.ON : OnOffType.OFF;
    }

    public Double getAvlWater() {
        return zone_avl_water;
    }

    public int getRootDepth() {
        return zone_root_depth;
    }

    public Double getMsgAllowsDeletion() {
        return zone_mgtAllowdDeletion;
    }

    public Double getEfficiency() {
        return zone_efficiency;
    }

    public int getYardAreaSqft() {
        return zone_yardAreaSqft;
    }

    public double getWaterDepth() {
        return zone_water_depth;
    }

    public int getRuntime() {
        return zone_runtime;
    }

    public String getImageUrl() {
        return zone_imageUrl;
    }

    public String getNozzleName() {
        return nozzle_name;
    }

    public String getNozzleImageUrl() {
        return nozzle_imageUrl;
    }

    public String getNozzleHeadCat() {
        return nozzle_headCat;
    }

    public Double getNozzleInchesPerHour() {
        return nozzle_inchesPerHour;
    }

    public void setStartRunTime(int duration) {
        zone_startRunTime = duration;
    }

    public int getStartRunTime() {
        return zone_startRunTime;
    }

    public void setEvent(RachioEvent event) {
        zone_event = event.summary;
    }

    public String getEvent() {
        return zone_event;
    }

    public boolean isEnable() {
        return zone_enabled;
    }

} // class RachioZone
