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

import java.util.ArrayList;

/**
 *
 * @author Markus Michels (markus7017) - Initial contribution
 */
class RachioCloudDevice {
    public long createDate = -1; // "createDate":1494626927000,
    public String id = ""; // "id":"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
    public String status = ""; // "status":"ONLINE",
    public ArrayList<RachioCloudZone> zones = new ArrayList<>();
    public double latitude = 0.0; // "latitude":26.928539276123,
    public double longitude = 0.0; // "longitude":-82.3335037231445,
    public String name = ""; // "name":"Rachio Turtle Pine House",
    public ArrayList<RachioCloudScheduleRule> scheduleRules = new ArrayList<>(); // "scheduleRules":[]
    public boolean cycleSoak = false; // "cycleSoak":false}
    public String serialNumber = ""; // "serialNumber":"VR0547710",
    public long rainDelayExpirationDate = 0; // "rainDelayExpirationDate":0,
    public String macAddress = ""; // "macAddress":"XXXXXXXXXXXX",
    public boolean on = true; // "on":true,
    public ArrayList<RachioCloudScheduleRule> flexScheduleRules = new ArrayList<>(); // "flexScheduleRules":[],
    public String model = ""; // "model":"GENERATION2_8ZONE",
    public String scheduleModeType = ""; // "scheduleModeType":"MANUAL",
    public boolean deleted = false; // "deleted":false,
    public boolean homeKitCompatible = false; // "homeKitCompatible":false

    public class RachioCloudScheduleRule {
        public String id = ""; // "id":"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
        public ArrayList<RachioCloudScheduleRuleZone> zones = new ArrayList<>(); // "zones":[]
        public ArrayList<String> scheduleJobTypes = new ArrayList<>(); // "scheduleJobTypes":["DAY_OF_WEEK_6","DAY_OF_WEEK_1","DAY_OF_WEEK_4"],
        public int startHour = 0; // "startHour":2,
        public int startMinute = 0; // "startMinute":0,
        public String operator = ""; // "operator":"AFTER",
        public String cycleSoakStatus = ""; // "cycleSoakStatus":"OFF",
        public long startDate = 0; // "startDate":1508788871213,
        public String name = ""; // "name":"Water all zones",
        public boolean enabled = true; // "enabled":true,
        public int startDay = 0; // "startDay":23,
        public int startMonth = 0; // "startMonth":9,
        public int startYear = 0; // "startYear":2017,
        public int totalDuration = 0; // "totalDuration":7211,
        public long endDate = -1; // "endDate":-62167406328787,
        public boolean etSkip = true; // "etSkip":true,
        public String externalName = ""; // "externalName":"Water all zones",
        public String type = ""; // "type":"FLEX"
        public boolean cycleSoak = false;
    }

    public class RachioCloudScheduleRuleZone {
        public String zoneId = ""; // "zoneId":"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
        public int duration = 0; // "duration":1211,
        public int sortOrder = 0; // "sortOrder":2
    }

    public class RachioCloudScheduleStatus {
        public String scheduleName = ""; // "scheduleName" : "Quick Run",
        public String routingId = ""; // "routingId" : "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
        public String icon = ""; // "icon" : "SCHEDULE",
        public String description = ""; // "description" : "Quick Run ran for 2 minutes.",
        public String endTimeForSummary = ""; // "endTimeForSummary" : "06:11 PM (EDT)",
        public String type = ""; // "type" : "SCHEDULE_STATUS",
        public String title = ""; // "title" : "Schedule Completed",
        public String deviceId = ""; // "deviceId" : "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
        public int duration = 0; // "duration" : 120,
        public String pushTitle = ""; // "pushTitle" : "Schedule Completed",
        public String startTime = ""; // "startTime" : "2018-04-09T22:09:07.134Z",
        public String id = ""; // "id" : "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
        public String iconUrl = ""; // "iconUrl" : "https://media.rach.io/v3_prod_event_icons/quick_run",
        public String timestamp = ""; // "timestamp" : "2018-04-09T22:11:11Z",
        public String summary = ""; // "summary" : "Quick Run ran for 2 minutes.",
        public int durationInMinutes = 0; // "durationInMinutes" : 2,
        public String timeZone = ""; // "timeZone" : "America/New_York",
        public String externalId = ""; // "externalId" : "xxxxxxx-xxxx-xxxx-xxxxx-´xxxxxxxx",
        public String timeForSummary = ""; // "timeForSummary" : "06:09 PM (EDT)",
        public String eventType = ""; // eventType" : "SCHEDULE_COMPLETED_EVENT",
        public String dateForSummary = ""; // "dateForSummary" : "4/09",
        public String scheduleType = ""; // "scheduleType" : "FIXED",
        public String subType = ""; // "subType" : "SCHEDULE_COMPLETED",
        public String endTime = ""; // "endTime" : "2018-04-09T22:11:07.134Z",
        public String category = ""; // "category" : "SCHEDULE"
    }

    public class RachioCloudRainDelay {
        public String summary = ""; // "summary" : "Scheduled waterings will now run on controller Rachio Turtle Pine
                                    // House.",
        public String routingId = ""; // "routingId" : "d3beb3ab-b85a-49fe-a45d-37c4d95ea9a8",
        public String icon = ""; // "icon" : "SCHEDULE",
        public String timeZone = ""; // "timeZone" : "America/New_York",
        public String externalId = ""; // "externalId" : "cc765dfb-d095-4ceb-8062-b9d88dcce911",
        public String eventType = ""; // "eventType" : "RAIN_DELAY_EVENT",
        public String type = ""; // "type" : "RAIN_DELAY",
        public String title = ""; // "title" : "Delay Watering OFF",
        public String deviceId = ""; // "deviceId" : "d3beb3ab-b85a-49fe-a45d-37c4d95ea9a8",
        public String deviceName = ""; // "deviceName" : "Rachio Turtle Pine House",
        public String dateTimeForSummary = ""; // "dateTimeForSummary" : "Wednesday, December 31 07:00 PM (EST)",
        public String startTime = ""; // "startTime" : "2018-04-10T00:59:49.899Z",
        public String subType = ""; // "subType" : "RAIN_DELAY_OFF",
        public String endTime = ""; // "endTime" : "1970-01-01T00:00:00Z",
        public String id = ""; // "id" : "bc528ada-2054-38d3-982c-6450ebe65c0b",
        public String iconUrl = ""; // "iconUrl" : "https://media.rach.io/v3_prod_event_icons/delay_water",
        public String category = ""; // "category" : "DEVICE",
    }

    public class RachioCloudNetworkSettings {
        // "network" : {
        public String gw = ""; // "gw" : "192.168.1.1",
        public String rssi = ""; // "rssi" : -61,
        public String dns2 = ""; // "dns2" : "75.75.76.76",
        public String dns1 = ""; // "dns1" : "75.75.75.75",
        public String ip = ""; // "ip" : "192.168.1.112",
        public String nm = ""; // "nm" : "255.255.255.0"
        // }
    }

} // class RachioCloudDevice
