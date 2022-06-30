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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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
    private String nexusBaseUrl;
    private boolean createRelativeSymlinks;
    private int countToKeep;
    private boolean deleteOld;
    private boolean verbose;
    private boolean moreVerbose;
    private boolean mostVerbose;
    private String nexusPathRestSearch;

    private String githubOwner;
    private String githubRepo;
    private String githubToken;
    private RepositoryStrategy defaultRepositoryStrategy;

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
            JSONObject jsonObject = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            FileDownloaderConfiguration result = new FileDownloaderConfiguration();
            extractConfigParameters(jsonObject, result);
            extractArtifactConfigurations(jsonObject, result);

            checkForMissingConfiguration(result);

            return ConfigurationHolder.config(result);
        } catch (JSONException | ConfigurationException e) {
            return ConfigurationHolder.error("Failed to parse configuration file: " + e.getMessage(), e);
        }
    }

    private static void checkForMissingConfiguration(FileDownloaderConfiguration result) throws ConfigurationException {
        for (ArtifactConfiguration artifactConfiguration : result.getArtifactConfigurations()) {
            if (RepositoryStrategy.NEXUS.equals(artifactConfiguration.getRepositoryStrategy())) {
                checkParameter(result::getUsername, "nexusUsername", artifactConfiguration);
                checkParameter(result::getPassword, "nexusPassword", artifactConfiguration);
                checkParameter(result::getNexusBaseUrl, "nexusUrl", artifactConfiguration);
            }
            if (RepositoryStrategy.GITHUB_PACKAGES.equals(artifactConfiguration.getRepositoryStrategy())) {
                checkParameter(result::getGithubOwner, "githubOwner", artifactConfiguration);
                checkParameter(result::getGithubToken, "githubRepo", artifactConfiguration);
                checkParameter(result::getGithubRepo, "githubToken", artifactConfiguration);
            }
        }
    }

    private static void checkParameter(Supplier supplier, String parameter, ArtifactConfiguration configuration) throws ConfigurationException {
        if (supplier.get() == null) {
            throw ConfigurationException.missingParameter(parameter, BASE_CONFIG,
                "For artifact: " + configuration.getMavenArtifactId() + " strategy: " + configuration.getRepositoryStrategy());
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
                result.add(FileArtifactConfiguration.create(artifacts.getJSONObject(i), result.getDefaultRepositoryStrategy()));
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

        JsonHelper.getOptionalString(config, "nexusUsername").ifPresent(result::setUsername);
        JsonHelper.getOptionalString(config, "nexusPassword").ifPresent(result::setPassword);
        JsonHelper.getOptionalString(config, "nexusUrl").ifPresent(result::setNexusBaseUrl);

        if (result.getUsername() != null || result.getPassword() != null || result.getNexusBaseUrl() != null) {
            getConfigParameter(config, "nexusUsername");
            getConfigParameter(config, "nexusPassword");
            getConfigParameter(config, "nexusUrl");
        }

        JsonHelper.getOptionalString(config, "githubOwner").ifPresent(result::setGithubOwner);
        JsonHelper.getOptionalString(config, "githubToken").ifPresent(result::setGithubToken);
        JsonHelper.getOptionalString(config, "githubRepo").ifPresent(result::setGithubRepo);

        if (result.getGithubOwner() != null || result.getGithubRepo() != null || result.getGithubToken() != null) {
            getConfigParameter(config, "githubOwner");
            getConfigParameter(config, "githubToken");
            getConfigParameter(config, "githubRepo");
        }

        result.setCreateRelativeSymlinks(getBooleanConfigParameter(BASE_CONFIG, config, "createRelativeSymlinks"));

        result.setVerbose(getBooleanConfigParameter(BASE_CONFIG, config, "verbose"));
        result.setMoreVerbose(JsonHelper.getOptionalBoolean(config, "moreVerbose"));
        result.setMostVerbose(JsonHelper.getOptionalBoolean(config, "mostVerbose"));

        JsonHelper.getOptionalString(config, "basePath").ifPresent(value -> result.setBasePath(Paths.get(value)));
        JsonHelper.getOptionalString(config, "nexusPathRestSearch").ifPresent(result::setNexusPathRestSearch);

        Optional<JSONObject> oldEntries = JsonHelper.getJsonObject(config, OLD_ENTRIES);
        if (oldEntries.isPresent()) {
            result.setCountToKeep(getIntegerParameter(OLD_ENTRIES, oldEntries.get(), "countToKeep"));
            result.setDeleteOld(getBooleanConfigParameter(OLD_ENTRIES, oldEntries.get(), "delete"));
        }

        String defaultStrategy = JsonHelper.getString(config, "defaultRepoStrategy", "NEXUS");
        try {
            result.setDefaultRepositoryStrategy(Enum.valueOf(RepositoryStrategy.class, defaultStrategy));
        } catch (Exception e) {
            throw ConfigurationException.invalidValue("defaultRepoStrategy", BASE_CONFIG,
                defaultStrategy, Arrays.toString(RepositoryStrategy.values()), e);
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

    public void setDefaultRepositoryStrategy(RepositoryStrategy defaultRepositoryStrategy) {
        this.defaultRepositoryStrategy = defaultRepositoryStrategy;
    }

    private void add(FileArtifactConfiguration fileArtifactConfiguration) {
        this.artifactConfigurations.add(fileArtifactConfiguration);
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public RepositoryStrategy getDefaultRepositoryStrategy() {
        return this.defaultRepositoryStrategy;
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

    public void setMostVerbose(boolean mostVerbose) {
        this.mostVerbose = mostVerbose;
    }

    @Override
    public boolean mostVerbose() {
        return this.mostVerbose;
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

    public void setNexusBaseUrl(String nexusBaseUrl) {
        this.nexusBaseUrl = nexusBaseUrl;
    }

    @Override
    public String getNexusBaseUrl() {
        return this.nexusBaseUrl;
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

    @Override
    public String getGithubOwner() {
        return githubOwner;
    }

    public void setGithubOwner(String githubOwner) {
        this.githubOwner = githubOwner;
    }

    @Override
    public String getGithubRepo() {
        return githubRepo;
    }
    public void setGithubRepo(String githubRepo) {
        this.githubRepo = githubRepo;
    }

    @Override
    public String getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }
}
