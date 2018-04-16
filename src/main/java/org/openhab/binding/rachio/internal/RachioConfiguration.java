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
package org.openhab.binding.rachio.internal;

import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.openhab.binding.rachio.handler.RachioDeviceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioDeviceHandler} contains the binding configuration and default values. The field names represent the
 * configuration names, do not rename them if you don't intend to break the configuration interface.
 *
 * @author Markus Michels (markus7017)
 */
public class RachioConfiguration {
    private final Logger logger = LoggerFactory.getLogger(RachioConfiguration.class);

    public static final String ERR_APIKEY = "ERROR: No/invalid APIKEY in configuration, check services/rachio.cfg";

    public String apikey = "";
    public int pollingInterval = DEFAULT_POLLING_INTERVAL;
    public int defaultRuntime = DEFAULT_ZONE_RUNTIME;
    public String callbackUrl = "";
    public Boolean clearAllCallbacks = false;
    public String ipFilter = "";

    public void updateConfig(Map<String, Object> config) {
        for (HashMap.Entry<String, Object> ce : config.entrySet()) {
            String key = ce.getKey();
            String value = ce.getValue().toString();
            if (key.equalsIgnoreCase("component.name") || key.equalsIgnoreCase("component.name")) {
                continue;
            }

            if (key.equalsIgnoreCase("service.pid")) {
                logger.debug("Rachio: Binding configuration:");
            }
            logger.debug("  {}={}", key, value);

            if (key.equalsIgnoreCase(PARAM_APIKEY)) {
                apikey = value;
            } else if (key.equalsIgnoreCase(PARAM_POLLING_INTERVAL)) {
                this.pollingInterval = Integer.parseInt(value);
            } else if (key.equalsIgnoreCase(PARAM_DEF_RUNTIME)) {
                this.defaultRuntime = Integer.parseInt(value);
            } else if (key.equalsIgnoreCase(PARAM_CALLBACK_URL)) {
                this.callbackUrl = value;
            } else if (key.equalsIgnoreCase(PARAM_IPFILTER)) {
                this.ipFilter = value;
            } else if (key.equalsIgnoreCase(PARAM_CLEAR_CALLBACK)) {
                String str = value;
                this.clearAllCallbacks = str.toLowerCase().equals("true");
            }
        }
    } // RachioBindingConfiguration
}
