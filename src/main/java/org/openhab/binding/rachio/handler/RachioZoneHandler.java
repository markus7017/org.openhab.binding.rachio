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
package org.openhab.binding.rachio.handler;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.rachio.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.binding.rachio.internal.api.RachioEvent;
import org.openhab.binding.rachio.internal.api.RachioZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioZoneHandler} is responsible for handling commands, which are
 * sent to one of the zone channels.
 *
 * @author Markus Michels
 */

@NonNullByDefault
public class RachioZoneHandler extends BaseThingHandler implements RachioStatusListener {
    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);
    @Nullable
    private RachioBridgeHandler cloudHandler;
    @Nullable
    Bridge bridge;
    @Nullable
    private RachioDevice dev;
    @Nullable
    private RachioZone zone;
    private Map<String, State> channelData = new HashMap<>();

    public RachioZoneHandler(Thing thing) {
        super(thing);
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        logger.debug("RachioZone: Initializing zone '{}'", this.getThing().getUID().toString());

        try {
            // initialize class objects
            bridge = getBridge();
            if (bridge != null) {
                ThingHandler handler = bridge.getHandler();
                if ((handler != null) && (handler instanceof RachioBridgeHandler)) {
                    cloudHandler = (RachioBridgeHandler) handler;
                    zone = cloudHandler.getZoneByUID(this.getThing().getUID());
                    if (zone != null) {
                        zone.setThingHandler(this);
                        dev = cloudHandler.getDevByUID(zone.getDevUID());
                    }
                }
            }
            if ((bridge == null) || (cloudHandler == null) || (dev == null) || (zone == null)) {
                logger.debug("RachioZone: Thing initialisation failed!");
            } else {
                // listen to bridge events
                cloudHandler.registerStatusListener(this);
                if (bridge.getStatus() != ThingStatus.ONLINE) {
                    logger.debug("Rachio: Bridge is offline!");
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                } else {
                    updateProperties();
                    updateStatus(dev.getStatus());
                    return;
                }
            }
        } catch (Exception e) {
            logger.error("RachioZone: Initialisation failed: {}", e.getMessage());
        }

        updateStatus(ThingStatus.OFFLINE);
    } // initialize()

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        /*
         * Note: if communication with thing fails for some reason,
         * indicate that by setting the status with detail information
         * updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
         */
        String channel = channelUID.getId();
        logger.debug("RachioZone.handleCommand {} for {}", command.toString(), channelUID.getAsString());
        if ((cloudHandler == null) || (zone == null)) {
            logger.debug("RachioZone: Cloud handler or device not initialized!");
            return;
        }

        String errorMessage = "";
        try {
            if (command == RefreshType.REFRESH) {
                // cloudHandler.refreshDeviceStatus();
                postChannelData();
            } else if (channel.equals(RachioBindingConstants.CHANNEL_ZONE_ENABLED)) {
                if (command instanceof OnOffType) {
                    if (command == OnOffType.ON) {
                    } else {
                    }
                }
            } else if (channel.equals(RachioBindingConstants.CHANNEL_ZONE_RUN)) {
                if (command instanceof OnOffType) {
                    if (command == OnOffType.ON) {
                        int runtime = zone.getStartRunTime();
                        logger.info("RachioZone: Starting zone '{} [{}]' for {} secs", zone.name, zone.zoneNumber,
                                runtime);
                        if (runtime == 0) {
                            runtime = cloudHandler.getDefaultRuntime();
                            logger.debug("RachioZone: No specific runtime selected, using default ({} secs);", runtime);
                        }
                        cloudHandler.startZone(zone.id, runtime);
                    } else {
                        logger.info("RachioZone: Stop watering for the device");
                        cloudHandler.stopWatering(dev.id);
                    }
                } else {
                    logger.debug("RachioZone: command value for {} is no OnOffType: {}", channel, command);
                }
            } else if (channel.equals(RachioBindingConstants.CHANNEL_ZONE_RUN_TIME)) {
                if (command instanceof DecimalType) {
                    int runtime = ((DecimalType) command).intValue();
                    logger.info("RachioZone: Zone will start for {} sec", runtime);
                    zone.setStartRunTime(runtime);
                } else {
                    logger.debug("RachioZone: command value is no DecimalType: {}", command);
                }
            }
        } catch (RachioApiException e) {
            errorMessage = e.toString();
        } catch (Throwable e) {
            errorMessage = e.getMessage();
        } finally {
            if (!errorMessage.isEmpty()) {
                logger.error("RachioZoneHandler: {}", errorMessage);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
            }
        }
    } // handleCommand()

    @SuppressWarnings("null")
    @Override
    public boolean onThingStateChangedl(@Nullable RachioDevice updatedDev, @Nullable RachioZone updatedZone) {
        if ((updatedZone != null) && (zone != null) && zone.id.equals(updatedZone.id)) {
            logger.debug("RachioZone: Update for zone '{}' received.", zone.id);
            zone.update(updatedZone);
            postChannelData();
            updateStatus(dev.getStatus());
            return true;
        }
        return false;
    }

    @SuppressWarnings("null")
    public boolean webhookEvent(RachioEvent event) {
        boolean update = false;
        try {
            zone.setEvent(event);

            String zoneName = event.zoneName;
            if (event.type.equals("ZONE_STATUS")) {
                if (event.zoneRunStatus.state.equals("STARTED")) {
                    logger.info("RachioZone[{}]: '{}' STARTED watering ({}).", zone.zoneNumber, zoneName,
                            event.timestamp);
                    updateState(RachioBindingConstants.CHANNEL_ZONE_RUN, OnOffType.ON);
                } else if (event.subType.equals("ZONE_STOPPED") || event.subType.equals("ZONE_COMPLETED")) {
                    logger.info(
                            "RachioZone[{}]: '{}' STOPPED watering (timestamp={}, current={}, duration={}sec/{}min, flowVolume={}).",
                            zone.zoneNumber, zoneName, event.timestamp, event.zoneCurrent, event.duration,
                            event.durationInMinutes, event.flowVolume);
                    updateState(RachioBindingConstants.CHANNEL_ZONE_RUN, OnOffType.OFF);
                } else {
                    logger.info("RachioZone: Event for zone[{}] '{}': {} (status={}, duration = {}sec)",
                            zone.zoneNumber, event.zoneName, event.summary, event.zoneRunStatus.state, event.duration);
                }
                update = true;
            } else if (event.subType.equals("ZONE_DELTA")) {
                logger.info("RachioZone: DELTA Event for zone#{} '{}': {}.{}", zone.zoneNumber, zone.name,
                        event.category, event.action);
                update = true;
            } else {
                logger.debug("RachioZone: Unhandled event type '{}_{}' for zone '{}'", event.type, event.subType,
                        zoneName);
            }

            if (update) {
                postChannelData();
            }
        } catch (Throwable e) {
            logger.error("RachioZone: Unable to process event: {}", e.getMessage());
        }

        return update;
    } // webhookEvent()

    // @Override
    @SuppressWarnings("null")
    public void postChannelData() {
        if (zone != null) {
            updateChannel(RachioBindingConstants.CHANNEL_ZONE_NAME, new StringType(zone.name));
            updateChannel(RachioBindingConstants.CHANNEL_ZONE_NUMBER, new DecimalType(zone.zoneNumber));
            updateChannel(RachioBindingConstants.CHANNEL_ZONE_ENABLED, zone.getEnabled());
            updateChannel(RachioBindingConstants.CHANNEL_ZONE_RUN, OnOffType.OFF);
            updateChannel(RachioBindingConstants.CHANNEL_ZONE_RUN_TIME, new DecimalType(zone.getStartRunTime()));
            updateChannel(RachioBindingConstants.CHANNEL_ZONE_RUN_TOTAL, new DecimalType(zone.runtime));
            updateChannel(RachioBindingConstants.CHANNEL_ZONE_IMAGEURL, new StringType(zone.imageUrl));
            updateChannel(RachioBindingConstants.CHANNEL_ZONE_EVENT, new StringType(zone.getEvent()));
            // updateChannel(RachioBindingConstants.CHANNEL_ZONE_AVL_WATER, new DecimalType(zone.getAvlWater()));
            // updateChannel(RachioBindingConstants.CHANNEL_ZONE_ROOT_DEPTH, new DecimalType(zone.getRootDepth()));
            // updateChannel(RachioBindingConstants.CHANNEL_ZONE_EFFICIENCY, new DecimalType(zone.getEfficiency()));
            // updateChannel(RachioBindingConstants.CHANNEL_ZONE_YARD_SQFT, new DecimalType(zone.getYardAreaSqft()));
            // updateChannel(RachioBindingConstants.CHANNEL_ZONE_WATHER_DEPTH, new DecimalType(zone.getWaterDepth()));
            // updateChannel(RachioBindingConstants.CHANNEL_ZONE_NZ_IMGURL, new StringType(zone.getNozzleImageUrl()));
            // updateChannel(RachioBindingConstants.CHANNEL_ZONE_NZ_HEADCAT, new StringType(zone.getNozzleHeadCat()));
            // updateChannel(RachioBindingConstants.CHANNEL_ZONE_NZ_IPH, new
            // DecimalType(zone.getNozzleInchesPerHour()));

        }
    } // postChannelData()

    @SuppressWarnings({ "null", "unused" })
    private boolean updateChannel(String channelName, State newValue) {
        State currentValue = channelData.get(channelName);
        if ((currentValue != null) && currentValue.equals(newValue)) {
            // no update required
            return false;
        }

        if (currentValue == null) {
            // new value -> update
            channelData.put(channelName, newValue);
        } else {
            // value changed -> update
            channelData.replace(channelName, newValue);

        }

        updateState(channelName, newValue);
        return true;
    }

    @SuppressWarnings("null")
    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);

        logger.trace("RachioZoneHandler: Bridge Status changed to {}", bridgeStatusInfo.getStatus());
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            updateProperties();
            updateStatus(dev.getStatus());
            postChannelData();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    } // bridgeStatusChanged()

    public void shutdown() {
        updateStatus(ThingStatus.OFFLINE);
    }

    @SuppressWarnings("null")
    private void updateProperties() {
        if ((cloudHandler != null) && (zone != null)) {
            logger.trace("Updating Rachio zone properties");
            Map<String, String> prop = zone.fillProperties();
            if (prop != null) {
                updateProperties(prop);
            } else {
                logger.debug("RachioZone: Unable to update properties for Thing!");
                return;
            }
        }
    } // updateProperties
} // class RachioZoneHandler
