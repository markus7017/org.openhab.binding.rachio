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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ConfigStatusBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.rachio.RachioBindingConstants;
import org.openhab.binding.rachio.internal.RachioEvent;
import org.openhab.binding.rachio.internal.RachioNetwork;
import org.openhab.binding.rachio.internal.api.RachioApi;
import org.openhab.binding.rachio.internal.api.RachioBindingConfiguration;
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.binding.rachio.internal.api.RachioZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ConfigStatusBridgeHandler} is responsible for implementing the cloud function.
 * The concept of a Bridge is used. In general multiple bridges are supported using different API keys.
 * Devices are linked to the bridge. All devices and zones go offline if the cloud api access fails.
 *
 * @author Markus Michels (markus 7017) - initial contribution
 */
public class RachioBridgeHandler extends ConfigStatusBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);
    private final List<RachioStatusListener> rachioStatusListeners = new CopyOnWriteArrayList<>();
    private final RachioApi rachioApi;
    private final RachioNetwork network;
    private ScheduledFuture<?> pollingJob;
    private boolean jobPending = false;

    /**
     * Thing Handler for the Bridge thing. Handles the cloud connection and links devices+zones to a bridge.
     * Creates an instance of the RachioApi (holding all RachioDevices + RachioZones for the given apikey)
     *
     * Sample thing definition:
     * sets apikey & callback url, sets polling interval to 300s and default zone watering time to 120s:
     *
     * Bridge rachio:cloud:1 [ apikey="xxxxxxxx-xxxx-xxxx-xxxxxxxx",
     * callbackUrl="http://mydomain.com:50001/rachio/webhook",
     * pollingInterval=300, defaultRuntime=120 ]
     * {
     * }
     *
     * @param bridge: Bridge class object
     */
    public RachioBridgeHandler(final Bridge bridge) {
        super(bridge);
        rachioApi = new RachioApi();
        rachioApi.setMaster();
        network = new RachioNetwork();
    }

    /**
     * Initialize the bridge/cloud handler. Creates a connection to the Rachio Cloud, reads devices + zones and
     * initialized the Thing mapping.
     */
    @Override
    public void initialize() {
        try {
            logger.debug("RachioBridgeHandler: Connecting to Rachio cloud");
            createCloudConnection(rachioApi);
            updateProperties();

            // Pass BridgeUID to device, RachioDeviceHandler will fill DeviceUID
            Bridge bridgeThing = this.getThing();
            HashMap<String, RachioDevice> deviceList = getDevices();
            for (HashMap.Entry<String, RachioDevice> de : deviceList.entrySet()) {
                RachioDevice dev = de.getValue();
                ThingUID devThingUID = new ThingUID(RachioBindingConstants.THING_TYPE_DEVICE, bridgeThing.getUID(),
                        dev.getThingID());
                dev.setUID(this.getThing().getUID(), devThingUID);
                dev.setMasterCopy(true);
                // Set DeviceUID for all zones
                HashMap<String, RachioZone> zoneList = dev.getZones();
                for (HashMap.Entry<String, RachioZone> ze : zoneList.entrySet()) {
                    RachioZone zone = ze.getValue();
                    ThingUID zoneZhingUID = new ThingUID(RachioBindingConstants.THING_TYPE_ZONE, bridgeThing.getUID(),
                            zone.getThingID());
                    zone.setUID(dev.getUID(), zoneZhingUID);
                }
            }

            // Informational: Display a info if ipFilter will be applied
            String ipFilter = getIpFilter();
            if (!ipFilter.equals("")) {
                logger.info("RachioBridge: The following IP filter will be applied: '{}'", ipFilter);
            }

            // Prepare port mapping
            /*
             * String url = getCallbackUrl();
             * if (!url.equals("")) {
             * String port[] = url.split(":");
             * int internalPort = Integer.parseInt(port[1]);
             * int externalPort = 8443;
             * 
             * if (network.initializePortMapping(externalPort, internalPort,
             * RachioBindingConstants.PORT_REFRESH_INTERVAL)) {
             * logger.info("RachioBridge: Port mapping initialzed, external {} -> internal {}", internalPort,
             * externalPort);
             * }
             * }
             */

            // Bridge initialized, change to ONLINE state
            // updateListenerManagement();
            logger.info("RachioCloud: Cloud connector initialized.");
            updateStatus(ThingStatus.ONLINE);
        } catch (Exception e) {
            logger.error("Unexpected error while communicating with Rachio cloud: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    } // initialize()

    /**
     * Handle Thing commands - the bridge doesn't implement any commands
     */
    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        // cloud handler has no channels
        logger.debug("RachioBridge.handleCommand {} for {}", command.toString(), channelUID.getAsString());
    }

    /**
     * Update device status (poll Rachio Cloud)
     * in addition webhooks are used to get events (if callbackUrl is configured)
     */
    public void refreshDeviceStatus() {
        try {
            synchronized (this) {
                if (jobPending) {
                    logger.debug("RachioBridge: Already checking");
                    return;
                }
                jobPending = true;
            }

            logger.trace("RachioBridgeHandler: refreshDeviceStatus");
            HashMap<String, RachioDevice> deviceList = getDevices();
            if (deviceList == null) {
                logger.debug("RachioBridgeHandler: Cloud access not initialized yet!");
                return;
            }

            RachioApi checkApi = new RachioApi();
            createCloudConnection(checkApi);
            if (this.getThing().getStatus() != ThingStatus.ONLINE) {
                logger.debug("RachioBridgeHandler: Bridge is ONLINE");
                updateStatus(ThingStatus.ONLINE);
            }

            HashMap<String, RachioDevice> checkDevList = checkApi.getDevices();
            for (HashMap.Entry<String, RachioDevice> de : checkDevList.entrySet()) {
                RachioDevice checkDev = de.getValue();
                RachioDevice dev = deviceList.get(checkDev.getId());
                if (dev == null) {
                    logger.info("RachioBridge: New device detected: '{}' - '{}'", checkDev.getId(), checkDev.getName());
                } else {
                    if (!dev.compare(checkDev)) {
                        logger.trace("RachioBridge: Update data for device '{}'", dev.getName());
                        if (dev.getThingHandler() != null) {
                            dev.getThingHandler().onThingStateChangedl(checkDev, null);
                        } else {
                            rachioStatusListeners.stream().forEach(l -> l.onThingStateChangedl(checkDev, null));
                        }
                    } else {
                        logger.trace("RachioBridge: Device '{}' was not updaterd", checkDev.getId());
                    }

                    HashMap<String, RachioZone> zoneList = dev.getZones();
                    HashMap<String, RachioZone> checkZoneList = dev.getZones();
                    for (HashMap.Entry<String, RachioZone> ze : checkZoneList.entrySet()) {
                        RachioZone checkZone = ze.getValue();
                        RachioZone zone = zoneList.get(checkZone.getId());
                        if (zone == null) {
                            logger.debug("RachioBridge: New zone detected: '{}' - '{}'", checkDev.getId(),
                                    checkZone.getName());
                        } else {
                            if (!zone.compare(checkZone)) {
                                logger.trace("RachioBridge: Update data for zone '{}'", zone.getName());
                                if (zone.getThingHandler() != null) {
                                    zone.getThingHandler().onThingStateChangedl(checkDev, null);
                                } else {
                                    rachioStatusListeners.stream().forEach(l -> l.onThingStateChangedl(checkDev, null));
                                }
                            } else {
                                logger.trace("RachioBridge: Zone '{}' was not updated.", checkZone.getId());
                            }
                        } // elif (zone == null)
                    } // for each zone
                } // for each device

                // Refresh UPnP port mapping
                if (network.refreshPortMapping()) {
                    logger.debug("RachioBridge: Port mapping refreshed.");
                }
            } // try
        } catch (Exception e) {
            logger.error("RachioBridge: Unexpected error while checking device status: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "RachioBridge: Unable to refresh device status: " + e.getMessage());
        } finally {
            jobPending = false;
        }
    } // refreshDeviceStatus()

    /**
     * Create a new SleepIQ cloud service connection. If a connection already exists, it will be lost.
     *
     * @throws LoginException if there is an error while authenticating to the service
     */
    private void createCloudConnection(RachioApi api) throws Exception {
        RachioBindingConfiguration bindingConfig = getConfigAs(RachioBindingConfiguration.class);
        Configuration config = getThing().getConfiguration();
        bindingConfig.apiKey = (String) config.get(RachioBindingConfiguration.PARAM_APIKEY);
        if (bindingConfig.apiKey.isEmpty()) {
            throw new Exception(
                    "RachioBridgeHandler: Unable to connect to Rachio Cloud: apikey not set, check services/rachio.cfg!");
        }

        if (!api.initialize(bindingConfig.apiKey, this.getThing().getUID())) {
            throw new Exception("Unable to connect to Rachio Cloud!");
        }
    } // createCloudConnection()

    /**
     * puts the device into standby mode = disable watering, schedules etc.
     *
     * @param deviceId: Device (ID retrieved from initialization)
     * @return true: successful, failed (check http error code)
     */
    public boolean disableDevice(String deviceId) {
        try {
            return rachioApi.disableDevice(deviceId);
        } catch (Exception e) {
            logger.error("RachioBridgeHandler.disableDevice failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * puts the device into run mode = watering, schedules etc.
     *
     * @param deviceId: Device (ID retrieved from initialization)
     * @return true: successful, failed (check http error code)
     */
    public boolean enableDevice(String deviceId) {
        try {
            return rachioApi.enableDevice(deviceId);
        } catch (Exception e) {
            logger.error("RachioBridgeHandler.enableDevice failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Stop watering for all zones, disable schedule etc. - puts the device into standby mode
     *
     * @param deviceId: Device (ID retrieved from initialization)
     * @return true: successful, failed (check http error code)
     * @return
     */
    public boolean stopWatering(String deviceId) {
        try {
            return rachioApi.stopWatering(deviceId);
        } catch (Exception e) {
            logger.error("RachioBridgeHandler.stopWatering failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Start rain delay cycle.
     *
     * @param deviceId: Device (ID retrieved from initialization)
     * @param delayTime: Number of seconds for rain delay sycle
     * @return true: successful, failed (check http error code)
     */
    public boolean startRainDelay(String deviceId, int delayTime) {
        try {
            return rachioApi.rainDelay(deviceId, delayTime);
        } catch (Exception e) {
            logger.error("RachioBridgeHandler.startRainDelay failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Start watering for multiple zones.
     *
     * @param zoneListJson: Contains a list of { "id": n} with the zone ids to start
     * @return true: successful, failed (check http error code)
     */
    public boolean runMultipleZones(String zoneListJson) {
        try {
            return rachioApi.runMultilpeZones(zoneListJson);
        } catch (Exception e) {
            logger.error("RachioBridgeHandler.runMultipleZones failed: {}", e.getMessage());
        }
        return false;

    }

    /**
     * Start a single zone for given number of seconds.
     *
     * @param zoneId: Rachio Cloud Zone ID
     * @param runTime: Number of seconds to run
     * @return true: successful, failed (check http error code)
     */
    public boolean startZone(String zoneId, int runTime) {
        try {
            return rachioApi.runZone(zoneId, runTime);
        } catch (Exception e) {
            logger.error("RachioBridgeHandler.startZone failed: {}", e.getMessage());
        }
        return false;
    }

    //
    // ------ Read Thing config
    //

    /**
     * Retrieve the apikey for connecting to rachio cloud
     *
     * @return the polling interval in seconds
     */
    public String getApiKey() {
        String apiKey = getConfigAs(RachioBindingConfiguration.class).apiKey;
        if (!apiKey.equals("")) {
            return apiKey;
        }
        Configuration config = getThing().getConfiguration();
        return (String) config.get(RachioBindingConfiguration.PARAM_APIKEY);
    }

    /**
     * Retrieve the polling interval from Thing config
     *
     * @return the polling interval in seconds
     */
    public int getPollingInterval() {
        return getConfigAs(RachioBindingConfiguration.class).pollingInterval;
    }

    /**
     * Retrieve the callback URL for Rachio Cloud Eevents
     *
     * @return callbackUrl
     */
    public String getCallbackUrl() {
        return getConfigAs(RachioBindingConfiguration.class).callbackUrl;
    }

    /**
     * Retrieve the clearAllCallbacks flag from thing config
     *
     * @return true=clear all callbacks, false=clear only the current one (avoid multiple instances)
     */
    public Boolean getClearAllCallbacks() {
        return getConfigAs(RachioBindingConfiguration.class).clearAllCallbacks;
    }

    /**
     *
     */
    public String getIpFilter() {
        return getConfigAs(RachioBindingConfiguration.class).ipFilter;
    }

    /**
     * Retrieve the default runtime from Thing config
     *
     * @return the polling interval in seconds
     */
    public int getDefaultRuntime() {
        return getConfigAs(RachioBindingConfiguration.class).defaultRuntime;
    }

    //
    // ------ Stuff used by other classes
    //

    /**
     * Get the list of discovered devices (those retrieved from the Rachio Cloud)
     *
     * @return HashMap of RachioDevice
     */
    public HashMap<String, RachioDevice> getDevices() {
        try {
            return rachioApi.getDevices();
        } catch (Exception e) {
            logger.error("RachioBridgeHandler: Unable to retrieve device list: {}", e.getMessage());
        }
        return null;
    }

    /**
     * return RachioDevice by device Thing UID
     *
     * @param thingUID
     * @return RachioDevice for that device Thing UID
     */
    public RachioDevice getDevByUID(ThingUID thingUID) {
        return rachioApi.getDevByUID(getThing().getUID(), thingUID);
    }

    /**
     * return RachioZone for given Zone Thing UID
     *
     * @param thingUID
     * @return
     */
    public RachioZone getZoneByUID(ThingUID thingUID) {
        return rachioApi.getZoneByUID(getThing().getUID(), thingUID);
    }

    /**
     * Register a webhook at Rachio Cloud for the given deviceID. The webhook triggers our servlet to popolate device &
     * zones events.
     *
     * @param deviceId: Matching device ID (as retrieved from device initialization)
     * @return trtue: successful, false: failed (check http error code)
     */
    public boolean registerWebHook(String deviceId) {
        try {
            if (getCallbackUrl().equals("")) {
                logger.trace("RachioApi: No callbackUrl configured.");
                return true;
            } else {
                return rachioApi.registerWebHook(deviceId, getCallbackUrl(), getExternalId(), getClearAllCallbacks());
            }
        } catch (Exception e) {
            logger.error("RachioBridgeHandler.registerWebHook({}) failed: {}", deviceId, e.getMessage());
        }
        return false;
    }

    public boolean webHookEvent(RachioEvent event) {
        try {
            HashMap<String, RachioDevice> deviceList = getDevices();
            for (HashMap.Entry<String, RachioDevice> de : deviceList.entrySet()) {
                RachioDevice dev = de.getValue();
                if (dev.getId().equals(event.deviceId) && (dev.getThingHandler() != null)) {
                    return dev.getThingHandler().webhookEvent(event);
                }
            }
            logger.debug("RachioEvent {}.{} for unknown device '{}': {}", event.category, event.type, event.deviceId,
                    event.summary);
        } catch (Exception e) {
            logger.error("RachioEvent: Unable to process event {}.{} for device '{}': {}", event.category, event.type,
                    event.deviceId, e.getMessage());
        }
        return false;
    }

    public String getExternalId() {
        // for now, just the api key
        return getApiKey();
    }

    /**
     * Start or stop a background polling job to look for bed status updates based on whether or not there are any
     * listeners to notify.
     */
    private synchronized void updateListenerManagement() {
        if (!rachioStatusListeners.isEmpty() && (pollingJob == null || pollingJob.isCancelled())) {
            pollingJob = scheduler.scheduleWithFixedDelay(pollingRunnable, getPollingInterval(), getPollingInterval(),
                    TimeUnit.SECONDS);
        } else if (rachioStatusListeners.isEmpty() && pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
            pollingJob = null;
        }
    }

    /**
     * Register the given listener to receive device status updates.
     *
     * @param listener the listener to register
     */
    public void registerStatusListener(final RachioStatusListener listener) {
        if (listener == null) {
            return;
        }

        rachioStatusListeners.add(listener);
        updateListenerManagement();
    }

    /**
     * Unregister the given listener from further device status updates.
     *
     * @param listener the listener to unregister
     * @return <code>true</code> if listener was previously registered and is now unregistered; <code>false</code>
     *         otherwise
     */
    public boolean unregisterStatusListener(final RachioStatusListener listener) {
        boolean result = rachioStatusListeners.remove(listener);
        if (result) {
            updateListenerManagement();
        }

        return result;
    }

    @Override
    public Collection<ConfigStatusMessage> getConfigStatus() {
        Collection<ConfigStatusMessage> configStatusMessages = new ArrayList<>();

        RachioBindingConfiguration config = getConfigAs(RachioBindingConfiguration.class);

        if (config.apiKey.isEmpty()) {
            configStatusMessages.add(ConfigStatusMessage.Builder.error(RachioBindingConfiguration.PARAM_APIKEY)
                    .withMessageKeySuffix(RachioBindingConfiguration.ERR_APIKEY)
                    .withArguments(RachioBindingConfiguration.PARAM_APIKEY).build());
        }

        return configStatusMessages;
    }

    /**
     * Update the given properties with attributes of the given bed. If no properties are given, a new map will be
     * created.
     *
     * @param bed the source of data
     * @param properties the properties to update (this may be <code>null</code>)
     * @return the given map (or a new map if no map was given) with updated/set properties from the supplied bed
     */
    /*
     * public Map<String, String> updateProperties(final ThingUID uid, Map<String, String> properties) {
     * if (rachioApi != null) {
     * RachioDevice dev = rachioApi.getDevByUID(getThing().getUID(), uid);
     * if (dev != null) {
     * return dev.fillProperties();
     * }
     * }
     * return null;
     * }
     */
    private void updateProperties() {
        if (rachioApi != null) {
            updateProperties(rachioApi.fillProperties());
        }
    }

    //
    // ------ Internal stuff
    //

    private Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            refreshDeviceStatus();
        }
    };

    @Override
    public synchronized void dispose() {
        logger.debug("RachioBridgeHandler: Disposing Rachio cloud handler");

        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
            pollingJob = null;
        }
    }

} // class RachioBridgeHandler
