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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioZone} stores attributes received from the Rachio cloud api and represents a zone..
 *
 * @author Markus Michels (markus7017) - Initial contribution
 */

public class RachioZone extends RachioCloudZone {
    private final Logger logger = LoggerFactory.getLogger(RachioZone.class);
    protected ThingUID dev_uid;
    protected ThingUID zone_uid;
    protected RachioZoneHandler thingHandler;
    protected String uniqueId = "";

    public String lastEvent = "";
    protected int startRunTime = 0;

    /**
     * Use reflection to shallow copy simple type fields with matching names from one object to another
     *
     * @param fromObj the object to copy from
     * @param toObj the object to copy to
     */
    public RachioZone(RachioCloudZone zone, String uniqueId) {
        try {
            RachioApi.copyMatchingFields(zone, this);
            this.uniqueId = uniqueId;
            logger.trace("RachioZone: Zone '{}' (number={}, id={}, enable={}) initialized.", zone.name, zone.zoneNumber,
                    zone.id, zone.enabled);
        } catch (Exception e) {
            logger.error("RachioZone: Unable to initialized: {}", e.getMessage());
        }
    }

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
        if ((czone == null) || (zoneNumber != czone.zoneNumber) || (enabled != czone.enabled)
                || (availableWater != czone.availableWater) || (efficiency != czone.efficiency)
                || (lastWateredDate != czone.lastWateredDate) || (depthOfWater != czone.depthOfWater)
                || (runtime != czone.runtime)) {
            return false;
        }
        return true;
    } // compare()

    public void update(RachioZone updatedZone) {
        if ((updatedZone == null) || !id.equalsIgnoreCase(updatedZone.id)) {
            return;
        }
        zoneNumber = updatedZone.zoneNumber;
        enabled = updatedZone.enabled;
        availableWater = updatedZone.availableWater;
        efficiency = updatedZone.efficiency;
        depthOfWater = updatedZone.depthOfWater;
        runtime = updatedZone.runtime;
        lastWateredDate = updatedZone.lastWateredDate;
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
        // build thing name like rachio_zone_1_74C63B174B7B_7
        return uniqueId + "-" + zoneNumber;
    }

    public Map<String, String> fillProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put(PROPERTY_NAME, name);
        properties.put(PROPERTY_ZONE_ID, id);
        return properties;
    }

    public OnOffType getEnabled() {
        return enabled ? OnOffType.ON : OnOffType.OFF;
    }

    // public String getNozzleName() {
    // return customNozzle.name;
    // }

    // public String getNozzleImageUrl() {
    // return customNozzle.imageUrl;
    // }

    // public Double getNozzleInchesPerHour() {
    // return customNozzle.inchesPerHour;
    //

    public void setStartRunTime(int runtime) {
        startRunTime = runtime;
    }

    public int getStartRunTime() {
        return startRunTime;
    }

    public void setEvent(RachioEvent event) {
        lastEvent = new RachioEventString(event).toJson();
    }

    public String getEvent() {
        return lastEvent;
    }

    public boolean isEnable() {
        return enabled;
    }
} // class RachioZone
