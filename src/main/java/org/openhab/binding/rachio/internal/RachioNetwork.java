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
package org.openhab.binding.rachio.internal;

import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.util.ArrayList;

import org.apache.commons.net.util.SubnetUtils;
import org.openhab.binding.rachio.internal.api.RachioHttp;
//import com.offbynull.portmapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * RachioNetwork: Implement network related functions
 *
 * @author Markus Michels (markus7017) - Initial contribution
 */
public class RachioNetwork {
    private final Logger logger = LoggerFactory.getLogger(RachioNetwork.class);

    public class AwsIpAddressRange {
        public String ip_prefix = "";
        public String region = "";
        public String service = "";
    }

    public class AwsIpList {
        public String syncToken = "";
        public String createDate = "";
        public ArrayList<AwsIpAddressRange> prefixes = new ArrayList<>();
    }

    // private MappedPort mappedPort;
    private String mappedPort;
    private long lastRefresh = 0;
    private ArrayList<AwsIpAddressRange> awsIpRanges = new ArrayList<>();

    public boolean initializeAwsList() {
        try {
            // Get currently assigned IP address ranges from AWS cloud.
            // The Rachio cloud service is hosted on AWS. The list will function as a filter to make sure that the
            // webhook call was originated by the AWS infrastructure - not perfect security, but helps to avoid abuse
            // and protect OH in
            // some kind
            RachioHttp http = new RachioHttp("");
            String jsonList = http.httpGet(AWS_IPADDR_DOWNLOAD_URL, "").resultString;
            Gson gson = new Gson();
            AwsIpList list = gson.fromJson(jsonList, AwsIpList.class);
            for (int i = 0; i < list.prefixes.size(); i++) {
                AwsIpAddressRange entry = list.prefixes.get(i);
                if (entry.region.startsWith(AWS_IPADDR_REGION_FILTER)) {
                    logger.trace("RachioNetwork: Adding range '{}' (region '{}' to AWS IP address list",
                            entry.ip_prefix, entry.region);
                    awsIpRanges.add(entry);
                }
            }
            logger.debug(
                    "RachioNetwork: AWS address list initialized, {} entries (will be used to verify inboud Rachio events)",
                    awsIpRanges.size());
            return true;
        } catch (Exception e) {
            logger.error("RachioNetwork: Unable to initialize: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Checks if client ip equals or is in range of ip networks provided by
     * semicolon separated list
     *
     * @param clientIp in numeric form like "192.168.0.10"
     * @param ipList like "127.0.0.1;192.168.0.0/24;10.0.0.0/8"
     * @return true if client ip from the list os ips and networks
     */
    public static boolean isIpInSubnet(String clientIp, String ipList) {
        if ((ipList == null) || ipList.equals("")) {
            // No ip address provided
            return true;
        }
        String[] subnetMasks = ipList.split(";");
        for (String subnetMask : subnetMasks) {
            subnetMask = subnetMask.trim();
            if (clientIp.equals(subnetMask)) {
                return true;
            }
            if (subnetMask.contains("/")) {
                if (new SubnetUtils(subnetMask).getInfo().isInRange(clientIp)) {
                    return true;
                }
            }
        }
        return false;
    } // evaluateIp

    public boolean isIpInAwsList(String clientIp) {
        if (awsIpRanges.size() == 0) {
            // filtering not enabled
            return true;
        }

        for (int i = 0; i < awsIpRanges.size(); i++) {
            AwsIpAddressRange e = awsIpRanges.get(i);
            if (isIpInSubnet(clientIp, e.ip_prefix)) {
                return true;
            }
        }
        return false;
    }

    public boolean initializePortMapping(int externalPort, int internalPort, int timeoutSec) {
        try {
            /*
             * // Start gateways
             * Gateway network = NetworkGateway.create();
             * Gateway process = ProcessGateway.create();
             * Bus networkBus = network.getBus();
             * Bus processBus = process.getBus();
             *
             * // Discover port forwarding devices and take the first one found
             * List<PortMapper> mappers = PortMapperFactory.discover(networkBus, processBus);
             * PortMapper mapper = mappers.get(0);
             *
             * // Map internal port 12345 to some external port (55555 preferred)
             * //
             * // IMPORTANT NOTE: Many devices prevent you from mapping ports that are <= 1024
             * // (both internal and external ports). Be mindful of this when choosing which
             * // ports you want to map.
             * MappedPort mappedPort = mapper.mapPort(PortType.TCP, internalPort, externalPort, timeoutSec);
             */
            lastRefresh = System.currentTimeMillis();
            logger.debug("RachioNetwork: Port mapping added ({}->{}, timeout {}); {}", externalPort, internalPort,
                    timeoutSec, mappedPort.toString());
            return true;

        } catch (Exception e) {
            logger.error("RachioNetwork: Unable to create port mapping ({}->{}, timeout {}): {}", externalPort,
                    internalPort, timeoutSec, e.getMessage());
        }

        return false;
    } // initializePortMapping()

    public boolean refreshPortMapping() {
        try {
            if (lastRefresh == 0) {
                // port mapping not initialized
                return true;
            }
            // Refresh mapping half-way through the lifetime of the mapping (for example,
            // if the mapping is available for 40 seconds, refresh it every 20 seconds)
            long lifetime = 60; // mappedPort.getLifetime()
            if (System.currentTimeMillis() - lastRefresh >= lifetime * 1000 / 2L) {
                // mappedPort = mapper.refreshPort(mappedPort, mappedPort.getLifetime() / 2L);
                logger.debug("RachioNetwork: Port mapping refreshed: {}", mappedPort.toString());
            }
            return true;

        } catch (Exception e) {
            logger.error("RachioNetwork: Unable to update port mapping ({}): {}", mappedPort.toString(),
                    e.getMessage());
        }
        return false;
    } // updatePortMapping()
}
