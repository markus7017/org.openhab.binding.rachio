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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.openhab.binding.rachio.internal.api.RachioEvent;

import com.google.gson.Gson;

/**
 * Subset of event properties that will be posted to the thing's event channel
 *
 * @author Markus Michels (markus7017) - Initial contribution
 */
@SuppressWarnings("unused")
public class RachioEventString {
    private final String eventDate;
    private final String summary;
    private final String topic;
    private final String type;
    private final String subType;

    private Gson gson = new Gson();

    public RachioEventString(RachioEvent event) {
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(event.eventDate, 0, ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        eventDate = dateTime.format(formatter);
        summary = event.summary;
        topic = event.topic;
        type = event.type;
        subType = event.subType;
    }

    public String toJson() {
        return gson.toJson(this);
    }
}
