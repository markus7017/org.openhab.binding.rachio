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
package org.openhab.binding.rachio.internal.discovery;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.rachio.RachioBindingConstants;
import org.openhab.binding.rachio.handler.RachioBridgeHandler;
import org.openhab.binding.rachio.internal.api.RachioApi;
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.binding.rachio.internal.api.RachioZone;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioDiscoveryService} is responsible for processing the
 * results of devices found through the Rachio cloud service.
 *
 * @author Markus Michels (markus7017)- Initial contribution
 */
@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.rachio")
public class RachioDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(RachioDiscoveryService.class);
    private boolean scanning = false;

    private RachioBridgeHandler cloudHandler;

    public RachioDiscoveryService() {
        super(RachioBindingConstants.SUPPORTED_THING_TYPES_UIDS, RachioBindingConstants.BINDING_DISCOVERY_TIMEOUT,
                true);
        String uids = RachioBindingConstants.SUPPORTED_THING_TYPES_UIDS.toString();
        logger.debug("Rachio: thing types: {} registered.", uids);
    }

    public void setCloudHandler(final RachioBridgeHandler cloudHandler) {
        if (cloudHandler == null) {
            logger.debug("Invalid RachioCloudHandler");
        }
        this.cloudHandler = cloudHandler;
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Starting background discovery for new Rachio controllers");
        startScan();
    }

    @Override
    protected synchronized void startScan() {
        try {
            synchronized (this) {
                if (scanning) {
                    logger.debug("RachioDiscovery: Already discoverying");
                    return;
                }
                scanning = true;
            }

            logger.debug("Starting scan for new Rachio controllers");
            HashMap<String, RachioDevice> deviceList;
            RachioApi rapi = new RachioApi();
            ThingUID bridgeUID;
            if (cloudHandler == null) {
                // String apiKey = "cc765dfb-d095-4ceb-8062-b9d88dcce911";
                // String apiKey = "3ad01c06-a381-44bf-85fb-014a115e219f";
                String apiKey = "";
                if (apiKey.equals("")) {
                    logger.debug("RachioDiscovery: API not yet initialized");
                    return;
                }
                bridgeUID = new ThingUID(RachioBindingConstants.BINDING_ID, "cloud", "1");
                rapi.initialize(apiKey, bridgeUID);
                deviceList = rapi.getDevices();

                @SuppressWarnings({ "unchecked", "rawtypes" })
                Map<String, Object> bridgeProp = (Map) fillProperties(apiKey);
                DiscoveryResult bridgeResult = DiscoveryResultBuilder.create(bridgeUID).withProperties(bridgeProp)
                        .withBridge(bridgeUID).withLabel("Rachio Cloud").build();
                thingDiscovered(bridgeResult);
            } else {
                deviceList = cloudHandler.getDevices();
                bridgeUID = cloudHandler.getThing().getUID();
            }
            if (deviceList == null) {
                logger.debug("RachioDiscovery: Rachio Cloud access not initialized yet!");
                return;
            }
            logger.debug("RachioDiscovery: Found {} devices.", deviceList.size());
            for (HashMap.Entry<String, RachioDevice> de : deviceList.entrySet()) {
                RachioDevice dev = de.getValue();
                logger.debug("RachioDiscovery: Check Rachio device with ID '{}'", dev.getId());

                // register thing if it not already exists
                ThingUID devThingUID = new ThingUID(RachioBindingConstants.THING_TYPE_DEVICE, bridgeUID,
                        dev.getThingID());
                dev.setUID(bridgeUID, devThingUID);
                if ((cloudHandler == null) || (cloudHandler.getThingByUID(devThingUID) == null)) {
                    logger.info("RachioDiscovery: New Rachio device discovered: '{}' (id {}), S/N={}, MAC={}",
                            dev.getName(), dev.getId(), dev.getSerial(), dev.getMacAddress());
                    logger.debug("  latitude={}, longitude={}, elevation={}", dev.getLatitude(), dev.getLongitude(),
                            dev.getElevation());
                    logger.info("   device status={}, paused={}, on={}", dev.getStatusStr(), dev.getPaused(),
                            dev.getEnabled());
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    Map<String, Object> properties = (Map) dev.fillProperties();
                    DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(devThingUID)
                            .withProperties(properties).withBridge(bridgeUID).withLabel(dev.getThingName()).build();
                    thingDiscovered(discoveryResult);
                } // if (cloudHandler.getThingByUID(dev_thingUID) == null)

                HashMap<String, RachioZone> zoneList = dev.getZones();
                logger.info("RachioDiscovery: Found {} zones for this device.", zoneList.size());
                for (HashMap.Entry<String, RachioZone> ze : zoneList.entrySet()) {
                    RachioZone zone = ze.getValue();
                    logger.debug("RachioDiscovery: Checking zone with ID '{}'", zone.getId());

                    // register thing if it not already exists
                    ThingUID zoneThingUID = new ThingUID(RachioBindingConstants.THING_TYPE_ZONE, bridgeUID,
                            zone.getThingID());
                    zone.setUID(devThingUID, zoneThingUID);
                    if ((cloudHandler == null) || (cloudHandler.getThingByUID(zoneThingUID) == null)) {
                        logger.info("RachioDiscovery: Zone[{}] '{}' (id={}) added, enabled={}", zone.getName(),
                                zone.getZoneNumber(), zone.getId(), zone.getEnabled());
                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        Map<String, Object> zproperties = (Map) zone.fillProperties();
                        DiscoveryResult zoneDiscoveryResult = DiscoveryResultBuilder.create(zoneThingUID)
                                .withProperties(zproperties).withBridge(bridgeUID)
                                .withLabel(dev.getName() + "[" + zone.getZoneNumber() + "]: " + zone.getName()).build();
                        thingDiscovered(zoneDiscoveryResult);
                    } // if (cloudHandler.getThingByUID(zoneThingUID) == null)

                } // for (each zone)
            } // for (seach device)
            logger.info("{}  Rachio controller initialized.", deviceList.size());

            stopScan();
        } catch (Exception e) {
            logger.error("RachioDiscovery: Unexpected error while discovering Rachio devices/zones: {}",
                    e.getMessage());
        }
    } // startScan()

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        scanning = false;
        logger.debug("RachioDiscervery: discovery done.");
    }

    private Map<String, String> fillProperties(String id) {
        Map<String, String> properties = new HashMap<>();
        properties.put(RachioBindingConstants.PROPERTY_NAME, "Rachio Cloud");
        properties.put(RachioBindingConstants.PROPERTY_ID, id);
        properties.put(Thing.PROPERTY_VENDOR, "Rachio");
        return properties;
    }

} // class RachioDiscoveryService
