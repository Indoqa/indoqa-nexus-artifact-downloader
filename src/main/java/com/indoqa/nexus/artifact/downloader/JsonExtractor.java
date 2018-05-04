/*
 * Licensed to the Indoqa Software Design und Beratung GmbH (Indoqa) under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Indoqa licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.indoqa.nexus.artifact.downloader;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;

public class JsonExtractor {

    public static List<JSONObject> getItems(JSONObject jsonObject) {
        return getJsonArrayAsList(jsonObject, "items");
    }

    public static List<JSONObject> getAssets(JSONObject jsonObject) {
        return getJsonArrayAsList(jsonObject, "assets");
    }

    public static String getVersion(JSONObject item) {
        return getString(item, "version");
    }

    public static String getDownloadUrl(JSONObject asset) {
        return getString(asset, "downloadUrl");
    }

    public static String getSha1(JSONObject asset) {
        return getString(getJsonObject(asset, "checksum"), "sha1");
    }

    public static String getContinuationToken(JSONObject jsonObject) {
        return getString(jsonObject, "continuationToken", null);
    }

    private static JSONObject getJsonObject(JSONObject jsonObject, String key) {
        if (!jsonObject.has(key) || jsonObject.isNull(key)) {
            return new JSONObject();
        }
        return jsonObject.getJSONObject(key);
    }

    private static String getString(JSONObject jsonObject, String key, String defaultValue) {
        if (!jsonObject.has(key) || jsonObject.isNull(key)) {
            return defaultValue;
        }
        return jsonObject.getString(key);
    }

    private static String getString(JSONObject jsonObject, String key) {
        return getString(jsonObject, key, "");
    }

    private static List<JSONObject> getJsonArrayAsList(JSONObject jsonObject, String key) {
        if (!jsonObject.has(key)) {
            return Collections.emptyList();
        }
        return jsonObject
            .getJSONArray(key)
            .toList()
            .stream()
            .filter(e -> e instanceof Map)
            .map(e -> new JSONObject((Map) e))
            .collect(Collectors.toList());
    }
}
