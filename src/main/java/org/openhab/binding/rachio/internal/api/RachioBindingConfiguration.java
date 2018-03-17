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
package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.binding.rachio.handler.RachioDeviceHandler;

/**
 * The {@link RachioDeviceHandler} contains the binding configuration and default values. The field names represent the
 * configuration names, do not rename them if you don't intend to break the configuration interface.
 *
 * @author Markus Michels
 */
public class RachioBindingConfiguration {
    public static final String PARAM_APIKEY = "apikey";
    public static final String PARAM_POLLING_INTERVAL = "pollingInterval";
    public static final String PARAM_DEF_RUNTIME = "defaultRuntime";
    public static final String PARAM_CALLBACK_URL = "callbackUrl";
    public static final String PARAM_IPFILTER = "ipFilter";

    public static final String ERR_APIKEY = "ERROR: No/invalid APIKEY in configuration, check services/rachio.cfg";

    public String apiKey = "";
    public int pollingInterval = 60;
    public int defaultRuntime = 120;
    public String callbackUrl = "";
    public Boolean clearAllCallbacks = false;
    public String ipFilter = "";

    public void update(@NonNull RachioBindingConfiguration newConfiguration) {
        this.apiKey = newConfiguration.apiKey;
        this.pollingInterval = newConfiguration.pollingInterval;
        this.defaultRuntime = newConfiguration.defaultRuntime;
        this.callbackUrl = newConfiguration.callbackUrl;
        this.clearAllCallbacks = newConfiguration.clearAllCallbacks;
        this.ipFilter = newConfiguration.ipFilter;
    }
}
