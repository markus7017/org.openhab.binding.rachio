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

    // List of non-standard Properties
    public static final String PROPERTY_APIKEY = "apikey";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_MODEL = "model";
    public static final String PROPERTY_EXT_ID = "externalId";
    public static final String PROPERTY_DEV_ID = "deviceId";
    public static final String PROPERTY_ZONE_ID = "zoneId";
    public static final String PROPERTY_PERSON_ID = "personId";
    public static final String PROPERTY_PERSON_USER = "userName";
    public static final String PROPERTY_PERSON_NAME = "accountFullName";
    public static final String PROPERTY_PERSON_EMAIL = "accountEMail";

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

    public static final String WEBHOOK_PATH = "/rachio/webhook";
    public static final String WEBHOOK_APPLICATION_JSON = "application/json";
    public static final String WEBHOOK_CHARSET = "utf-8";
    public static final String WEBHOOK_USER_AGENT = "Mozilla/5.0";

    public static final String RACHIO_JSON_RATE_LIMIT = "X-RateLimit-Limit";
    public static final String RACHIO_JSON_RATE_REMAINING = "X-RateLimit-Remaining";
    public static final String RACHIO_JSON_RATE_RESET = "X-RateLimit-Reset";

    public static int API_RATE_TRESHHOLD = 200; // if we have than those remaining api calls, polling stops
    public static int API_SKIP_RATE = 10; // number of calls to skip if remaining calls are < treshhold
}
