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

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.binding.rachio.internal.api.RachioZone;

/**
 * The {@link RachioStatusListener} is notified when a chamber is updated.
 *
 * @author Gregory Moyer - Initial contribution
 */
public interface RachioStatusListener {
    /**
     * This method will be called whenever a new device/zone status is received by the cloud handler.
     *
     * @param updatedDev On device updates this is the new RachioDevice information, maybe null!!
     * @param supdatedZone On zone updates this is the new RachioZone information, maybe null!!
     */

    public boolean onThingStateChangedl(@Nullable RachioDevice updatedDev, @Nullable RachioZone updatedZone);
}
