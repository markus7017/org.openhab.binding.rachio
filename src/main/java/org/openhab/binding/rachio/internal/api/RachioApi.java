/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.rachio.internal.api;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.rachio.RachioBindingConstants;
import org.openhab.binding.rachio.internal.util.Http;
import org.openhab.binding.rachio.internal.util.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * The {@link RachioApi} implements the interface to the Rachio cloud service (using http).
 *
 * @author Markus Michels (markus7017) - Initial contribution
 */

public class RachioApi {
    private final Logger logger = LoggerFactory.getLogger(RachioApi.class);

    private static final String APIURL_BASE = "https://api.rach.io/1/public/";

    private static final String APIURL_GET_PERSON = "person/info"; // obtain personId
    private static final String APIURL_GET_PERSONID = "person"; // obtain personId
    private static final String APIURL_GET_DEVICE = "device"; // get device details, needs /<device id>

    private static final String APIURL_DEV_PUT_ON = "device/on"; // Enable device / all functions
    private static final String APIURL_DEV_PUT_OFF = "device/off"; // Disable device / all functions
    private static final String APIURL_DEV_PUT_STOP = "device/stop_water"; // stop watering (all zones)
    private static final String APIURL_DEV_PUT_RAIN_DELAY = "device/rain_delay"; // Rain delay device
    private static final String APIURL_DEV_POST_WEBHOOK = "notification/webhook"; // Register WebHook for Device
    private static final String APIURL_DEV_QUERY_WEBHOOK = "notification"; // completes to
                                                                           // /public/notification/:deviceId/webhook
    private static final String APIURL_DEV_DELETE_WEBHOOK = "notification/webhook";

    private static final String APIURL_ZONE_PUT_START = "zone/start"; // start a zone
    private static final String APIURL_ZONE_PUT_MULTIPLE_START = "/zone/start_multiple"; // start multiple zones

    private static final String APIURL_NOT_GET_LIST = "notification/webhook_event_type"; // get list of available
                                                                                         // notification types
    // WebHook event types
    public final String WHE_DEVICE_STATUS = "DEVICE_STATUS"; // "Device status event has occurred"
    public final String WHE_RAIN_DELAY = "RAIN_DELAY"; // "A rain delay event has occurred"
    public final String WEATHER_INTELLIGENCE = "WEATHER_INTELLIGENCE"; // A weather intelligence event has has occurred
    public final String WHE_WATER_BUDGET = "WATER_BUDGET"; // A water budget event has occurred
    public final String WHE_SCHEDULE_STATUS = "SCHEDULE_STATUS";
    public final String WHE_ZONE_STATUS = "ZONE_STATUS";
    public final String WHE_RAIN_SENSOR_DETECTION = "RAIN_SENSOR_DETECTION"; // physical rain sensor event has coccurred
    public final String WHE_ZONE_DELTA = "ZONE_DELTA"; // A physical rain sensor event has occurred
    public final String WHE_DELTA = "DELTA"; // "An entity has been inserted, updated, or deleted"

    public class RachioApiWebHookEntry {
        public long createDate = -1;
        public long lastUpdateDate = -1;
        public String id = "";
        public String url = "";
        public JsonArray eventTypes;
        public String externalId = "";

        RachioApiWebHookEntry() {
            eventTypes = new JsonArray();
        }
    }

    private static final String JSON_OPTION_ID = "id";
    private static final String JSON_OPTION_PERSON_USERNAME = "username";
    private static final String JSON_OPTION_PERSON_FULLNAME = "fullName";
    private static final String JSON_OPTION_PERSON_EMAIL = "email";

    protected String apiKey = "";
    protected String personId = "";
    protected String userName = "";
    protected String fullName = "";
    protected String email = "";

    public boolean masterCopy = false;
    private HashMap<String, RachioDevice> deviceList = new HashMap<String, RachioDevice>();
    protected Http api = null;
    private Map<String, Integer> wh_eventTypes;

    public void setMaster() {
        masterCopy = true;
    }

    public Boolean initialize(String apiKey, ThingUID bridgeUID) {
        try {
            this.apiKey = apiKey;
            api = new Http(this.apiKey);
            if (initializePersonId() && initializeDevices(bridgeUID) && initializeZones() && initializeWebHook()) {
                logger.trace("Rachio API initialized");
                return true;
            }
        } catch (Exception e) {
            logger.error("Rachio API.Exception: {}", e.getMessage());
        }

        api = null;
        logger.error("RachioApi.initialize(): API initialization failed!");
        return false;
    } // initialize()

    public HashMap<String, RachioDevice> getDevices() {
        return deviceList;
    }

    public RachioDevice getDevByUID(ThingUID bridgeUID, ThingUID thingUID) {
        for (HashMap.Entry<String, RachioDevice> entry : deviceList.entrySet()) {
            RachioDevice dev = entry.getValue();
            // logger.debug("RachioDev.getDevByUID: bridge {} / {}, device {} / {}", bridgeUID, dev.bridge_uid,
            // thingUID, dev.dev_uid);
            if (dev.bridge_uid.equals(bridgeUID) && dev.getUID().equals(thingUID)) {
                logger.trace("RachioApi: Device '{}' found.", dev.getName());
                return dev;
            }
        }
        logger.debug("RachioApi.getDevByUID: Unable map UID to device");
        return null;
    } // getDevByUID()

    public RachioZone getZoneByUID(ThingUID bridgeUID, ThingUID zoneUID) {
        HashMap<String, RachioDevice> deviceList = getDevices();
        if (deviceList == null) {
            return null;
        }
        for (HashMap.Entry<String, RachioDevice> de : deviceList.entrySet()) {
            RachioDevice dev = de.getValue();

            HashMap<String, RachioZone> zoneList = dev.getZones();
            for (HashMap.Entry<String, RachioZone> ze : zoneList.entrySet()) {
                RachioZone zone = ze.getValue();
                if (zone.getUID().equals(zoneUID)) {
                    return zone;
                }
            }
        }
        return null;
    } // getDevByUID()

    private Boolean initializePersonId() throws Exception {
        if (api == null) {
            logger.debug("RachioApi.initializePersonId: API not initialized!");
            return false;
        }
        String returnContent = "";
        try {
            returnContent = api.sendHttpGet(APIURL_BASE + APIURL_GET_PERSON, null);
            personId = Parse.jsonString(returnContent, JSON_OPTION_ID, "");
            logger.debug("Using personId '{}'", personId);
            return true;
        } catch (Exception e) {
            logger.error("RachioApo: Unable to get personId: {}, data='{}'", e.getMessage(), returnContent);
        }
        return false;
    } // initializePersonId()

    public String getUserInfo() {
        String info = "";
        if (userName != null) {
            info = fullName + "(" + userName + ", " + email + ")";
        }
        return info;
    }

    public Boolean stopWatering(String deviceId) throws Exception {
        try {
            api.sendHttpPut(APIURL_BASE + APIURL_DEV_PUT_STOP, "{ \"id\" : \"" + deviceId + "\" }");
            logger.debug("Watering stopped for device '{}'", deviceId);
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: {} failed: {}", APIURL_DEV_PUT_STOP, e.getMessage());
        }
        return false;
    } // stopWatering()

    public Boolean enableDevice(String deviceId) throws Exception {
        try {
            api.sendHttpPut(APIURL_BASE + APIURL_DEV_PUT_ON, "{ \"id\" : \"" + deviceId + "\" }");
            logger.debug("RachioApi: Device '{}' enabled.", deviceId);
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: {} failed: {}", APIURL_DEV_PUT_ON, e.getMessage());
        }
        return false;
    } // enableDevice

    public Boolean disableDevice(String deviceId) throws Exception {
        try {
            api.sendHttpPut(APIURL_BASE + APIURL_DEV_PUT_OFF, "{ \"id\" : \"" + deviceId + "\" }");
            logger.debug("RachioApi: Device '{}' disabled.", deviceId);
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: {} failed: {}", APIURL_DEV_PUT_OFF, e.getMessage());
        }
        return false;
    } // disableDevice

    public Boolean rainDelay(String deviceId, Integer delay) throws Exception {
        try {
            api.sendHttpPut(APIURL_BASE + APIURL_DEV_PUT_RAIN_DELAY,
                    "{ \"id\" : \"" + deviceId + "\", \"durartion\" : " + delay + " }");
            logger.debug("RachioApi: Rain Delay for Device '{}' enabled.", deviceId);
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: {} failed: {}", APIURL_DEV_PUT_RAIN_DELAY, e.getMessage());
        }
        return false;
    } // rainDelay

    public boolean runMultilpeZones(String zoneListJson) {
        try {
            api.sendHttpPut(APIURL_BASE + APIURL_ZONE_PUT_MULTIPLE_START, zoneListJson);
            logger.debug("RachioApi: Multiple zones started started.");
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: {} failed: {}", APIURL_ZONE_PUT_MULTIPLE_START, e.getMessage());
        }
        return false;
    } // startZone()

    public boolean runZone(String zoneId, int duration) {
        try {
            logger.debug("RachioApi: Start zone '{}' for {} sec.", zoneId, duration);
            api.sendHttpPut(APIURL_BASE + APIURL_ZONE_PUT_START,
                    "{ \"id\" : \"" + zoneId + "\", \"duration\" : " + duration + " }");
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: Running zone '{}' for {}sec failed: {}", zoneId, duration, e.getMessage());
        }
        return false;
    } // startZone()

    public Boolean getDeviceInfo(String deviceId) throws Exception {
        try {
            api.sendHttpGet(APIURL_BASE + APIURL_GET_DEVICE + "/" + deviceId, null);
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: {} failed: {}", APIURL_GET_DEVICE, e.getMessage());
        }
        return false;
    } // getDeviceInfo

    public boolean registerWebHook(String deviceId, String callbackUrl, String externalId, Boolean clearAllCallbacks) {
        // first check/delete existing webhooks
        try {
            String jsonWebHooks = api.sendHttpGet(APIURL_BASE + APIURL_DEV_QUERY_WEBHOOK + "/" + deviceId + "/webhook",
                    null);
            logger.debug("RachioApi: Registered webhooks for device '{}': {}", deviceId, jsonWebHooks);
            JsonParser jsonParser = new JsonParser();
            JsonElement jelement = jsonParser.parse(jsonWebHooks);
            JsonArray jaWebHooks = jelement.getAsJsonArray();
            Gson gson = new Gson();
            for (int i = 0; i < jaWebHooks.size(); i++) {
                JsonElement je = jaWebHooks.get(i);
                String json = je.toString();
                RachioApiWebHookEntry whe = gson.fromJson(json, RachioApiWebHookEntry.class);
                logger.debug("RachioApi: WebHook #{}: id='{}', url='{}', externalId='{}'", i, whe.id, whe.url,
                        whe.externalId);
                if (clearAllCallbacks || whe.url.equals(callbackUrl)) {
                    logger.debug("RachioApi: The callback url '{}' is already registered -> delete", callbackUrl);
                    api.sendHttpDelete(APIURL_BASE + APIURL_DEV_DELETE_WEBHOOK + "/" + whe.id, null);
                }
            }
        } catch (Exception e) {
        }

        try {
            // {
            // "device":{"id":"2a5e7d3c-c140-4e2e-91a1-a212a518adc5"},
            // "externalId" : "external company ID",
            // "url":"https://www.mydomain.com/another_webhook",
            // "eventTypes":[{"id":"1"},{"id":"2"}]
            // }
            /*
             * id:5, type=DEVICE_STATUS
             * id:6, type=RAIN_DELAY
             * id:7, type=WEATHER_INTELLIGENCE
             * id:8, type=WATER_BUDGET
             * id:9, type=SCHEDULE_STATUS
             * id:10, type=ZONE_STATUS
             * id:11, type=RAIN_SENSOR_DETECTION
             * id:12, type=ZONE_DELTA
             * id:14, type=DELTA
             */
            logger.debug("RachioApi: Register WebHook, callback url = '{}'", callbackUrl);
            String jsonData = "{ " + "\"device\":{\"id\":\"" + deviceId + "\"}, " + "\"externalId\" : \"" + externalId
                    + "\", " + "\"url\" : \"" + callbackUrl + "\", " + "\"eventTypes\" : [" + "{\"id\" : \""
                    + wh_eventTypes.get(WHE_DEVICE_STATUS) + "\"}, " + "{\"id\" : \""
                    + wh_eventTypes.get(WHE_RAIN_DELAY) + "\"}, " + "{\"id\" : \""
                    + wh_eventTypes.get(WEATHER_INTELLIGENCE) + "\"}, " + "{\"id\" : \""
                    + wh_eventTypes.get(WHE_WATER_BUDGET) + "\"}, " + "{\"id\" : \"" + wh_eventTypes.get(WHE_ZONE_DELTA)
                    + "\"}, " + "{\"id\" : \"" + wh_eventTypes.get(WHE_SCHEDULE_STATUS) + "\"}, " + "{\"id\" : \""
                    + wh_eventTypes.get(WHE_ZONE_STATUS) + "\"}, " + "{\"id\" : \"" + wh_eventTypes.get(WHE_DELTA)
                    + "\"} " + "] }";
            api.sendHttpPost(APIURL_BASE + APIURL_DEV_POST_WEBHOOK, jsonData);
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: {} failed: {}", APIURL_GET_DEVICE, e.getMessage());
        }
        return false;
    }

    // ------------ internal stuff

    private Boolean initializeDevices(ThingUID BridgeUID) {
        try {
            if (api == null) {
                logger.debug("RachioApi.initializeDevices: API not initialized");
                return false;
            }
            deviceList = new HashMap<String, RachioDevice>(); // discard current list
            String returnContent = api.sendHttpGet(APIURL_BASE + APIURL_GET_PERSONID + "/" + personId, null);
            logger.trace("RachioApi: Initialize from JSON='{}'", returnContent);
            userName = Parse.jsonString(returnContent, JSON_OPTION_PERSON_USERNAME, "");
            fullName = Parse.jsonString(returnContent, JSON_OPTION_PERSON_FULLNAME, "");
            email = Parse.jsonString(returnContent, JSON_OPTION_PERSON_EMAIL, "");

            JsonArray devList = Parse.jsonObjectArray(returnContent, "devices");
            for (int i = 0; i < devList.size(); i++) {
                JsonElement je = devList.get(i);
                String jsonDev = je.toString();
                String id = Parse.jsonString(jsonDev, JSON_OPTION_ID, "");
                deviceList.put(id, new RachioDevice(jsonDev));
                logger.trace("RachioApi: Device '{}' added.", id);
            } // for
            return true;
        } catch (Exception e) {
            logger.error("RachioApi.initializeDevices: Exception: {}", e.getMessage());
        }
        logger.error("RachioApi.initializeDevices: Initialization failed");
        return false;
    } // initializeDevices()

    public Boolean initializeZones() {
        return true;
    } // initializeZones()

    public boolean initializeWebHook() {
        try {
            // 1st check the list of available webhook events
            String jsonEventlist = api.sendHttpGet(APIURL_BASE + APIURL_NOT_GET_LIST, null);
            decodeEventTypes(jsonEventlist);
            return true;
        } catch (Exception e) {
            logger.error("RachioApi.registerWebHook: Exception: {}", e.getMessage());
        }
        logger.debug("RachioApi.registerWebHook: Initialization failed");
        return false;
    }

    public boolean decodeEventTypes(String jsonEvents) {
        logger.trace("RachioWebHook: API event types:");
        try {
            JsonParser jsonParser = new JsonParser();
            JsonElement jelement = jsonParser.parse(jsonEvents);
            JsonArray jarray = jelement.getAsJsonArray();
            wh_eventTypes = new HashMap<String, Integer>();
            for (int i = 0; i < jarray.size(); i++) {
                String json = jarray.get(i).toString();
                String key = org.openhab.binding.rachio.internal.util.Parse.jsonString(json, "name", "");
                Integer id = org.openhab.binding.rachio.internal.util.Parse.jsonInt(json, "id", 0);
                logger.trace("  id:{}, type={}", id, key);
                wh_eventTypes.put(key, id);
            }
            return true;
        } catch (Exception e) {
            logger.error("RachioWebHook: Unable to read event types: {}", e.getMessage());
        }
        return false;
    } // decodeEventTypes

    public int getEventId(String Event) {
        try {
            return wh_eventTypes.get(Event);

        } catch (Exception e) {
            logger.error("RachioWebHook: Unable to map event type '{}' to id!: {}", Event, e.getMessage());
            return -1;
        }
    }

    public Map<String, String> fillProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put(Thing.PROPERTY_VENDOR, RachioBindingConstants.BINDING_VENDOR);
        properties.put(RachioBindingConstants.PROPERTY_APIKEY, apiKey);
        properties.put(RachioBindingConstants.PROPERTY_PERSON_ID, personId);
        properties.put(RachioBindingConstants.PROPERTY_PERSON_USER, userName);
        properties.put(RachioBindingConstants.PROPERTY_PERSON_NAME, fullName);
        properties.put(RachioBindingConstants.PROPERTY_PERSON_EMAIL, email);
        return properties;
    }

} // class

/*
 * WebHook Event Data
 * [
 * {"createDate":1417885403247,
 * "lastUpdateDate":1417885403247,
 * "id":5,
 * "name":"DEVICE_STATUS",
 * "description":"Device status event has occurred",
 * "type":"WEBHOOK"
 * },
 * {"createDate":1417885403299,
 * "lastUpdateDate":1417885403299,
 * "id":6,"name":
 * "RAIN_DELAY",
 * "description":"A rain delay event has occurred",
 * "type":"WEBHOOK"
 * },
 * {"createDate":1417885403356,
 * "lastUpdateDate":1417885403356,
 * "id":7,
 * "name":"WEATHER_INTELLIGENCE",
 * "description":"A weather intelligence event has has occurred",
 * "type":"WEBHOOK"
 * },
 * {"createDate":1417885403407,
 * "lastUpdateDate":1417885403407,
 * "id":8,
 * "name":"WATER_BUDGET",
 * "description":"A water budget event has occurred",
 * "type":"WEBHOOK"
 * },
 * {"createDate":1417885403465,
 * "lastUpdateDate":1417885403465,
 * "id":9,
 * "name":"SCHEDULE_STATUS",
 * "description":"A schedule status event has occurred",
 * "type":"WEBHOOK"
 * },
 * {"createDate":1417885403520,
 * "lastUpdateDate":1417885403520,
 * "id":10,
 * "name":"ZONE_STATUS",
 * "description":"A zone status event has occurred",
 * "type":"WEBHOOK"
 * },
 * {"createDate":1417885403576,
 * "lastUpdateDate":1417885403576,
 * "id":11,
 * "name":"RAIN_SENSOR_DETECTION",
 * "description":"A physical rain sensor event has occurred",
 * "type":"WEBHOOK"
 * },
 * {"createDate":1417885403627,
 * "lastUpdateDate":1417885403627,
 * "id":12,
 * "name":"ZONE_DELTA",
 * "description":"A zone has been updated due to water budget",
 * "type":"WEBHOOK"
 * },
 * {"createDate":1424124175588,
 * "lastUpdateDate":1424124175588,
 * "id":14,
 * "name":"DELTA",
 * "description":"An entity has been inserted, updated, or deleted",
 * "type":"WEBHOOK"
 * }
 * ]
 *
 */