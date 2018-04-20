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

package org.openhab.binding.rachio.internal.api;

import static java.net.HttpURLConnection.*;
import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;

import org.openhab.binding.rachio.internal.api.RachioEvent.RachioApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;;

/**
 * The {@link RachioHttp} class contains static methods for communicating HTTP GET
 * and HTTP POST requests.
 *
 * @author Chris Graham - Initial contribution
 * @author Markus Michels - re-used for the Rachio binding
 */
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);

    public static class RachioHttpException extends Exception {
        private static final long serialVersionUID = 1L;

        public RachioHttpException(String message) {
            super(message);
        }
    }

    private int apiCalls = 0;
    private String apikey = "";

    /**
     * Constructor for the Rachio API class to create a connection to the Rachio cloud service.
     *
     * @param key Rachio API Access token (see Web UI)
     * @throws Exception
     */
    public RachioHttp(final String key) throws RachioHttpException {
        apikey = key;
    }

    /**
     * Given a URL and a set parameters, send a HTTP GET request to the URL location created by the URL and parameters.
     *
     * @param url The URL to send a GET request to.
     * @param urlParameters List of parameters to use in the URL for the GET request. Null if no parameters.
     * @return RachioApiResult including GET response, http code etc.
     * @throws Exception
     */
    public RachioApiResult httpGet(String url, String urlParameters) throws RachioHttpException {
        return httpRequest(HTTP_METHOD_GET, url, urlParameters, null);
    }

    /**
     * Given a URL and a set parameters, send a HTTP POST request to the URL location created by the URL and parameters.
     *
     * @param url The URL to send a POST request to.
     * @param urlParameters List of parameters to use in the URL for the POST request. Null if no parameters.
     * @return RachioApiResult including GET response, http code etc.
     * @throws Exception
     */
    public RachioApiResult httpPut(String url, String putData) throws RachioHttpException {
        return httpRequest(HTTP_METHOD_PUT, url, null, putData);
    }

    /**
     * Given a URL and a set parameters, send a HTTP POST request to the URL location created by the URL and parameters.
     *
     * @param url The URL to send a POST request to.
     * @param postData List of parameters to use in the URL for the POST request. Null if no parameters.
     * @return RachioApiResult including GET response, http code etc.
     * @throws Exception
     */
    public RachioApiResult httpPost(String url, String postData) throws RachioHttpException {
        return httpRequest(HTTP_METHOD_POST, url, null, postData);
    }

    /**
     * Given a URL and a set parameters, send a HTTP GET request to the URL location created by the URL and parameters.
     *
     * @param url The URL to send a GET request to.
     * @param urlParameters List of parameters to use in the URL for the GET request. Null if no parameters.
     * @return RachioApiResult including GET response, http code etc.
     * @throws Exception if something went wrong (e.g. unable to connect)
     */
    public RachioApiResult httpDelete(String url, String urlParameters) throws RachioHttpException {
        return httpRequest(HTTP_METHOD_DELETE, url, urlParameters, null);
    }

    /**
     * Given a URL and a set parameters, send a HTTP GET request to the URL location created by the URL and parameters.
     *
     * @param url The URL to send a GET request to.
     * @param urlParameters List of parameters to use in the URL for the GET request. Null if no parameters.
     * @return RachioApiResult including GET response, http code etc.
     * @throws Exception
     */
    protected RachioApiResult httpRequest(String method, String url, String urlParameters, String reqDatas)
            throws RachioHttpException {

        try {
            RachioApiResult result = new RachioApiResult();
            apiCalls++;

            URL location = null;
            if (urlParameters != null) {
                location = new URL(url + "?" + urlParameters);
            } else {
                location = new URL(url);
            }
            result.requestMethod = method;
            result.url = location.toString();
            result.apiCalls = apiCalls;

            HttpURLConnection request = (HttpURLConnection) location.openConnection();
            if (apikey != null) {
                request.setRequestProperty("Authorization", "Bearer " + apikey);
                result.apikey = apikey;
            }
            request.setRequestMethod(method);
            request.setConnectTimeout(15000); // set timeout to 15 seconds
            request.setRequestProperty("User-Agent", WEBHOOK_USER_AGENT);
            request.setRequestProperty("Content-Type", WEBHOOK_APPLICATION_JSON);
            logger.trace("RachioHttp[Call #{}]: Call Rachio cloud service: {} '{}')", apiCalls,
                    request.getRequestMethod(), result.url);
            if (method.equals(HTTP_METHOD_PUT) || method.equals(HTTP_METHOD_POST)) {
                request.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(request.getOutputStream());
                wr.writeBytes(reqDatas);
                wr.flush();
                wr.close();
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
            StringBuilder response = new StringBuilder();

            result.responseCode = request.getResponseCode();
            if (request.getHeaderField(RACHIO_JSON_RATE_LIMIT) != null) {
                result.setRateLimit(request.getHeaderField(RACHIO_JSON_RATE_LIMIT),
                        request.getHeaderField(RACHIO_JSON_RATE_REMAINING),
                        request.getHeaderField(RACHIO_JSON_RATE_RESET));
                if (result.isRateLimitBlocked()) {
                    String message = MessageFormat.format("RachioHttp: Critcal API rate limit: {0} / {1}, reset at {2}",
                            result.rateRemaining, result.rateLimit, result.rateReset);
                    throw new RachioApiException(message, result);
                }
            }

            if ((result.responseCode != HTTP_OK)
                    && ((result.responseCode != HTTP_NO_CONTENT) || (!result.requestMethod.equals(HTTP_METHOD_PUT)
                            && !result.requestMethod.equals(HTTP_METHOD_DELETE)))) {
                String message = MessageFormat.format(
                        "RachioHttp: Error sending HTTP {0} request to {2} - http response code={2}",
                        request.getRequestMethod(), url, result.responseCode);
                throw new RachioHttpException(message);
            }

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            result.resultString = response.toString();
            logger.trace("RachioHttp: {} {} - Response='{}'", request.getRequestMethod(), url, result.resultString);

            return result;
        } catch (Exception e) {
            throw new RachioHttpException(e.getMessage());
        }
    }

} // class
