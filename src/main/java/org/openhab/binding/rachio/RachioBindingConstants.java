/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * @author Markus Michels (markus7017), Hans-Jörg Merk (hmerk) - Initial contribution
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.rachio;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link RachioBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Markus Michels (markus7017) - Initial contribution
 */
@NonNullByDefault
public class RachioBindingConstants {

    public static final String BINDING_ID = "rachio";
    public static final String BINDING_VENDOR = "Rachio";

    public static int BINDING_DISCOVERY_TIMEOUT = 60;
    public static int PORT_REFRESH_INTERVAL = 60;

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_CLOUD = new ThingTypeUID(BINDING_ID, "cloud");
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");
    public static final ThingTypeUID THING_TYPE_ZONE = new ThingTypeUID(BINDING_ID, "zone");

    public static final Set<ThingTypeUID> SUPPORTED_BRIDGE_THING_TYPES_UIDS = Stream.of(THING_TYPE_CLOUD)
            .collect(Collectors.toSet());
    public static final Set<ThingTypeUID> SUPPORTED_DEVICE_THING_TYPES_UIDS = Stream
            .of(THING_TYPE_DEVICE, THING_TYPE_ZONE).collect(Collectors.toSet());
    public static final Set<ThingTypeUID> SUPPORTED_ZONE_THING_TYPES_UIDS = Stream.of(THING_TYPE_ZONE)
            .collect(Collectors.toSet());
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Stream
            .concat(SUPPORTED_BRIDGE_THING_TYPES_UIDS.stream(), SUPPORTED_DEVICE_THING_TYPES_UIDS.stream())
            .collect(Collectors.toSet());

    // Config opntions (e.g. rachio.cfg)
    public static final String PARAM_APIKEY = "apikey";
    public static final String PARAM_POLLING_INTERVAL = "pollingInterval";
    public static final String PARAM_DEF_RUNTIME = "defaultRuntime";
    public static final String PARAM_CALLBACK_URL = "callbackUrl";
    public static final String PARAM_CLEAR_CALLBACK = "clearAllCallbacks";
    public static final String PARAM_IPFILTER = "ipFilter";

    // List of non-standard Properties
    public static final String PROPERTY_APIKEY = PARAM_APIKEY;
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_MODEL = "model";
    public static final String PROPERTY_EXT_ID = "externalId";
    public static final String PROPERTY_DEV_ID = "deviceId";
    public static final String PROPERTY_ZONE_ID = "zoneId";
    public static final String PROPERTY_PERSON_ID = "personId";
    public static final String PROPERTY_PERSON_USER = "accounUserName";
    public static final String PROPERTY_PERSON_NAME = "accountFullName";
    public static final String PROPERTY_PERSON_EMAIL = "accountEMail";

    // Default for config options / thing settings
    public static int DEFAULT_HTTP_TIMEOUT = 15 * 1000;
    public static int DEFAULT_POLLING_INTERVAL = 120;
    public static int DEFAULT_ZONE_RUNTIME = 300;

    // List of all Device Channel ids
    public static final String CHANNEL_DEVICE_NAME = "name";
    public static final String CHANNEL_DEVICE_ACTIVE = "active";
    public static final String CHANNEL_DEVICE_ONLINE = "online";
    public static final String CHANNEL_DEVICE_PAUSED = "paused";
    public static final String CHANNEL_DEVICE_RUN = "run";
    public static final String CHANNEL_DEVICE_RUN_ZONES = "runZones";
    public static final String CHANNEL_DEVICE_RUN_TIME = "runTime";
    public static final String CHANNEL_DEVICE_STOP = "stop";
    public static final String CHANNEL_DEVICE_EVENT = "devEvent";
    public static final String CHANNEL_DEVICE_LATITUDE = "latitude";
    public static final String CHANNEL_DEVICE_LONGITUDE = "longitude";
    public static final String CHANNEL_DEVICE_ELEVATION = "elevation";
    public static final String CHANNEL_DEVICE_RAIN_DELAY = "rainDelay";

    // List of all Zone Channel ids
    public static final String CHANNEL_ZONE_NAME = "name";
    public static final String CHANNEL_ZONE_NUMBER = "number";
    public static final String CHANNEL_ZONE_ENABLED = "enabled";
    public static final String CHANNEL_ZONE_RUN = "run";
    public static final String CHANNEL_ZONE_RUN_TIME = "runTime";
    public static final String CHANNEL_ZONE_RUN_TOTAL = "runTotal";
    public static final String CHANNEL_ZONE_EVENT = "zoneEvent";
    public static final String CHANNEL_ZONE_IMAGEURL = "imageUrl";
    // public static final String CHANNEL_ZONE_AVL_WATER = "avlWater";
    // public static final String CHANNEL_ZONE_ROOT_DEPTH = "rootDepth";
    // public static final String CHANNEL_ZONE_EFFICIENCY = "efficiency";
    // public static final String CHANNEL_ZONE_YARD_SQFT = "yardSqft";
    // public static final String CHANNEL_ZONE_WATHER_DEPTH = "watherDepth";
    // public static final String CHANNEL_ZONE_NZ_HEADCAT = "nozzleHeadCat";
    // public static final String CHANNEL_ZONE_NZ_IPH = "nozzleIph";
    // public static final String CHANNEL_ZONE_NZ_IMGURL = "nozzleImageUrl";

    // --------------- Rachio Cloud API
    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_PUT = "PUT";
    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_DELETE = "DELETE";
    public static final int HTTP_TIMOUT = 15000;

    public static final String APIURL_BASE = "https://api.rach.io/1/public/";

    public static final String APIURL_GET_PERSON = "person/info"; // obtain personId
    public static final String APIURL_GET_PERSONID = "person"; // obtain personId
    public static final String APIURL_GET_DEVICE = "device"; // get device details, needs /<device id>

    public static final String APIURL_DEV_PUT_ON = "device/on"; // Enable device / all functions
    public static final String APIURL_DEV_PUT_OFF = "device/off"; // Disable device / all functions
    public static final String APIURL_DEV_PUT_STOP = "device/stop_water"; // stop watering (all zones)
    public static final String APIURL_DEV_PUT_RAIN_DELAY = "device/rain_delay"; // Rain delay device
    public static final String APIURL_DEV_POST_WEBHOOK = "notification/webhook"; // Register WebHook for Device
    public static final String APIURL_DEV_QUERY_WEBHOOK = "notification"; // completes to
                                                                          // /public/notification/:deviceId/webhook
    public static final String APIURL_DEV_DELETE_WEBHOOK = "notification/webhook";

    public static final String APIURL_ZONE_PUT_START = "zone/start"; // start a zone
    public static final String APIURL_ZONE_PUT_MULTIPLE_START = "zone/start_multiple"; // start multiple zones

    // private static final String APIURL_NOT_GET_LIST = "notification/webhook_event_type"; // get list of available

    // notification types
    // WebHook event types
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
    public static final String WHE_DEVICE_STATUS = "5"; // "Device status event has occurred"
    public static final String WHE_RAIN_DELAY = "6"; // "A rain delay event has occurred"
    public static final String WEATHER_INTELLIGENCE = "7"; // A weather intelligence event has has occurred
    public static final String WHE_WATER_BUDGET = "8"; // A water budget event has occurred
    public static final String WHE_SCHEDULE_STATUS = "9";
    public static final String WHE_ZONE_STATUS = "10";
    public static final String WHE_RAIN_SENSOR_DETECTION = "11"; // physical rain sensor event has coccurred
    public static final String WHE_ZONE_DELTA = "12"; // A physical rain sensor event has occurred
    public static final String WHE_DELTA = "14"; // "An entity has been inserted, updated, or deleted"

    public static final String WEBHOOK_PATH = "/rachio/webhook";
    public static final String WEBHOOK_APPLICATION_JSON = "application/json";
    public static final String WEBHOOK_CHARSET = "utf-8";
    public static final String WEBHOOK_USER_AGENT = "Mozilla/5.0";

    public static final String RACHIO_JSON_RATE_LIMIT = "X-RateLimit-Limit";
    public static final String RACHIO_JSON_RATE_REMAINING = "X-RateLimit-Remaining";
    public static final String RACHIO_JSON_RATE_RESET = "X-RateLimit-Reset";
    public static final int RACHIO_RATE_LIMIT_WARNING = 200; // slow down polling
    public static final int RACHIO_RATE_LIMIT_CRITICAL = 100; // stop polling
    public static final int RACHIO_RATE_LIMIT_BLOCK = 20; // block api access
    public static final int RACHIO_RATE_SKIP_CALLS = 5;

    public static final String AWS_IPADDR_DOWNLOAD_URL = "https://ip-ranges.amazonaws.com/ip-ranges.json";
    public static final String AWS_IPADDR_REGION_FILTER = "us-";
}
