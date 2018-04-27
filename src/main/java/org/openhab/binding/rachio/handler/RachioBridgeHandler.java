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

import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.text.MessageFormat;
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
import org.openhab.binding.rachio.internal.RachioConfiguration;
import org.openhab.binding.rachio.internal.api.RachioApi;
import org.openhab.binding.rachio.internal.api.RachioApi.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.binding.rachio.internal.api.RachioEvent;
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
    private RachioConfiguration bindingConfig;
    private RachioConfiguration thingConfig = new RachioConfiguration();

    private final List<RachioStatusListener> rachioStatusListeners = new CopyOnWriteArrayList<>();
    private final RachioApi rachioApi;
    private String personId = "";

    private ScheduledFuture<?> pollingJob;
    private boolean jobPending = false;
    private int skipCalls = 0;

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
        rachioApi = new RachioApi(personId);
    }

    public void setConfiguration(RachioConfiguration defaultConfig) {
        bindingConfig = defaultConfig;
    }

    /**
     * Initialize the bridge/cloud handler. Creates a connection to the Rachio Cloud, reads devices + zones and
     * initialized the Thing mapping.
     */
    @Override
    public void initialize() {
        String errorMessage = "";

        try {
            thingConfig = bindingConfig;
            thingConfig.updateConfig(getConfig().getProperties());

            logger.debug("RachioBridge: Connecting to Rachio cloud");
            createCloudConnection(rachioApi);
            updateProperties();

            // Pass BridgeUID to device, RachioDeviceHandler will fill DeviceUID
            Bridge bridgeThing = this.getThing();
            HashMap<String, RachioDevice> deviceList = getDevices();
            for (HashMap.Entry<String, RachioDevice> de : deviceList.entrySet()) {
                RachioDevice dev = de.getValue();
                ThingUID devThingUID = new ThingUID(THING_TYPE_DEVICE, bridgeThing.getUID(), dev.getThingID());
                dev.setUID(this.getThing().getUID(), devThingUID);
                // Set DeviceUID for all zones
                HashMap<String, RachioZone> zoneList = dev.getZones();
                for (HashMap.Entry<String, RachioZone> ze : zoneList.entrySet()) {
                    RachioZone zone = ze.getValue();
                    ThingUID zoneThingUID = new ThingUID(THING_TYPE_ZONE, bridgeThing.getUID(), zone.getThingID());
                    zone.setUID(dev.getUID(), zoneThingUID);
                }
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
        } catch (RachioApiException e) {
            errorMessage = e.toString();
        } catch (UnknownHostException e) {
            errorMessage = MessageFormat.format("Unknown host '{0}' or Internet connection down", e.getMessage());
        } catch (Throwable e) {
            errorMessage = e.getMessage();
        } finally {
            if (!errorMessage.isEmpty()) {
                logger.error("RachioBridge: {}", errorMessage);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
            }
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
        String errorMessage = "";
        logger.debug("RachioBridgeHandler: refreshDeviceStatus");

        try {
            synchronized (this) {
                if (jobPending) {
                    logger.debug("RachioBridge: Already checking");
                    return;
                }
                jobPending = true;
            }

            HashMap<String, RachioDevice> deviceList = getDevices();
            if (deviceList == null) {
                logger.debug("RachioBridgeHandler: Cloud access not initialized yet!");
                return;
            }

            RachioApi checkApi = new RachioApi(personId);
            createCloudConnection(checkApi);
            if (checkApi.getLastApiResult().isRateLimitBlocked()) {
                String errorCritical = "";
                errorCritical = MessageFormat.format(
                        "RachioBridge: API access blocked on update ({0} / {1}), reset at {2}",
                        checkApi.getLastApiResult().rateRemaining, checkApi.getLastApiResult().rateLimit,
                        checkApi.getLastApiResult().rateReset);
                logger.error("{}", errorCritical);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorCritical); // shutdown
                                                                                                         // bridge+devices+zones
                return;
            }
            if (checkApi.getLastApiResult().isRateLimitWarning()) {
                skipCalls++;
                if (skipCalls % RACHIO_RATE_SKIP_CALLS > 0) {
                    logger.info("RachioBridge: API result is getting critical -> skip update ({} / {})", skipCalls,
                            RACHIO_RATE_SKIP_CALLS);
                    return;
                }
            }
            if (this.getThing().getStatus() != ThingStatus.ONLINE) {
                logger.debug("RachioBridgeHandler: Bridge is ONLINE");
                updateStatus(ThingStatus.ONLINE);
            }

            HashMap<String, RachioDevice> checkDevList = checkApi.getDevices();
            for (HashMap.Entry<String, RachioDevice> de : checkDevList.entrySet()) {
                RachioDevice checkDev = de.getValue();
                RachioDevice dev = deviceList.get(checkDev.id);
                if (dev == null) {
                    logger.info("RachioBridge: New device detected: '{}' - '{}'", checkDev.id, checkDev.name);
                } else {
                    if (!dev.compare(checkDev)) {
                        logger.trace("RachioBridge: Update data for device '{}'", dev.name);
                        if (dev.getThingHandler() != null) {
                            dev.getThingHandler().onThingStateChangedl(checkDev, null);
                        } else {
                            rachioStatusListeners.stream().forEach(l -> l.onThingStateChangedl(checkDev, null));
                        }
                    } else {
                        logger.trace("RachioBridge: Device '{}' was not updaterd", checkDev.id);
                    }

                    HashMap<String, RachioZone> zoneList = dev.getZones();
                    HashMap<String, RachioZone> checkZoneList = dev.getZones();
                    for (HashMap.Entry<String, RachioZone> ze : checkZoneList.entrySet()) {
                        RachioZone checkZone = ze.getValue();
                        RachioZone zone = zoneList.get(checkZone.id);
                        if (zone == null) {
                            logger.debug("RachioBridge: New zone detected: '{}' - '{}'", checkDev.id, checkZone.name);
                        } else {
                            if (!zone.compare(checkZone)) {
                                logger.trace("RachioBridge: Update data for zone '{}'", zone.name);
                                if (zone.getThingHandler() != null) {
                                    zone.getThingHandler().onThingStateChangedl(checkDev, null);
                                } else {
                                    rachioStatusListeners.stream().forEach(l -> l.onThingStateChangedl(checkDev, null));
                                }
                            } else {
                                logger.trace("RachioBridge: Zone '{}' was not updated.", checkZone.id);
                            }
                        } // elif (zone == null)
                    } // for each zone
                } // for each device
            } // try
        } catch (RachioApiException e) {
            errorMessage = e.toString();
        } catch (Throwable e) {
            errorMessage = e.getMessage();
        } finally {
            if (!errorMessage.isEmpty()) {
                logger.error("RachioBridge: {}", errorMessage);
                // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
            }
            jobPending = false;
        }
    } // refreshDeviceStatus()

    public void shutdown() {
        logger.info("RachioBridge: Shutting down");
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
    }

    /**
     * Create a new SleepIQ cloud service connection. If a connection already exists, it will be lost.
     *
     * @throws LoginException if there is an error while authenticating to the service
     */
    private void createCloudConnection(RachioApi api) throws RachioApiException, UnknownHostException {
        if (thingConfig.apikey.isEmpty()) {
            throw new RachioApiException(
                    "RachioBridgeHandler: Unable to connect to Rachio Cloud: apikey not set, check services/rachio.cfg!");
        }

        if (api.initialize(thingConfig.apikey, this.getThing().getUID())) {
            personId = api.getPersonId(); // cache personId, might throw exception
        }
    } // createCloudConnection()

    /**
     * puts the device into standby mode = disable watering, schedules etc.
     *
     * @param deviceId: Device (ID retrieved from initialization)
     * @return true: successful, failed (check http error code)
     */
    public void disableDevice(String deviceId) throws RachioApiException {
        rachioApi.disableDevice(deviceId);
    }

    /**
     * puts the device into run mode = watering, schedules etc.
     *
     * @param deviceId: Device (ID retrieved from initialization)
     * @return true: successful, failed (check http error code)
     */
    public void enableDevice(String deviceId) throws RachioApiException {
        rachioApi.enableDevice(deviceId);
    }

    /**
     * Stop watering for all zones, disable schedule etc. - puts the device into standby mode
     *
     * @param deviceId: Device (ID retrieved from initialization)
     * @return true: successful, failed (check http error code)
     * @return
     */
    public void stopWatering(String deviceId) throws RachioApiException {
        rachioApi.stopWatering(deviceId);
    }

    /**
     * Start rain delay cycle.
     *
     * @param deviceId: Device (ID retrieved from initialization)
     * @param delayTime: Number of seconds for rain delay sycle
     * @return true: successful, failed (check http error code)
     */
    public void startRainDelay(String deviceId, int delayTime) throws RachioApiException {
        rachioApi.rainDelay(deviceId, delayTime);
    }

    /**
     * Start watering for multiple zones.
     *
     * @param zoneListJson: Contains a list of { "id": n} with the zone ids to start
     * @return true: successful, failed (check http error code)
     */
    public void runMultipleZones(String zoneListJson) throws RachioApiException {
        rachioApi.runMultilpeZones(zoneListJson);
    }

    /**
     * Start a single zone for given number of seconds.
     *
     * @param zoneId: Rachio Cloud Zone ID
     * @param runTime: Number of seconds to run
     * @return true: successful, failed (check http error code)
     */
    public void startZone(String zoneId, int runTime) throws RachioApiException {
        rachioApi.runZone(zoneId, runTime);
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
        String apikey = getConfigAs(RachioConfiguration.class).apikey;
        if (!apikey.equals("")) {
            return apikey;
        }
        Configuration config = getThing().getConfiguration();
        return (String) config.get(PARAM_APIKEY);
    }

    /**
     * Retrieve the polling interval from Thing config
     *
     * @return the polling interval in seconds
     */
    public int getPollingInterval() {
        return getConfigAs(RachioConfiguration.class).pollingInterval;
    }

    /**
     * Retrieve the callback URL for Rachio Cloud Eevents
     *
     * @return callbackUrl
     */
    public String getCallbackUrl() {
        return getConfigAs(RachioConfiguration.class).callbackUrl;
    }

    /**
     * Retrieve the clearAllCallbacks flag from thing config
     *
     * @return true=clear all callbacks, false=clear only the current one (avoid multiple instances)
     */
    public Boolean getClearAllCallbacks() {
        return getConfigAs(RachioConfiguration.class).clearAllCallbacks;
    }

    /**
     *
     */
    public String getIpFilter() {
        return getConfigAs(RachioConfiguration.class).ipFilter;
    }

    /**
     * Retrieve the default runtime from Thing config
     *
     * @return the polling interval in seconds
     */
    public int getDefaultRuntime() {
        return getConfigAs(RachioConfiguration.class).defaultRuntime;
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
    public void registerWebHook(String deviceId) throws RachioApiException {
        if (getCallbackUrl().equals("")) {
            logger.trace("RachioApi: No callbackUrl configured.");
        } else {
            rachioApi.registerWebHook(deviceId, getCallbackUrl(), getExternalId(), getClearAllCallbacks());
        }
    }

    /**
     * Handle inbound WebHook event (dispatch to device handler)
     *
     * @param event
     * @return
     */
    public boolean webHookEvent(RachioEvent event) {
        try {
            HashMap<String, RachioDevice> deviceList = getDevices();
            for (HashMap.Entry<String, RachioDevice> de : deviceList.entrySet()) {
                RachioDevice dev = de.getValue();
                if (dev.id.equalsIgnoreCase(event.deviceId) && (dev.getThingHandler() != null)) {
                    return dev.getThingHandler().webhookEvent(event);
                }
            }
            logger.debug("RachioEvent {}.{} for unknown device '{}': {}", event.category, event.type, event.deviceId,
                    event.summary);
        } catch (Throwable e) {
            logger.error("RachioEvent: Unable to process event {}.{} for device '{}': {}", event.category, event.type,
                    event.deviceId, e.getMessage());
        }
        return false;
    }

    public String getExternalId() {
        // return a MD5 of the apikey
        return getMD5Hash(getApiKey());
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

        RachioConfiguration config = getConfigAs(RachioConfiguration.class);

        if (config.apikey.isEmpty()) {
            configStatusMessages.add(ConfigStatusMessage.Builder.error(PARAM_APIKEY)
                    .withMessageKeySuffix(RachioConfiguration.ERR_APIKEY).withArguments(PARAM_APIKEY).build());
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

    private static final String MD5_HASH_ALGORITHM = "MD5";
    private static final String UTF8_CHAR_SET = "UTF-8";

    /**
     * Given a string, return the MD5 hash of the String.
     *
     * @param unhashed The string contents to be hashed.
     * @return MD5 Hashed value of the String. Null if there is a problem hashing the String.
     */
    public static String getMD5Hash(String unhashed) {
        try {
            byte[] bytesOfMessage = unhashed.getBytes(UTF8_CHAR_SET);

            MessageDigest md5 = MessageDigest.getInstance(MD5_HASH_ALGORITHM);

            byte[] hash = md5.digest(bytesOfMessage);

            StringBuilder sb = new StringBuilder(2 * hash.length);

            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }

            String digest = sb.toString();

            return digest;
        } catch (Exception exp) {
            return null;
        }
    }

} // class RachioBridgeHandler
