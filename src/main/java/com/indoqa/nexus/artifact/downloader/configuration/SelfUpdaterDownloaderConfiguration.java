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

import java.util.Collections;
import java.util.Optional;

public class SelfUpdaterDownloaderConfiguration implements DownloaderConfiguration, ArtifactConfiguration {

    private static final String SELFUPDATE_COMMAND = "selfupdate";

    public static ConfigurationHolder createSelfUpdateConfig(String[] args) {
        if (args == null || args.length > 1 || args.length == 0) {
            return ConfigurationHolder.help(
                "Supply " + SELFUPDATE_COMMAND + " as first argument to update indoqa-nexus-downloader.jar");
        }

        String command = args[0].trim();
        if (!command.equalsIgnoreCase(SELFUPDATE_COMMAND)) {
            return ConfigurationHolder.error("No " + SELFUPDATE_COMMAND + " command supplied.", null);
        }

        return ConfigurationHolder.config(new SelfUpdaterDownloaderConfiguration());
    }

    @Override
    public String getMavenGroupId() {
        return "com.indoqa";
    }

    @Override
    public String getMavenArtifactId() {
        return "indoqa-nexus-artifact-downloader";
    }

    @Override
    public String getRepository() {
        return "";
    }

    @Override
    public String getMavenType() {
        return "runnable.jar";
    }

    @Override
    public Optional<String> getArtifactVersion() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getName() {
        return Optional.empty();
    }

    @Override
    public RepositoryStrategy getRepositoryStrategy() {
        return RepositoryStrategy.MAVEN_CENTRAL;
    }

    @Override
    public boolean verbose() {
        return false;
    }

    @Override
    public boolean moreVerbose() {
        return false;
    }

    @Override
    public String getUsername() {
        return "notused";
    }

    @Override
    public String getPassword() {
        return "notused";
    }

    @Override
    public String getNexusBaseUrl() {
        return "http://example.com/not/used";
    }

    @Override
    public boolean createRelativeSymlinks() {
        return false;
    }

    @Override
    public boolean deleteOldEntries() {
        return true;
    }

    @Override
    public int getKeepNumberOfOldEntries() {
        return 2;
    }

    @Override
    public Iterable<ArtifactConfiguration> getArtifactConfigurations() {
        return Collections.singleton(this);
    }
}
