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

import org.openhab.binding.rachio.internal.api.RachioEvent.RachioApiResult;

/**
 * RachioApiException
 *
 * @author Markus Michels (markus7017) - Initial contribution
 */
public class RachioApiException extends Exception {
	private static final long serialVersionUID = -2579498702258574787L;

	public RachioApiException(String message) {
		super(message);
	}

	public RachioApiException(String message, Throwable throwable) {
		super(message, throwable);
	}

	private RachioApiResult apiResult = new RachioApiResult();

	public RachioApiException(String message, RachioApiResult result) {
		super(message);
		apiResult = result;
	}

	public RachioApiResult getApiResult() {
		return apiResult;
	}

	@Override
	public String getMessage() {
		return super.getMessage();
	}

}
