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
package com.indoqa.nexus.downloader.client.configuration;

import java.io.IOException;
import java.util.Optional;

import org.apache.http.HttpHeaders;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpDownloaderConfiguration extends FileDownloaderConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpDownloaderConfiguration.class);
    private static final String DISABLE_SSL = "disable-ssl";

    public static final String DEFAULT_JSON_FILE = "default.json";

    public static final String HEADER_IDQ_NEXUS_DL_PROJECT = "IDQ-NEXUS-DL-PROJECT";
    public static final String HEADER_IDQ_NEXUS_DL_VARIANT = "IDQ-NEXUS-DL-VARIANT";
    public static final String HEADER_IDQ_NEXUS_DL_HOST = "IDQ-NEXUS-DL-HOST";

    private static String indoqaDownloaderConfigHost = "downloader-config.indoqa.com";

    public static ConfigurationHolder create(String[] args) {
        return create(args, Optional.empty(), Optional.empty());
    }

    public static ConfigurationHolder create(String[] args, Optional<String> downloaderConfigHost, Optional<String> hostname) {
        return create(args, Executor.newInstance(), downloaderConfigHost, hostname);
    }

    public static ConfigurationHolder create(String[] args, Executor executor, Optional<String> downloaderConfigHost, Optional<String> hostname) {
        String uuid = args[0];
        Optional<String> variant = Optional.of(DEFAULT_JSON_FILE);
        if (args.length == 2) {
            variant = getVariant(args[1]);
        }

        try {
            byte[] bytes = downloadConfiguration(executor, uuid, variant, downloaderConfigHost.orElse(indoqaDownloaderConfigHost), hostname);
            return FileDownloaderConfiguration.readConfiguration(bytes);
        } catch (IOException e) {
            return ConfigurationHolder.error("Could not download configuration.", e);
        }
    }

    private static Optional<String> getVariant(String variant) {
        if (variant.isEmpty()) {
            return Optional.of(DEFAULT_JSON_FILE);
        }
        if (variant.indexOf(".json") > -1) {
            return Optional.of(variant);
        }
        return Optional.of(variant + ".json");
    }

    private static byte[] downloadConfiguration(Executor executor, String project, Optional<String> variant,
        String downloaderConfigHost, Optional<String> hostname) throws IOException {
        Request get = Request.Get(createUrl(downloaderConfigHost));
        get.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        get.addHeader(HEADER_IDQ_NEXUS_DL_PROJECT, project);
        variant.ifPresent(v -> get.addHeader(HEADER_IDQ_NEXUS_DL_VARIANT, v));

        if (!hostname.isPresent()) {
            hostname = Hostname.getHostname();
        }
        hostname.ifPresent(h -> get.addHeader(HEADER_IDQ_NEXUS_DL_HOST, h));

        LOGGER.debug("Request: {}", get);
        LOGGER.debug("Project {}, hostname {}", project, hostname);

        Response response = executor.execute(get);
        return response.returnContent().asBytes();
    }

    private static String createUrl(String downloaderConfigHost) {
        if (Boolean.parseBoolean(System.getProperty(DISABLE_SSL, Boolean.FALSE.toString()))) {
            return "http://" + downloaderConfigHost + "/configuration";
        }
        return "https://" + downloaderConfigHost + "/configuration";
    }
}
