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
import org.openhab.binding.rachio.internal.RachioEvent;
import org.openhab.binding.rachio.internal.RachioEventString;
import org.openhab.binding.rachio.internal.util.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * The {@link RachioDevice} stores attributes received from the Rachio cloud api and represents a device..
 *
 * @author Markus Michels (markus7017) - Initial contribution
 */

public class RachioDevice {
    private final Logger logger = LoggerFactory.getLogger(RachioDevice.class);

    protected ThingUID bridge_uid;
    protected ThingUID dev_uid;
    protected HashMap<String, RachioZone> zoneList = new HashMap<String, RachioZone>();

    protected String dev_id = "";
    protected String dev_name = "";
    protected String dev_status = "";
    protected Boolean dev_paused = false;
    protected Boolean dev_on = false;
    protected Double dev_latitude = 0.0;
    protected Double dev_longitude = 0.0;
    protected Double dev_elevation = 0.0;
    protected String dev_timeZone = "";
    protected Integer dev_utcOffset = 0;
    protected String dev_zip = "";
    protected String dev_model = "";
    protected String dev_serial = "";
    protected String dev_mac = "";
    protected Integer dev_rainDelay = 0;
    protected String dev_event = "";

    private RachioDeviceHandler thingHandler = null;
    protected String dev_runList = "";
    protected Integer dev_runTime = 0;
    public boolean masterCopy = false;

    public void setMasterCopy(boolean masterCopy) {
        this.masterCopy = masterCopy;
    }

    public RachioDevice(String json) {
        try {
            logger.trace("RachioDevice: initialize, JSON={}", json);

            dev_id = Parse.jsonString(json, "id", "");
            dev_status = Parse.jsonString(json, "status", "");
            dev_paused = Parse.jsonBoolean(json, "paused", false);
            dev_on = Parse.jsonBoolean(json, "on", true);
            dev_latitude = Parse.jsonDouble(json, "latitude", 0.0);
            dev_longitude = Parse.jsonDouble(json, "longitude", 0.0);
            dev_elevation = Parse.jsonDouble(json, "elevation", 0.0);
            dev_timeZone = Parse.jsonString(json, "timeZone", "");
            dev_utcOffset = Parse.jsonInt(json, "utcOffset", 0);
            dev_zip = Parse.jsonString(json, "zip", "");
            dev_name = Parse.jsonString(json, "name", "");
            dev_serial = Parse.jsonString(json, "serialNumber", "");
            dev_mac = Parse.jsonString(json, "macAddress", "");
            dev_model = Parse.jsonString(json, "model", "");
            if (dev_name.isEmpty()) {
                dev_name = dev_id; // make sure name is filled, will be used for the Thing name
            }

            // "rainDelayStartDate":1517810739407,
            // "rainDelayExpirationDate":1517814299000,
            // "utcOffset":-18000000,

            JsonArray zl = Parse.jsonObjectArray(json, "zones");
            for (int i = 0; i < zl.size(); i++) {
                JsonElement je = zl.get(i);
                String jsonZone = je.toString();
                // logger.debug("JSON for zone data: {}", jsonZone);

                // ObjectMapper mapper = new ObjectMapper();
                // RachioZone z = mapper.readValue(jsonZone, RachioZone.class);

                String id = Parse.jsonString(jsonZone, "id", "");
                RachioZone rz = new RachioZone(jsonZone, getThingID());
                zoneList.put(id, rz);
            }
        } catch (Exception e) {
            logger.error("Unable to parse RachioZone from API ({}), JSON='{}'", e.getMessage(), json);
        }
    } // rachioDevice()

    /**
     * Set the ThingHandler for this device
     *
     * @param deviceHandler
     */
    public void setThingHandler(RachioDeviceHandler deviceHandler) {
        if ((!masterCopy) || (deviceHandler == null)) {
            logger.debug("RachioDevice: Invalud thing handler!");
        }
        thingHandler = deviceHandler;
    }

    /**
     * @return thing handler for this zone
     */
    public RachioDeviceHandler getThingHandler() {
        if (!masterCopy) {
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
        if ((cdev == null) || !dev_id.equals(cdev.getId()) || !dev_status.equals(cdev.dev_status)
                || (dev_on != cdev.dev_on) || (dev_paused != cdev.dev_paused)) {
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
        if ((updatedData == null) || !dev_id.equals(updatedData.getId())) {
            return;
        }
        dev_status = updatedData.dev_status;
        dev_paused = updatedData.dev_paused;
        dev_on = updatedData.dev_on;
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
        properties.put(RachioBindingConstants.PROPERTY_NAME, getName());
        properties.put(RachioBindingConstants.PROPERTY_MODEL, getModel());
        properties.put(RachioBindingConstants.PROPERTY_ID, getId());
        properties.put(Thing.PROPERTY_VENDOR, RachioBindingConstants.BINDING_VENDOR);
        properties.put(Thing.PROPERTY_SERIAL_NUMBER, dev_serial);
        properties.put(Thing.PROPERTY_MAC_ADDRESS, dev_mac);
        return properties;
    }

    /**
     * Get the thing unique id
     *
     * @return Suffix for the thing name
     */
    public String getThingID() {
        return getMacAddress();
    }

    /**
     * Get the thing's name
     *
     * @return Name
     */
    public String getThingName() {
        return getName();
    }

    /**
     * Get controller name
     *
     * @return Name
     */
    public String getName() {
        return dev_name;
    }

    /**
     * Get controller model
     *
     * @return Model
     */
    public String getModel() {
        return dev_model;
    }

    /**
     * Get deviceId - use for calls to the Rachio Cloud
     *
     * @return
     */
    public String getId() {
        return dev_id;
    }

    /**
     * Get controller's MAC address (wlan interface)
     *
     * @return MAC address
     */
    public String getMacAddress() {
        return dev_mac;
    }

    /**
     * Get controller's S/N
     *
     * @return Serial Number
     */
    public String getSerial() {
        return dev_serial;
    }

    /**
     * Get controller status as OnOffType
     *
     * @return Thing status
     */
    public ThingStatus getStatus() {
        if (dev_status.equals("ONLINE")) {
            return ThingStatus.ONLINE;
        }
        return ThingStatus.OFFLINE;
    }

    /**
     * Get controller status (online/offline) as OnOffType
     *
     * @return Controller status, ON=online, OFF=offline
     */
    public OnOffType getOnline() {
        return dev_status.equals("ONLINE") ? OnOffType.ON : OnOffType.OFF;
    }

    /**
     * Get device status as string (ONLINE...)
     *
     * @return Device status
     */
    public String getStatusStr() {
        return dev_status;
    }

    /**
     * Get enabled status as OnOffType
     *
     * @return ON=enabled, OFF=disabled
     */
    public OnOffType getEnabled() {
        return dev_on ? OnOffType.ON : OnOffType.OFF;
    }

    /**
     * Get operation mode
     *
     * @return ON=running, OFF=standby
     */
    public OnOffType getPaused() {
        return dev_paused ? OnOffType.ON : OnOffType.OFF;
    }

    /**
     * Get controller's GPS coordinate: Latitude
     *
     * @return Latitude
     */
    public Double getLatitude() {
        return dev_latitude;
    }

    /**
     * Get controller's GPS coordinate: Longitude
     *
     * @return Longitude
     */
    public Double getLongitude() {
        return dev_longitude;
    }

    /**
     * Get controller's GPS coordinate: Elevation
     *
     * @return Elevation
     */
    public Double getElevation() {
        return dev_elevation;
    }

    /**
     * Get controller's rain delay status
     *
     * @return Rain Delay
     */
    public int getRainDelay() {
        return dev_rainDelay;
    }

    /**
     * Put controller into rain delay mode
     *
     * @param newDelay Number of seconds for the Rain Delay mode
     */
    public void setRainDelay(int newDelay) {
        dev_rainDelay = newDelay;
    }

    /**
     * Get the list of zones to run when starting watering on the controller
     *
     * @return Comma seperated list of zones to run
     */
    public String getRunZones() {
        return dev_runList;
    }

    /**
     * Set the zone list for running the controller
     *
     * @param list Comma seperated list of zone IDs
     */
    public void setRunZones(String list) {
        dev_runList = list;
    }

    /**
     * Get total run time for the controller as returned from the Cloud API
     *
     * @return Total run time for the controller
     */
    public int getRunTime() {
        return dev_runTime;
    }

    /**
     * Set the run time for next run
     *
     * @param time Number of seconds to run the zones
     */
    public void setRunTime(int time) {
        dev_runTime = time;
    }

    public void setEvent(RachioEvent event) {
        RachioEventString e = new RachioEventString(event);
        dev_event = e.toJson();
    }

    public String getEvent() {
        return dev_event;
    }

    public String getAllRunZonesJson(int defaultRuntime) {
        boolean flAll = dev_runList.equals("") || dev_runList.equals("ALL");

        String json = "{ \"zones\" : [";
        for (HashMap.Entry<String, RachioZone> ze : zoneList.entrySet()) {
            RachioZone zone = ze.getValue();
            if (flAll || dev_runList.contains(zone.getZoneNumber() + ",") && (zone.getEnabled() == OnOffType.ON)) {
                int runtime = zone.getStartRunTime() > 0 ? zone.getStartRunTime() : defaultRuntime;
                json = json + "{ \"id\" : \"" + zone.getId() + "\", \"duration\" : " + runtime + ", \"sortOrder\" : 1}";
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

} // class rachioDevice

// -------

/**
 * API Sample Data:
 * HTTP/1.1 200 OK
 * {
 * "id": "3c59a593-04b8-42df-91db-758f4fe4a97f",
 * "username": "franz",
 * "fullName": "Franz Garsombke",
 * "email": "franz@rach.io",
 * "devices": [
 * {
 * "id": "2a5e7d3c-c140-4e2e-91a1-a212a518adc5",
 * "status": "ONLINE",
 * "zones": [
 * {
 * "id": "e02de192-5a2b-4669-95c6-34deea3d23cb",
 * "zoneNumber": 3,
 * "name": "Zone 3",
 * "enabled": false,
 * "customNozzle": {
 * "name": "Fixed Spray Head",
 * "imageUrl": "https://s3-us-west-2.amazonaws.com/rachio-api-icons/nozzle/fixed_spray.png",
 * "category": "FIXED_SPRAY_HEAD",
 * "inchesPerHour": 1.4
 * },
 * "availableWater": 0.17,
 * "rootZoneDepth": 10,
 * "managementAllowedDepletion": 0.5,
 * "efficiency": 0.6,
 * "yardAreaSquareFeet": 1000,
 * "irrigationAmount": 0,
 * "depthOfWater": 0.85,
 * "runtime": 3643
 * },
 * ...
 * ],
 * "timeZone": "America/Denver",
 * "latitude": 39.84634,
 * "longitude": -105.3383,
 * "zip": "80403",
 * "name": "Prototype 7",
 * "scheduleRules": [
 * {
 * "id": "cc9c6e6f-c285-4a7b-9911-ff6065e7ff5b",
 * "name": "",
 * "externalName": "unknown"
 * }
 * "serialNumber": "PROTOTYPE7SN",
 * "rainDelayExpirationDate": 1420027691501,
 * "rainDelayStartDate": 1420026367029,
 * "macAddress": "PROTOTYPE7MA",
 * "elevation": 2376.8642578125,
 * "webhooks": [],
 * "paused": false,
 * "on": true,
 * "flexScheduleRules": [],
 * "utcOffset": -25200000
 * }
 * ],
 * "enabled": true
 * }
 *
 */
