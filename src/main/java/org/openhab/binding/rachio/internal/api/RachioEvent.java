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
    public String correlationId = "";
    public String deviceId = "";
    public String id = "";

    public String timeZone = "";
    public String timestamp = "";
    public String timeForSummary = "";
    public String endTime = "";

    public long eventDate = -1;
    public long createDate = -1;
    public long lastUpdateDate = -1;
    public int sequence = -1;

    public String eventType = "";
    public String category = "";
    public String type = "";
    public String subType = "";
    public String topic = "";
    public String summary = "";
    public String description = "";
    public String pushTitle = "";

    public String icon = "";
    public String iconUrl = "";
    public String pin = "";

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

    // public JsonArray eventDatas;
    // public HashMap<String, String> eventParms;
    // public HashMap<String, RachioEventProperty> deltaProperties;

    public Integer rateLimit = 0;
    public Integer rateRemaining = 0;
    public String rateReset = "";

    RachioEvent() {
        // eventDatas = new JsonArray();
    }

    /*
     * public void setEventParms() {
     * try {
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
     * } // setEventParms()
     */
    public void setRateLimit(int rateLimit, int rateRemaining, String rateReset) {
        this.rateLimit = rateLimit;
        this.rateRemaining = rateRemaining;
        this.rateReset = rateReset;
    }

    public void setRateLimit(String rateLimit, String rateRemaining, String rateReset) {

        if (rateLimit != null) {
            this.rateLimit = Integer.parseInt(rateLimit);
        }
        if (rateRemaining != null) {
            this.rateRemaining = Integer.parseInt(rateRemaining);
        }
        if (rateReset != null) {
            this.rateReset = rateReset;
        }
    }

    public boolean checkRateLimit() {

        if ((rateLimit == 0) || (rateRemaining == 0)) {
            return true;
        }
        if (rateRemaining > 200) {
            logger.trace("RachioApi: Remaing number of API: limit={}, remaining={}, reset at {}", rateLimit,
                    rateRemaining, rateReset);
            return true;
        }
        logger.error("RachioApi: Remaing number of API calls is critical: limit={}, remaining={}, reset at {}",
                rateLimit, rateRemaining, rateReset);
        return false;
    }
} // class RachioEvent
