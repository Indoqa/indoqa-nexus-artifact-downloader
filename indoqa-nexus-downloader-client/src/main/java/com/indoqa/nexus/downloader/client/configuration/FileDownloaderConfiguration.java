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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.indoqa.nexus.downloader.client.json.JsonHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDownloaderConfiguration implements DownloaderConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloaderConfiguration.class);
    public static final String BASE_CONFIG = "baseConfig";
    public static final String OLD_ENTRIES = "oldEntries";

    private List<ArtifactConfiguration> artifactConfigurations = new ArrayList<>();
    private Path basePath;

    private String username;
    private String password;
    private String baseUrl;
    private boolean createRelativeSymlinks;
    private int countToKeep;
    private boolean deleteOld;
    private boolean verbose;
    private boolean moreVerbose;
    private String nexusPathRestSearch;

    public static ConfigurationHolder create(String[] args) {
        Path path = Paths.get(args[0]);
        if (Files.exists(path)) {
            return readConfiguration(path);
        }
        return ConfigurationHolder.help("No configuration file found '" + path.toAbsolutePath() + "'");
    }

    protected static ConfigurationHolder readConfiguration(Path path) {
        try{
            return readConfiguration(Files.readAllBytes(path));
        } catch (IOException | JSONException e) {
            return ConfigurationHolder.error("Failed to parse configuration file '" + path + "': " + e.getMessage(), e);
        }
    }

    protected static ConfigurationHolder readConfiguration(byte[] bytes) {
        try {
            JSONObject jsonObject = new JSONObject(new String(bytes, Charset.forName("UTF-8")));
            FileDownloaderConfiguration result = new FileDownloaderConfiguration();
            extractConfigParameters(jsonObject, result);
            extractArtifactConfigurations(jsonObject, result);
            return ConfigurationHolder.config(result);
        } catch (JSONException | ConfigurationException e) {
            return ConfigurationHolder.error("Failed to parse configuration file: " + e.getMessage(), e);
        }
    }

    private static void extractArtifactConfigurations(JSONObject jsonObject, FileDownloaderConfiguration result)
        throws ConfigurationException {
        try {
            JSONArray artifacts = jsonObject.getJSONArray("mavenArtifacts");
            extractArtifactConfigurations(result, artifacts);
        } catch (JSONException e) {
            LOGGER.error("Could not extract artifacts configuration from config. {}", e.getMessage());
            throw ConfigurationException.missingParameter("mavenArtifacts", "config", e);
        }
    }

    private static void extractArtifactConfigurations(FileDownloaderConfiguration result, JSONArray artifacts) {
        for (int i = 0; i < artifacts.length(); i++) {
            try {
                result.add(FileArtifactConfiguration.create(artifacts.getJSONObject(i)));
            } catch (JSONException | ConfigurationException e) {
                LOGGER.error("Could not read artifact configuration number {}, ", i, e);
            }
        }
    }

    private static void extractConfigParameters(JSONObject jsonObject, FileDownloaderConfiguration result)
        throws ConfigurationException {
        JSONObject config = JsonHelper
            .getJsonObject(jsonObject, BASE_CONFIG)
            .orElseThrow(() -> ConfigurationException.missingParameter(BASE_CONFIG, "Configuration file"));
        result.setUsername(getConfigParameter(config, "nexusUsername"));
        result.setPassword(getConfigParameter(config, "nexusPassword"));
        result.setBaseUrl(getConfigParameter(config, "nexusUrl"));
        result.setCreateRelativeSymlinks(getBooleanConfigParameter(BASE_CONFIG, config, "createRelativeSymlinks"));

        result.setVerbose(getBooleanConfigParameter(BASE_CONFIG, config, "verbose"));
        result.setMoreVerbose(getBooleanConfigParameter(BASE_CONFIG, config, "moreVerbose"));

        JsonHelper.getOptionalString(config, "basePath").ifPresent(value -> result.setBasePath(Paths.get(value)));
        JsonHelper.getOptionalString(config, "nexusPathRestSearch").ifPresent(result::setNexusPathRestSearch);

        Optional<JSONObject> oldEntries = JsonHelper.getJsonObject(config, OLD_ENTRIES);
        if (oldEntries.isPresent()) {
            result.setCountToKeep(getIntegerParameter(OLD_ENTRIES, oldEntries.get(), "countToKeep"));
            result.setDeleteOld(getBooleanConfigParameter(OLD_ENTRIES, oldEntries.get(), "delete"));
        }
    }

    private static int getIntegerParameter(String context, JSONObject object, String parameter) throws ConfigurationException {
        try {
            return object.getInt(parameter);
        } catch (JSONException e) {
            throw ConfigurationException.missingParameter(parameter, context, e);
        }
    }

    private static String getConfigParameter(JSONObject config, String parameter) throws ConfigurationException {
        try {
            return config.getString(parameter);
        } catch (JSONException e) {
            throw ConfigurationException.missingParameter(parameter, BASE_CONFIG, e);
        }
    }

    private static boolean getBooleanConfigParameter(String context, JSONObject config, String parameter)
        throws ConfigurationException {
        try {
            return config.getBoolean(parameter);
        } catch (JSONException e) {
            throw ConfigurationException.missingParameter(parameter, context, e);
        }
    }

    private void add(FileArtifactConfiguration fileArtifactConfiguration) {
        this.artifactConfigurations.add(fileArtifactConfiguration);
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public boolean verbose() {
        return this.verbose;
    }

    public void setMoreVerbose(boolean moreVerbose) {
        this.moreVerbose = moreVerbose;
    }

    @Override
    public boolean moreVerbose() {
        return this.moreVerbose;
    }

    public void setBasePath(Path basePath) {
        this.basePath = basePath;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String getNexusBaseUrl() {
        return this.baseUrl;
    }

    public void setCreateRelativeSymlinks(boolean createRelativeSymlinks) {
        this.createRelativeSymlinks = createRelativeSymlinks;
    }

    @Override
    public boolean createRelativeSymlinks() {
        return this.createRelativeSymlinks;
    }

    public void setDeleteOld(boolean deleteOld) {
        this.deleteOld = deleteOld;
    }

    @Override
    public boolean deleteOldEntries() {
        return this.deleteOld;
    }

    public void setCountToKeep(int countToKeep) {
        this.countToKeep = countToKeep;
    }

    @Override
    public int getKeepNumberOfOldEntries() {
        return this.countToKeep;
    }

    @Override
    public Iterable<ArtifactConfiguration> getArtifactConfigurations() {
        return artifactConfigurations;
    }

    @Override
    public Path getWorkingPath() {
        if (this.basePath == null) {
            return DownloaderConfiguration.super.getWorkingPath();
        }
        return basePath;
    }

    public void setNexusPathRestSearch(String nexusPathRestSearch) {
        this.nexusPathRestSearch = nexusPathRestSearch;
    }

    @Override
    public String getNexusPathRestSearch() {
        if (this.nexusPathRestSearch == null) {
            return DownloaderConfiguration.super.getNexusPathRestSearch();
        }
        return nexusPathRestSearch;
    }
}
