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
package com.indoqa.nexus.artifact.downloader.configuration;

import java.util.Locale;

public final class ConfigurationException extends Exception {

    private static Locale DEFAULT = Locale.GERMAN;

    private ConfigurationException(String message) {
        super(message);
    }
    private ConfigurationException(String message, Exception e) {
        super(message, e);
    }

    public static ConfigurationException missingParameter(String parameter, String context) {
        return new ConfigurationException(String.format(DEFAULT, "Parameter '%s' is missing in '%s'", parameter, context));
    }

    public static ConfigurationException missingParameter(String parameter, String context, Exception e) {
        return new ConfigurationException(String.format(DEFAULT, "Parameter '%s' is missing in '%s'", parameter, context), e);
    }

    public static ConfigurationException invalidValue(String parameter, String context, String value, String validValues) {
        return new ConfigurationException(String.format(DEFAULT, "Parameter '%s' value '%s' is invalid in '%s'. "
            + "Valid options are '%s'", parameter, value, context, validValues));
    }
}
