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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.rachio.RachioBindingConstants;
import org.openhab.binding.rachio.handler.RachioDeviceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioDevice} stores attributes received from the Rachio cloud api and represents a device..
 *
 * @author Markus Michels (markus7017) - Initial contribution
 */

public class RachioDevice extends RachioCloudDevice {
    private final Logger logger = LoggerFactory.getLogger(RachioDevice.class);

    // extensions to cloud attributes
    public String runList = "";
    public Integer runTime = 0;
    public String lastEvent = "";
    public boolean paused = false;
    public int rainDelay = 0;

    public ThingUID bridge_uid;
    public ThingUID dev_uid;
    private HashMap<String, RachioZone> zoneList = new HashMap<String, RachioZone>();
    private RachioDeviceHandler thingHandler = null;

    public RachioDevice(RachioCloudDevice device) {
        try {
            RachioApi.copyMatchingFields(device, this);
            zoneList = new HashMap<String, RachioZone>(); // discard current list
            for (int i = 0; i < device.zones.size(); i++) {
                RachioCloudZone zone = device.zones.get(i);
                zoneList.put(zone.id, new RachioZone(zone, getThingID()));
            }
            logger.info("RachioDevice '{}', id={} initialized with {} zones.", device.name, device.id,
                    device.zones.size());
        } catch (Exception e) {
            logger.error("RachioDevice: Unable to initialize: {}", e.getMessage());
        }
    }

    /**
     * Set the ThingHandler for this device
     *
     * @param deviceHandler
     */
    public void setThingHandler(RachioDeviceHandler deviceHandler) {
        if (deviceHandler == null) {
            logger.debug("RachioDevice: Invalud thing handler!");
        }
        thingHandler = deviceHandler;
    }

    /**
     * @return thing handler for this zone
     */
    public RachioDeviceHandler getThingHandler() {
        if (thingHandler == null) {
            logger.debug("RachioDevice: Invalud thing handler!");
        }
        return thingHandler;
    }

    /**
     * compare some specific device properties to decide if channel updates are performed
     *
     * @param cdev device properties to compare
     * @return true: no change, false: update required
     */
    public boolean compare(RachioDevice cdev) {
        if ((cdev == null) || !id.equalsIgnoreCase(cdev.id) || !status.equalsIgnoreCase(cdev.status) || (on != cdev.on)
                || (paused != cdev.paused)) {
            logger.trace("RachioDevice: update data received");
            return false;
        }
        return true;
    }

    /**
     * Copy relevant attributes read from cloud
     *
     * @param updatedData new device settings received from cloud call
     */
    public void update(RachioDevice updatedData) {
        if ((updatedData == null) || !id.equals(updatedData.id)) {
            return;
        }
        status = updatedData.status;
        on = updatedData.on;
        paused = updatedData.paused;
    }

    /**
     * Save ThingUID (used for mapping ThingUID to internal data structure)
     *
     * @param bridgeUID
     * @param deviceUID
     */
    public void setUID(ThingUID bridgeUID, ThingUID deviceUID) {
        bridge_uid = bridgeUID;
        dev_uid = deviceUID;
    }

    /**
     * @return Device thing uid
     */
    public ThingUID getUID() {
        return dev_uid;
    }

    /**
     * Fill the Thing property data
     *
     * @return A map for key/value
     */
    public Map<String, String> fillProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put(Thing.PROPERTY_VENDOR, RachioBindingConstants.BINDING_VENDOR);
        properties.put(RachioBindingConstants.PROPERTY_NAME, name);
        properties.put(RachioBindingConstants.PROPERTY_MODEL, model);
        properties.put(Thing.PROPERTY_SERIAL_NUMBER, serialNumber);
        properties.put(Thing.PROPERTY_MAC_ADDRESS, macAddress);
        return properties;
    }

    /**
     * Get the thing unique id
     *
     * @return Suffix for the thing name
     */
    public String getThingID() {
        return macAddress;
    }

    /**
     * Get the thing's name
     *
     * @return Name
     */
    public String getThingName() {
        return name;
    }

    /**
     * Get controller status as OnOffType
     *
     * @return Thing status
     */
    public ThingStatus getStatus() {
        if (status.equals("ONLINE")) {
            return ThingStatus.ONLINE;
        }
        if (status.equals("OFFLINE")) {
            return ThingStatus.OFFLINE;
        }
        logger.debug("RachioDevice: Device status '{}' was mapped to OFFLINE", status);
        return ThingStatus.OFFLINE;
    }

    public void setStatus(String new_status) {
        if (new_status.equals("ONLINE") || new_status.equals("OFFLINE")) {
            status = new_status;
            return;
        }
        logger.debug("RachioDevice: Device status '{}' was not set!", new_status);
    }

    /**
     * Get controller status (online/offline) as OnOffType
     *
     * @return Controller status, ON=online, OFF=offline
     */
    public OnOffType getOnline() {
        return status.equals("ONLINE") ? OnOffType.ON : OnOffType.OFF;
    }

    /**
     * Get enabled status as OnOffType
     *
     * @return ON=enabled, OFF=disabled
     */
    public OnOffType getEnabled() {
        return on ? OnOffType.ON : OnOffType.OFF;
    }

    /**
     * Get operation mode
     *
     * @return ON=running, OFF=standby
     */
    public OnOffType getSleepMode() {
        return paused ? OnOffType.ON : OnOffType.OFF;
    }

    public void setSleepMode(String subType) {
        paused = subType.contains("ON") ? true : false;
    }

    /**
     * Put controller into rain delay mode
     *
     * @param newDelay Number of seconds for the Rain Delay mode
     */
    public void setRainDelay(int newDelay) {
        rainDelay = newDelay;
    }

    /**
     * Get the list of zones to run when starting watering on the controller
     *
     * @return Comma seperated list of zones to run
     */
    public String getRunZones() {
        return runList;
    }

    /**
     * Set the zone list for running the controller
     *
     * @param list Comma seperated list of zone IDs
     */
    public void setRunZones(String list) {
        runList = list;
    }

    /**
     * Get total run time for the controller as returned from the Cloud API
     *
     * @return Total run time for the controller
     */
    public int getRunTime() {
        return runTime;
    }

    /**
     * Set the run time for next run
     *
     * @param time Number of seconds to run the zones
     */
    public void setRunTime(int time) {
        runTime = time;
    }

    public void setEvent(RachioEvent event) {
        lastEvent = new RachioEventString(event).toJson();
    }

    public String getEvent() {
        return lastEvent;
    }

    public String getAllRunZonesJson(int defaultRuntime) {
        boolean flAll = runList.equals("") || runList.equalsIgnoreCase("ALL");

        String json = "{ \"zones\" : [";
        for (HashMap.Entry<String, RachioZone> ze : zoneList.entrySet()) {
            RachioZone zone = ze.getValue();
            if (flAll || runList.contains(zone.zoneNumber + ",") && (zone.getEnabled() == OnOffType.ON)) {
                int runtime = zone.getStartRunTime() > 0 ? zone.getStartRunTime() : defaultRuntime;
                json = json + "{ \"id\" : \"" + zone.id + "\", \"duration\" : " + runtime + ", \"sortOrder\" : 1}";
            }
        }
        json = json + "]";
        return json;
    }

    /**
     * Get a list of all zones belonging to this controller
     *
     * @return Zone list (HashMap)
     */
    public HashMap<String, RachioZone> getZones() {
        return zoneList;
    }

    public RachioZone getZoneByNumber(int zoneNumber) {
        for (HashMap.Entry<String, RachioZone> ze : zoneList.entrySet()) {
            RachioZone zone = ze.getValue();
            if ((zone != null) && zone.zoneNumber == zoneNumber) {
                return zone;
            }
        }
        return null;
    }

    public RachioZone getZoneById(String zoneId) {
        for (HashMap.Entry<String, RachioZone> ze : zoneList.entrySet()) {
            RachioZone zone = ze.getValue();
            if ((zone != null) && zone.id.equals(zoneId)) {
                return zone;
            }
        }
        return null;
    }
} // class rachioDevice
