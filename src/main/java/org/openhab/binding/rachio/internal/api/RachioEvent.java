/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * @author Markus Michels (markus7017) - Initial contribution
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.rachio.internal.api;

import java.util.HashMap;

import org.openhab.binding.rachio.internal.api.RachioApi.RachioApiResult;
import org.openhab.binding.rachio.internal.api.RachioCloudDevice.RachioCloudNetworkSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioEvent} A Rachio webhook event
 *
 * @author Markus Michels (markus7017) - Initial contribution
 */
public class RachioEvent {
    private final Logger logger = LoggerFactory.getLogger(RachioEvent.class);

    public class RachioEventProperty {
        public String propertyName;
        public String oldValue;
        public String newValue;
    }

    public class RachioZoneStatus {
        public Integer duration = 0;
        public String scheduleType = "";
        public Integer zoneNumber = 0;
        public String executionType = "";
        public String state = "";
        public String startTime = "";
        public String endTime = "";
        // public Integer corId = 0;
        // public Integer seqId = 0;
        // public Integer ix = 0;
    }

    public String externalId = "";
    public String routingId = "";
    public String connectId = "";
    public String correlationId = "";
    public String scheduleId = "";
    public String deviceId = "";
    public String zoneId = "";
    public String id = "";

    public String timeZone = "";
    public String timestamp = "";
    public String timeForSummary = "";
    public String endTime = "";

    public long eventDate = -1;
    public long createDate = -1;
    public long lastUpdateDate = -1;
    public int sequence = -1;
    public String status = ""; // COLD_REBOOT: "status" : "coldReboot",

    /*
     * type : DEVICE_STATUS
     *
     * Subtype:
     *
     * OFFLINE
     * ONLINE
     * OFFLINE_NOTIFICATION
     * COLD_REBOOT
     * SLEEP_MODE_ON
     * SLEEP_MODE_OFF
     * BROWNOUT_VALVE
     * RAIN_SENSOR_DETECTION_ON
     * RAIN_SENSOR_DETECTION_OFF
     * RAIN_DELAY_ON
     * RAIN_DELAY_OFF
     *
     * Type : SCHEDULE_STATUS
     *
     * Subtype:
     *
     * SCHEDULE_STARTED
     * SCHEDULE_STOPPED
     * SCHEDULE_COMPLETED
     * WEATHER_INTELLIGENCE_NO_SKIP
     * WEATHER_INTELLIGENCE_SKIP
     * WEATHER_INTELLIGENCE_CLIMATE_SKIP
     * WEATHER_INTELLIGENCE_FREEZE
     *
     * Type : ZONE_STATUS
     *
     * Subtype:
     *
     * ZONE_STARTED
     * ZONE_STOPPED
     * ZONE_COMPLETED
     * ZONE_CYCLING
     * ZONE_CYCLING_COMPLETED
     *
     * Type : DEVICE_DELTA
     * Subtype : DEVICE_DELTA
     *
     * Type : ZONE_DELTA
     * Subtype : ZONE_DELTA
     *
     * Type : SCHEDULE_DELTA
     * Subtype : SCHEDULE_DELTA
     */
    public String type = "";
    public String subType = "";
    public String eventType = "";
    public String category = "";
    public String topic = "";
    public String action = "";
    public String summary = "";
    public String description = "";
    public String title = "";
    public String pushTitle = "";

    public String icon = "";
    public String iconUrl = "";

    // ZONE_STATUS
    public Integer zoneNumber = 0;
    public String zoneName = "";
    public Integer zoneCurrent = 0;
    public String zoneRunState = "";
    public Integer duration = 0;
    public Integer durationInMinutes = 0;
    public Integer flowVolume = 0;
    public RachioZoneStatus zoneRunStatus;

    // SCHEDULE_STATUS
    public String scheduleName = "";
    public String scheduleType = "";

    // COLD_REBOOT
    public String deviceName = ""; // "deviceName" : "Rachio Turtle Pine House",
    RachioCloudNetworkSettings network; // "network" : {"gw" : "192.168.1.1", "rssi" : -61, "dns2" : "75.75.76.76",
                                        // "dns1" : "75.75.75.75", "ip" : "192.168.1.112", "nm" : "255.255.255.0" }
    String pin = "";

    public RachioApiResult apiResult = new RachioApiResult();

    // public JsonArray eventDatas;
    public HashMap<String, String> eventParms;
    public HashMap<String, RachioEventProperty> deltaProperties;

    public RachioEvent() {
        // eventDatas = new JsonArray();
    }

    // public void setEventParms() {
    /*
     * No longer used for APIv3
     *
     * try {
     *
     * eventParms = new HashMap<String, String>();
     * deltaProperties = new HashMap<String, RachioEventProperty>();
     * for (int i = 0; i < eventDatas.size(); i++) {
     * JsonElement jElement = eventDatas.get(i);
     * JsonObject jObject = jElement.getAsJsonObject();
     * String key = jObject.get("key").getAsString();
     * JsonElement convertedValue = jObject.get("convertedValue");
     * if (convertedValue != null) {
     * String value = convertedValue.getAsString();
     * eventParms.put(key, value);
     * }
     * JsonElement deltaContainer = jObject.get("deltaContainer");
     * if (type.equals("DELTA") && (deltaContainer != null)) {
     * JsonObject deltasObject = deltaContainer.getAsJsonObject();
     * JsonElement deltas = deltasObject.get("deltas");
     * if (deltas != null) {
     * JsonArray deltaArray = deltas.getAsJsonArray();
     * for (int dc = 0; dc < deltaArray.size(); dc++) {
     * JsonElement jeProperty = deltaArray.get(dc);
     * JsonObject joProperty = jeProperty.getAsJsonObject();
     * RachioEventProperty evProperty = new RachioEventProperty();
     * evProperty.propertyName = joProperty.get("propertyName").getAsString();
     * evProperty.oldValue = joProperty.get("oldValue").getAsString();
     * evProperty.newValue = joProperty.get("newValue").getAsString();
     * deltaProperties.put(evProperty.propertyName, evProperty);
     * }
     * }
     * }
     * }
     * } catch (Exception e) {
     * logger.error("RachioEvent: Unable process parms for event '{}': {}", type, e.getMessage());
     * }
     */
    // } // setEventParms()

} // class RachioEvent
