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
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.binding.rachio.internal.api.RachioEvent;
import org.openhab.binding.rachio.internal.api.RachioZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioDeviceHandler} is responsible for handling commands, which are
 * sent to one of the device channels.
 *
 * @author Markus Michels (markus7017) - Initial contribution
 */

@NonNullByDefault
public class RachioDeviceHandler extends BaseThingHandler implements RachioStatusListener {
    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);
    @Nullable
    Bridge bridge;
    @Nullable
    RachioBridgeHandler cloudHandler;
    @Nullable
    RachioDevice dev;
    private Map<String, State> channelData = new HashMap<>();

    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        logger.debug("RachioDevice: Initializing Rachio device '{}'.", getThing().getUID().getAsString());

        try {
            bridge = getBridge();
            if (bridge != null) {
                ThingHandler handler = bridge.getHandler();
                if ((handler != null) && (handler instanceof RachioBridgeHandler)) {
                    cloudHandler = (RachioBridgeHandler) handler;
                    dev = cloudHandler.getDevByUID(this.getThing().getUID());
                    dev.setThingHandler(this);
                    cloudHandler.registerStatusListener(this);
                    cloudHandler.registerWebHook(dev.getId());
                    if (bridge.getStatus() != ThingStatus.ONLINE) {
                        logger.debug("Rachio: Bridge is offline!");
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                    } else {
                        if (dev != null) {
                            updateProperties();
                            updateStatus(dev.getStatus());
                            logger.debug("RachioDevice: Rachio device '{}' initialized.",
                                    getThing().getUID().getAsString());
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("RachioDev: Initialization failed: {}", e.getMessage());
        }
        updateStatus(ThingStatus.OFFLINE);
    } // initialize()

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String channel = channelUID.getId();
        if (channel == null) {
            logger.debug("Called with a null channel id - ignoring");
            return;
        }
        logger.debug("RachioDevice.handleCommand {} for channel '{}'", command.toString(), channel);

        if ((cloudHandler == null) || (dev == null)) {
            logger.debug("RachioDevice: Cloud handler or device not initialized!");
            return;
        }

        if (command == RefreshType.REFRESH) {
            if (refreshChannel(channel)) {
                logger.debug("RachioDevice: Channel '{}' was refreshed", channel);
            }

        } else if (channel.equals(RachioBindingConstants.CHANNEL_DEVICE_ACTIVE)) {
            if (command instanceof OnOffType) {
                if (command == OnOffType.OFF) {
                    logger.info("RachioDevice: Pause device '{}' (disable watering, schedules etc.)", dev.getName());
                    cloudHandler.disableDevice(dev.getId());
                } else {
                    logger.info("RachioDevice: Resume device '{}' (enable watering, schedules etc.)", dev.getName());
                    cloudHandler.enableDevice(dev.getId());
                }
            } else {
                logger.debug("RachioDevice: command value is no OnOffType: {}", command);
            }
        } else if (channel.equals(RachioBindingConstants.CHANNEL_DEVICE_RUN_TIME)) {
            if (command instanceof DecimalType) {
                int runtime = ((DecimalType) command).intValue();
                logger.info("RachioDevice: Default Runtime for zones set to {} sec", runtime);
                dev.setRunTime(runtime);
            } else {
                logger.debug("RachioDevice: command value is no DecimalType: {}", command);
            }
        } else if (channel.equals(RachioBindingConstants.CHANNEL_DEVICE_RUN_ZONES)) {
            if (command instanceof StringType) {
                logger.info("RachioDevice: Run multiple zones: '{}' ('' = ALL)", command.toString());
                dev.setRunZones(command.toString());
            } else {
                logger.debug("RachioDevice: command value is no StringType: {}", command);
            }
        } else if (channel.equals(RachioBindingConstants.CHANNEL_DEVICE_RUN)) {
            if (command == OnOffType.ON) {
                logger.info("RachioDevice: START watering zones '{}' ('' = ALL)", dev.getRunZones());
                cloudHandler.runMultipleZones(dev.getAllRunZonesJson(cloudHandler.getDefaultRuntime()));
            }
        } else if (channel.equals(RachioBindingConstants.CHANNEL_DEVICE_STOP)) {
            if (command == OnOffType.ON) {
                logger.info("RachioDevice: STOP watering for device '{}'", dev.getName());
                cloudHandler.stopWatering(dev.getId());
                updateState(RachioBindingConstants.CHANNEL_DEVICE_STOP, OnOffType.OFF);
            }
        } else if (channel.equals(RachioBindingConstants.CHANNEL_DEVICE_RAIN_DELAY)) {
            if (command instanceof DecimalType) {
                logger.info("RachioDevice: Start rain delay cycle for {} sec", command.toString());
                dev.setRainDelay(((DecimalType) command).intValue());
                cloudHandler.startRainDelay(dev.getId(), ((DecimalType) command).intValue());
            } else {
                logger.debug("RachioDevice: command value is no DecimalType: {}", command);
            }
        }
    } // handleCommand()

    @SuppressWarnings("null")
    private void postChannelData() {
        if (dev != null) {
            logger.debug("RachioDevice: Updating  status");
            updateChannel(RachioBindingConstants.CHANNEL_DEVICE_NAME, new StringType(dev.getThingName()));
            updateChannel(RachioBindingConstants.CHANNEL_DEVICE_ONLINE, dev.getOnline());
            updateChannel(RachioBindingConstants.CHANNEL_DEVICE_ACTIVE, dev.getEnabled());
            updateChannel(RachioBindingConstants.CHANNEL_DEVICE_PAUSED, dev.getPaused());
            updateChannel(RachioBindingConstants.CHANNEL_DEVICE_STOP, OnOffType.OFF);
            updateChannel(RachioBindingConstants.CHANNEL_DEVICE_RUN_ZONES, new StringType(dev.getRunZones()));
            updateChannel(RachioBindingConstants.CHANNEL_DEVICE_RUN_TIME, new DecimalType(dev.getRunTime()));
            updateChannel(RachioBindingConstants.CHANNEL_DEVICE_RAIN_DELAY, new DecimalType(dev.getRainDelay()));
            updateChannel(RachioBindingConstants.CHANNEL_DEVICE_LATITUDE, new DecimalType(dev.getLongitude()));
            updateChannel(RachioBindingConstants.CHANNEL_DEVICE_LONGITUDE, new DecimalType(dev.getLatitude()));
            updateChannel(RachioBindingConstants.CHANNEL_DEVICE_ELEVATION, new DecimalType(dev.getElevation()));
            updateChannel(RachioBindingConstants.CHANNEL_DEVICE_EVENT, new StringType(dev.getEvent()));
        }
    }

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

    @SuppressWarnings({ "null", "unused" })
    private boolean refreshChannel(String channelName) {
        State currentValue = channelData.get(channelName);
        if (currentValue != null) {
            updateState(channelName, currentValue);
            return true;
        }
        return false;
    }

    @SuppressWarnings("null")
    @Override
    public boolean onThingStateChangedl(@Nullable RachioDevice updatedDev, @Nullable RachioZone updatedZone) {
        if ((updatedDev != null) && (dev != null) && dev.getId().equals(updatedDev.getId())) {
            logger.debug("RachioDevice: Update for device '{}' received.", dev.getId());
            dev.update(updatedDev);
            postChannelData();
            updateStatus(dev.getStatus());
            return true;
        }
        return false;
    }

    @SuppressWarnings("null")
    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);

        logger.debug("RachioDeviceHandler: Bridge Status changed to {}", bridgeStatusInfo.getStatus());
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            updateProperties();
            postChannelData();
            updateStatus(dev.getStatus());
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    } // bridgeStatusChanged()

    @SuppressWarnings("null")
    public boolean webhookEvent(RachioEvent event) {
        boolean update = false;
        try {
            dev.setEvent(event);
            String etype = event.type;
            if (etype.equals("ZONE_STATUS") /* || event.subType.equals("ZONE_DELTA") */) {
                RachioZone zone = dev.getZoneByNumber(event.zoneRunStatus.zoneNumber);
                if ((zone != null) && (zone.getThingHandler() != null)) {
                    return zone.getThingHandler().webhookEvent(event);
                }
            } else if (etype.equals("DEVICE_STATUS")) {
                logger.info("RachioDevice '{}': Device status updated ({}): {}.", dev.getName(), event.subType,
                        event.summary);
                if (event.subType.equals("COLD_REBOOT")) {
                    logger.info("Rachio device {} was restarted.", dev.getName());
                    update = true;
                }
                // update = true;
            } else if (event.subType.equals("DEVICE_DELTA")) {
                logger.info("RachioDevice '{}': Device DELTA received, status={}.", dev.getName(), event.eventType);
                // update = true;
            } else if (etype.equals("SCHEDULE_STATUS")) {
                logger.info("RachioDevice '{}': schedule'{}' {} (type={}, estimatedDuration = {}sec - {})",
                        dev.getName(), event.scheduleName, event.pushTitle, event.scheduleType, event.duration,
                        event.summary);
                update = true;
            } else {
                logger.debug("RachioDevice '{}': Unhandled event '{}_{}' ({})", event.deviceId, etype, event.subType,
                        event.summary);
            }

            if (update) {
                postChannelData();
            } else {
                logger.debug("RachioDevice: Unhandled event '{}.{}' for device '{}': {}", event.type, event.subType,
                        dev.getName(), event.summary);

            }
        } catch (Exception e) {
            logger.error("RachioDevice: Unable to process event '{}' - {}: {}", event.type, event.summary,
                    e.getMessage());
        }

        return update;
    } // webhookEvent

    @SuppressWarnings("null")
    private void updateProperties() {
        if (dev != null) {
            logger.trace("Updating Rachio sprinkler properties");
            updateProperties(dev.fillProperties());
        }
    } // updateProperties()
} // class
