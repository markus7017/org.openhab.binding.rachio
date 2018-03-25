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

package org.openhab.binding.rachio.internal.util;

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
 * The {@link Http} class contains static methods for communicating HTTP GET
 * and HTTP POST requests.
 *
 * @author Chris Graham - Initial contribution
 * @author Markus Michels - re-used for the Rachio binding
 */
public class Http {
    private final Logger logger = LoggerFactory.getLogger(Http.class);

    private int apiCalls = 0;

    private static final String HTTP_GET = "GET";
    private static final String HTTP_PUT = "PUT";
    private static final String HTTP_POST = "POST";
    private static final String HTTP_DELETE = "DELETE";

    private String apikey = "";

    /**
     * Constructor for the Rachio API class to create a connection to the Rachio cloud service.
     *
     * @param key Rachio API Access token (see Web UI)
     * @throws Exception
     */
    public Http(final String key) throws Exception {
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

        apiCalls++;
        logger.trace("RachioHttp[Call #{}]: Call Rachio cloud service: {} '{}')", apiCalls, request.getRequestMethod(),
                location.toString());
        BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuilder response = new StringBuilder();

        int responseCode = request.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception(
                    "RachioHttp: Error sending HTTP GET request to " + url + ". Got response code: " + responseCode);
        }

        String rateLimit = request.getHeaderField(RACHIO_JSON_RATE_LIMIT);
        String rateRemaining = request.getHeaderField(RACHIO_JSON_RATE_REMAINING);
        String rateReset = request.getHeaderField(RACHIO_JSON_RATE_RESET);
        if (rateLimit != null) {
            logger.debug("RachioHttp: API rate limit={}, remaining={}, reset at {}", rateLimit, rateRemaining,
                    rateReset);
        }

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
        logger.trace("RachioHttp[Call #{}]: {} Call Rachio cloud service: '{}')", apiCalls, request.getRequestMethod(),
                request.toString());
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
        logger.trace("RachioHttp[Call #{}]: {} Call Rachio cloud service: '{}')", apiCalls, request.getRequestMethod(),
                request.toString());
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

    /**
     * Given a URL and a set parameters, send a HTTP GET request to the URL location created by the URL and parameters.
     *
     * @param url The URL to send a GET request to.
     * @param urlParameters List of parameters to use in the URL for the GET request. Null if no parameters.
     * @return String contents of the response for the GET request.
     * @throws Exception
     */
    public String sendHttpDelete(String url, String urlParameters) throws Exception {
        URL location = null;

        if (urlParameters != null) {
            location = new URL(url + "?" + urlParameters);
        } else {
            location = new URL(url);
        }
        logger.trace("RachioHttp: Call Rachio cloud service (url='{}')", location.toString());

        HttpURLConnection request = (HttpURLConnection) location.openConnection();
        request.setRequestMethod(HTTP_DELETE);
        request.setRequestProperty("User-Agent", WEBHOOK_USER_AGENT);
        if (apikey != null) {
            request.setRequestProperty("Authorization", "Bearer " + apikey);
        }

        // Send DELETE request
        apiCalls++;
        logger.trace("RachioHttp[Call #{}]: {} Call Rachio cloud service: '{}')", apiCalls, request.getRequestMethod(),
                request.toString());
        request.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(request.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        int responseCode = request.getResponseCode();
        if ((responseCode != HttpURLConnection.HTTP_OK) && (responseCode != HttpURLConnection.HTTP_NO_CONTENT)) {

            throw new Exception(
                    "RachioHttp: Error sending HTTP DELETE request to " + url + ". Got response code: " + responseCode);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();

        logger.trace("RachioHttp: {} {} - Response='{}'", request.getRequestMethod(), url, response.toString());
        return response.toString();
    } // sendHttpDelete

} // class
