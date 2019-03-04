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
package com.indoqa.nexus.downloader.client.json;

import java.util.List;

import org.json.JSONObject;

public final class JsonDownloadExtractor {

    public static List<JSONObject> getItems(JSONObject jsonObject) {
        return JsonHelper.getJsonArrayAsList(jsonObject, "items");
    }

    public static List<JSONObject> getAssets(JSONObject jsonObject) {
        return JsonHelper.getJsonArrayAsList(jsonObject, "assets");
    }

    public static String getVersion(JSONObject item) {
        return JsonHelper.getString(item, "version");
    }

    public static String getDownloadUrl(JSONObject asset) {
        return JsonHelper.getString(asset, "downloadUrl");
    }

    public static String getSha1(JSONObject asset) {
        return JsonHelper.getString(JsonHelper.getJsonObject(asset, "checksum").get(), "sha1");
    }

    public static String getContinuationToken(JSONObject jsonObject) {
        return JsonHelper.getString(jsonObject, "continuationToken", null);
    }

}
