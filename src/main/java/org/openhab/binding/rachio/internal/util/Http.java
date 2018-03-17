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

    private static final String HTTP_GET = "GET";
    private static final String HTTP_PUT = "PUT";
    private static final String HTTP_POST = "POST";
    private static final String HTTP_DELETE = "DELETE";
    private static final String USER_AGENT = "Mozilla/5.0";

    private String apiKey = "";

    /**
     * Constructor for the Rachio API class to create a connection to the Rachio cloud service.
     *
     * @param key Rachio API Access token (see Web UI)
     * @throws Exception
     */
    public Http(final String key) throws Exception {
        apiKey = key;
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
        logger.trace("RachioHttp: Call Rachio cloud service (url='{}')", location.toString());

        HttpURLConnection connection = (HttpURLConnection) location.openConnection();
        connection.setRequestMethod(HTTP_GET);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        if (apiKey != null) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("Error sending HTTP GET request to " + url + ". Got response code: " + responseCode);
        }

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();

        logger.trace("RachioHttp: Response='{}'", response.toString());
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
        HttpsURLConnection connection = (HttpsURLConnection) location.openConnection();
        connection.setRequestMethod(HTTP_PUT);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Content-Type", "application/json");
        if (apiKey != null) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        }

        // Send post request
        connection.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        int responseCode = connection.getResponseCode();
        if ((responseCode != HttpURLConnection.HTTP_OK) && (responseCode != HttpURLConnection.HTTP_NO_CONTENT)) {
            throw new Exception("Error sending HTTP PUT request to " + url + ". Got responce code: " + responseCode);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

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
        HttpsURLConnection connection = (HttpsURLConnection) location.openConnection();
        connection.setRequestMethod(HTTP_POST);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Content-Type", "application/json");
        if (apiKey != null) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        }

        // Send post request
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("Error sending HTTP POST request to " + url + ". Got responce code: " + responseCode);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

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

        HttpURLConnection connection = (HttpURLConnection) location.openConnection();
        connection.setRequestMethod(HTTP_DELETE);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        if (apiKey != null) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        }

        int responseCode = connection.getResponseCode();
        if ((responseCode != HttpURLConnection.HTTP_OK) && (responseCode != HttpURLConnection.HTTP_NO_CONTENT)) {

            throw new Exception("Error sending HTTP DELETE request to " + url + ". Got response code: " + responseCode);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();

        logger.trace("RachioHttp: Response='{}'", response.toString());
        return response.toString();
    } // sendHttpGet

} // class
