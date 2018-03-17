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
package org.openhab.binding.rachio.internal;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

    public String externalId = "";

    public String id = "";
    public long eventDate = -1;
    public long createDate = -1;
    public long lastUpdateDate = -1;
    public int sequence = -1;
    public String correlationId = "";

    public String deviceId = "";
    public String category = "";
    public String type = "";
    public String subType = "";
    public String topic = "";
    public String summary = "";
    public String iconUrl = "";

    public JsonArray eventDatas;
    public HashMap<String, String> eventParms;
    public HashMap<String, RachioEventProperty> deltaProperties;

    RachioEvent() {
        eventDatas = new JsonArray();
    }

    public void setEventParms() {
        try {
            eventParms = new HashMap<String, String>();
            deltaProperties = new HashMap<String, RachioEventProperty>();
            for (int i = 0; i < eventDatas.size(); i++) {
                JsonElement jElement = eventDatas.get(i);
                JsonObject jObject = jElement.getAsJsonObject();
                String key = jObject.get("key").getAsString();
                JsonElement convertedValue = jObject.get("convertedValue");
                if (convertedValue != null) {
                    String value = convertedValue.getAsString();
                    eventParms.put(key, value);
                }
                JsonElement deltaContainer = jObject.get("deltaContainer");
                if (type.equals("DELTA") && (deltaContainer != null)) {
                    JsonObject deltasObject = deltaContainer.getAsJsonObject();
                    JsonElement deltas = deltasObject.get("deltas");
                    if (deltas != null) {
                        JsonArray deltaArray = deltas.getAsJsonArray();
                        for (int dc = 0; dc < deltaArray.size(); dc++) {
                            JsonElement jeProperty = deltaArray.get(dc);
                            JsonObject joProperty = jeProperty.getAsJsonObject();
                            RachioEventProperty evProperty = new RachioEventProperty();
                            evProperty.propertyName = joProperty.get("propertyName").getAsString();
                            evProperty.oldValue = joProperty.get("oldValue").getAsString();
                            evProperty.newValue = joProperty.get("newValue").getAsString();
                            deltaProperties.put(evProperty.propertyName, evProperty);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("RachioEvent: Unable process parms for event '{}': {}", type, e.getMessage());
        }
    }
} // class RachioEvent
