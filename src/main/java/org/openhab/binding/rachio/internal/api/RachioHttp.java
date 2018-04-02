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

import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioHttp} class contains static methods for communicating HTTP GET
 * and HTTP POST requests.
 *
 * @author Chris Graham - Initial contribution
 * @author Markus Michels - re-used for the Rachio binding
 */
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);

    private int apiCalls = 0;

    private static final String HTTP_GET = "GET";
    private static final String HTTP_PUT = "PUT";
    private static final String HTTP_POST = "POST";
    private static final String HTTP_DELETE = "DELETE";

    private String apikey = "";
    private static Integer rateLimit = 0;
    private static Integer rateRemaining = 0;
    private static String rateResetTime = "";

    /**
     * Constructor for the Rachio API class to create a connection to the Rachio cloud service.
     *
     * @param key Rachio API Access token (see Web UI)
     * @throws Exception
     */
    public RachioHttp(final String key) throws Exception {
        apikey = key;
    }

    /**
     * Given a URL and a set parameters, send a HTTP GET request to the URL location created by the URL and parameters.
     *
     * @param url The URL to send a GET request to.
     * @param urlParameters List of parameters to use in the URL for the GET request. Null if no parameters.
     * @return String contents of the response for the GET request.
     * @throws Exception
     */
    public String sendHttpGet(String url, String urlParameters) throws Exception {
        URL location = null;

        if (urlParameters != null) {
            location = new URL(url + "?" + urlParameters);
        } else {
            location = new URL(url);
        }

        HttpURLConnection request = (HttpURLConnection) location.openConnection();
        request.setRequestMethod(HTTP_GET);
        request.setRequestProperty("User-Agent", WEBHOOK_USER_AGENT);
        if (apikey != null) {
            request.setRequestProperty("Authorization", "Bearer " + apikey);
        }

        // Send GET
        apiCalls++;
        logger.debug("RachioHttp: Call #{} to Rachio cloud service: {} '{}')", apiCalls, request.getRequestMethod(),
                location.toString());

        int responseCode = request.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception(
                    "RachioHttp: Error sending HTTP GET request to " + url + ". Got response code: " + responseCode);
        }
        saveRateLimit(request);

        BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();

        logger.trace("RachioHttp: {} {} - Response='{}'", request.getRequestMethod(), url, response.toString());
        return response.toString();
    } // sendHttpGet

    /**
     * Given a URL and a set parameters, send a HTTP POST request to the URL location created by the URL and parameters.
     *
     * @param url The URL to send a POST request to.
     * @param urlParameters List of parameters to use in the URL for the POST request. Null if no parameters.
     * @return String contents of the response for the POST request.
     * @throws Exception
     */
    public String sendHttpPut(String url, String urlParameters) throws Exception {
        URL location = new URL(url);
        HttpsURLConnection request = (HttpsURLConnection) location.openConnection();
        request.setRequestMethod(HTTP_PUT);
        request.setRequestProperty("User-Agent", WEBHOOK_USER_AGENT);
        request.setRequestProperty("Content-Type", WEBHOOK_APPLICATION_JSON);
        if (apikey != null) {
            request.setRequestProperty("Authorization", "Bearer " + apikey);
        }

        // Send PUT request
        apiCalls++;
        logger.debug("RachioHttp: Call #{} to Rachio cloud service: {} '{}')", apiCalls, request.getRequestMethod(),
                location.toString());
        request.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(request.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        int responseCode = request.getResponseCode();
        if ((responseCode != HttpURLConnection.HTTP_OK) && (responseCode != HttpURLConnection.HTTP_NO_CONTENT)) {
            throw new Exception(
                    "RachioHttp: Error sending HTTP PUT request to " + url + ". Got responce code: " + responseCode);
        }
        saveRateLimit(request);

        BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        logger.trace("RachioHttp: {} {} - Response='{}'", request.getRequestMethod(), url, response.toString());
        return response.toString();
    } // sendHttpPUT

    /**
     * Given a URL and a set parameters, send a HTTP DELETE request to the URL location created by the URL and
     * parameters.
     *
     * @param url The URL to send a DELETE request to.
     * @param urlParameters List of parameters to use in the URL for the DELETE request. Null if no parameters.
     * @return String contents of the response for the DELETE request.
     * @throws Exception
     */
    public String sendHttpDelete(String url, String urlParameters) throws Exception {
        URL location = null;

        if (urlParameters != null) {
            location = new URL(url + "?" + urlParameters);
        } else {
            location = new URL(url);
        }

        HttpURLConnection request = (HttpURLConnection) location.openConnection();
        request.setRequestMethod(HTTP_DELETE);
        request.setRequestProperty("User-Agent", WEBHOOK_USER_AGENT);
        if (apikey != null) {
            request.setRequestProperty("Authorization", "Bearer " + apikey);
        }

        // Send DELETE request
        apiCalls++;
        logger.debug("RachioHttp: Call #{} to Rachio cloud service: {} '{}')", apiCalls, request.getRequestMethod(),
                location.toString());
        BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuilder response = new StringBuilder();
        int responseCode = request.getResponseCode();
        if ((responseCode != HttpURLConnection.HTTP_OK) && (responseCode != HttpURLConnection.HTTP_NO_CONTENT)) {
            throw new Exception("RachioHttp: Error sending HTTP DELETE returned code " + responseCode + ", url=" + url);
        }
        saveRateLimit(request);

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();

        logger.trace("RachioHttp: {} {} - Response='{}'", request.getRequestMethod(), url, response.toString());
        return response.toString();
    } // sendHttpDelete

    /**
     * Given a URL and a set parameters, send a HTTP POST request to the URL location created by the URL and parameters.
     *
     * @param url The URL to send a POST request to.
     * @param urlParameters List of parameters to use in the URL for the POST request. Null if no parameters.
     * @return String contents of the response for the POST request.
     * @throws Exception
     */
    public String sendHttpPost(String url, String urlParameters) throws Exception {
        URL location = new URL(url);
        HttpsURLConnection request = (HttpsURLConnection) location.openConnection();
        request.setRequestMethod(HTTP_POST);
        request.setRequestProperty("User-Agent", WEBHOOK_USER_AGENT);
        request.setRequestProperty("Content-Type", WEBHOOK_APPLICATION_JSON);
        if (apikey != null) {
            request.setRequestProperty("Authorization", "Bearer " + apikey);
        }

        // Send post request
        apiCalls++;
        logger.debug("RachioHttp: Call #{} to Rachio cloud service: {} '{}')", apiCalls, request.getRequestMethod(),
                location.toString());
        request.setDoOutput(true);
        request.setRequestMethod("POST");
        DataOutputStream wr = new DataOutputStream(request.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        int responseCode = request.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("Error sending HTTP POST request to " + url + ". Got responce code: " + responseCode);
        }
        saveRateLimit(request);

        BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        logger.trace("RachioHttp: {} {} - Response='{}'", request.getRequestMethod(), url, response.toString());
        return response.toString();
    } // sendHttpPost

    private boolean saveRateLimit(HttpURLConnection request) {
        String strLimit = request.getHeaderField(RACHIO_JSON_RATE_LIMIT);
        String strRemaining = request.getHeaderField(RACHIO_JSON_RATE_REMAINING);
        String strResetTime = request.getHeaderField(RACHIO_JSON_RATE_RESET);
        if (strLimit != null) {
            rateLimit = Integer.parseInt(strLimit);
            if (strRemaining != null) {
                rateRemaining = Integer.parseInt(strRemaining);
            }
            if (strResetTime != null) {
                rateResetTime = strResetTime;
            }
        }
        if (rateLimit != 0) {
            logger.debug("RachioApi: Rate Limit={}, remaining API calls={}, reset at {}", rateLimit, rateRemaining,
                    rateResetTime);
            apiCalls = rateLimit - rateRemaining;
        }
        return true;
    } // saveRateLimit

    public boolean checkRateLimit(int treshhold) {
        if ((rateLimit != 0) && (rateRemaining < treshhold)) {
            logger.info(
                    "RachioApi: Remaining number of api calls is below treshhold (limit={}, calls={}, remaining={}, reset at {}",
                    rateLimit, apiCalls, rateRemaining, rateResetTime);
            return false;
        }
        return true;
    }
} // class
