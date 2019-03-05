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
package com.indoqa.nexus.downloader.main.resources;

import org.apache.commons.lang3.StringUtils;

import spark.Request;

public final class QueryParameterHelper {

    private QueryParameterHelper() {
        // hide utility class constructor
    }

    public static int getCount(Request req) {
        return getInt(req, "count", 0, Integer.MAX_VALUE, 10);
    }

    public static int getInt(Request request, String name, int minValue, int maxValue, int defaultValue) {
        String value = request.queryParams(name);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }

        try {
            int actualValue = Integer.parseInt(value);
            if (actualValue < minValue || actualValue > maxValue) {
                throw BadRequestException.invalidValue(name, actualValue, minValue, maxValue);
            }

            return actualValue;
        } catch (NumberFormatException e) {
            throw BadRequestException.invalidInteger(name, value);
        }
    }

    public static int getStart(Request req) {
        return getInt(req, "start", 0, Integer.MAX_VALUE, 0);
    }
}
