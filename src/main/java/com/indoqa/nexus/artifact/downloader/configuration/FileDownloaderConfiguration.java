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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.indoqa.nexus.artifact.downloader.json.JsonHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDownloaderConfiguration implements DownloaderConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloaderConfiguration.class);

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

    public static final String HELP = "No (valid) configuration file was found working directory (downloader.json)\n"
        + "No (valid) configuration file path was supplied as first argument indoqa-nexus-downloader.jar [path]";

    public static ConfigurationHolder create(String[] args) {
        if (args.length >= 1) {
            Path path = Paths.get(args[0]);
            if (Files.exists(path)) {
                return readConfigurationFile(path);
            }
        }
        Path path = Paths.get("./downloader.json");
        if (Files.exists(path)) {
            return readConfigurationFile(path);
        }
        return ConfigurationHolder.help(HELP);
    }

    private static ConfigurationHolder readConfigurationFile(Path path) {
        try {
            JSONObject jsonObject = new JSONObject(new String(Files.readAllBytes(path), Charset.forName("UTF-8")));
            FileDownloaderConfiguration result = new FileDownloaderConfiguration();
            extractConfigParameters(jsonObject, result);
            extractArtifactConfigurations(jsonObject, result);
            return ConfigurationHolder.config(result);
        } catch (IOException | JSONException | ConfigurationException e) {
            return ConfigurationHolder.error("Failed to parse configuration file '" + path + "': " + e.getMessage(), e);
        }
    }

    private static void extractArtifactConfigurations(JSONObject jsonObject, FileDownloaderConfiguration result)
        throws ConfigurationException {
        try {
            JSONArray artifacts = jsonObject.getJSONArray("mavenArtifacts");

            for (int i = 0; i < artifacts.length(); i++) {
                try {
                    result.add(FileArtifactConfiguration.create(artifacts.getJSONObject(i)));
                } catch (JSONException | ConfigurationException e) {
                    LOGGER.error("Could not read artifact configuration number {}, ", i, e);
                }
            }
        } catch (JSONException e) {
            LOGGER.error("Could not extract artifacts configuration from config. {}", e.getMessage());
            throw ConfigurationException.missingParameter("mavenArtifacts", "config", e);
        }
    }

    private static void extractConfigParameters(JSONObject jsonObject, FileDownloaderConfiguration result)
        throws ConfigurationException {
        JSONObject config = JsonHelper
            .getJsonObject(jsonObject, "baseConfig")
            .orElseThrow(() -> ConfigurationException.missingParameter("baseConfig", "Configuration file"));
        result.setUsername(getConfigParameter(config, "nexusUsername"));
        result.setPassword(getConfigParameter(config, "nexusPassword"));
        result.setBaseUrl(getConfigParameter(config, "nexusUrl"));
        result.setCreateRelativeSymlinks(getBooleanConfigParameter("baseConfig", config, "createRelativeSymlinks"));

        result.setVerbose(getBooleanConfigParameter("baseConfig", config, "verbose"));
        result.setMoreVerbose(getBooleanConfigParameter("baseConfig", config, "moreVerbose"));

        JsonHelper.getOptionalString(config, "basePath").ifPresent(value -> result.setBasePath(Paths.get(value)));

        Optional<JSONObject> oldEntries = JsonHelper.getJsonObject(config,"oldEntries");
        if (oldEntries.isPresent()) {
            result.setCountToKeep(getIntegerParameter("oldEntries", oldEntries.get(), "countToKeep"));
            result.setDeleteOld(getBooleanConfigParameter("oldEntries", oldEntries.get(), "delete"));
        }
    }

    private static int getIntegerParameter(String context, JSONObject object, String parameter) throws ConfigurationException {
        try{
            return object.getInt(parameter);
        }catch (JSONException e){
            throw ConfigurationException.missingParameter(parameter, context, e);
        }
    }

    private static String getConfigParameter(JSONObject config, String parameter) throws ConfigurationException {
        try {
            return config.getString(parameter);
        } catch (JSONException e) {
            throw ConfigurationException.missingParameter(parameter, "baseConfig", e);
        }
    }

    private static boolean getBooleanConfigParameter(String context, JSONObject config, String parameter) throws ConfigurationException {
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
}
