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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link Parse} class contains static methods for parsing JSON
 * output based on key names.
 *
 * @author Chris Graham - Initial contribution
 * @author Markus Michels - re-used for the Rachio binding, some more functions
 */
public class Parse {
    private static JsonParser jsonParser = new JsonParser();

    /**
     * Parses a string from a JSON string given its key name.
     *
     * @param jsonData The JSON formatted string to parse from.
     * @param keyName The name of the object data to return.
     * @return String value of the objects data.
     */
    public static String jsonSection(String jsonData, String keyName) {
        try {
            JsonElement jelement = jsonParser.parse(jsonData);
            JsonObject jobject = jelement.getAsJsonObject();
            JsonElement jSecion = jobject.get(keyName);
            return jSecion.toString();
        } catch (Exception e) {
            return "{ }";
        }
    }

    /**
     * Parses a string from a JSON string given its key name.
     *
     * @param jsonData The JSON formatted string to parse from.
     * @param keyName The name of the object data to return.
     * @return String value of the objects data.
     */
    public static String jsonString(String jsonData, String keyName, String defaultValue) {
        try {
            JsonElement jelement = jsonParser.parse(jsonData);
            JsonObject jobject = jelement.getAsJsonObject();
            return jobject.get(keyName).getAsString();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Parses an integer from a JSON string given its key name.
     *
     * @param jsonData The JSON formatted string to parse from.
     * @param keyName The name of the object data to return.
     * @return int value of the objects data.
     */
    public static int jsonInt(String jsonData, String keyName, int defaultValue) {
        try {
            JsonElement jelement = jsonParser.parse(jsonData);
            JsonObject jobject = jelement.getAsJsonObject();
            return jobject.get(keyName).getAsInt();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Parses an integer from a JSON string given its key name.
     *
     * @param jsonData The JSON formatted string to parse from.
     * @param keyName The name of the object data to return.
     * @return double value of the objects data.
     */
    public static double jsonDouble(String jsonData, String keyName, Double defaultValue) {
        try {
            JsonElement jelement = jsonParser.parse(jsonData);
            JsonObject jobject = jelement.getAsJsonObject();
            return jobject.get(keyName).getAsDouble();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean jsonBoolean(String jsonData, String keyName, boolean defaultValue) {
        try {
            String val = jsonString(jsonData, keyName, "false");
            if (val.equals("true") || val.equals("TRUE") || val.equals("1") || val.equals("on") || val.equals("ON")) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Parses an int from a JSON array given its key name in the JSON string.
     *
     * @param jsonData The JSON formatted string to parse from.
     * @param keyName The name of the object array to search through.
     * @param index Index (starting at 0) number of the item in the JSON array to return.
     * @return int value of the objects data.
     */
    public static int jsonIntAtArrayIndex(String jsonData, String keyName, int index) {
        JsonElement jelement = jsonParser.parse(jsonData);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonArray jarray = jobject.get(keyName).getAsJsonArray();
        return jarray.get(index).getAsInt();
    }

    /**
     * Parses a String from a JSON array given its key name in the JSON string.
     *
     * @param jsonData The JSON formatted string to parse from.
     * @param keyName The name of the object array to search through.
     * @param index Index (starting at 0) number of the item in the JSON array to return.
     * @return String value of the objects data.
     */
    public static String jsonStringAtArrayIndex(String jsonData, String keyName, int index) {
        JsonElement jelement = jsonParser.parse(jsonData);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonArray jarray = jobject.get(keyName).getAsJsonArray();
        return jarray.get(index).getAsString();
    }

    public static JsonArray jsonObjectArray(String jsonData, String keyName) {
        JsonElement jelement = jsonParser.parse(jsonData);
        JsonObject jobject = jelement.getAsJsonObject();
        return jobject.get(keyName).getAsJsonArray();
    }

    /**
     * Parses an int array from a JSON string given its key name.
     *
     * @param jsonData The JSON formatted string to parse from.
     * @param keyName The name of the object array to return.
     * @return List of Integers with the values of a JSON Array.
     */
    public static List<Integer> jsonIntArray(String jsonData, String keyName) {
        List<Integer> returnList = new ArrayList<Integer>();

        JsonElement jelement = jsonParser.parse(jsonData);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonArray jarray = jobject.get(keyName).getAsJsonArray();

        for (int i = 0; i < jarray.size(); i++) {
            returnList.add(jarray.get(i).getAsInt());
        }

        return returnList;
    }

    /**
     * Parses an String array from a JSON string given its key name.
     *
     * @param jsonData The JSON formatted string to parse from.
     * @param keyName The name of the object array to search through.
     * @return List of Strings with the values of a JSON Array.
     */
    public static List<String> jsonStringArray(String jsonData, String keyName) {
        List<String> returnList = new ArrayList<String>();

        JsonElement jelement = jsonParser.parse(jsonData);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonArray jarray = jobject.get(keyName).getAsJsonArray();

        int sz = jarray.size();
        for (int i = 0; i < sz; i++) {
            JsonElement o = jarray.get(i);
            String s = (o.getAsString());
            returnList.add(s);
        }

        return returnList;
    }
}
