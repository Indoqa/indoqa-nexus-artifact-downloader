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

import java.nio.file.Path;
import java.nio.file.Paths;

public interface DownloaderConfiguration {

    RepositoryStrategy getDefaultRepositoryStrategy();

    boolean verbose();

    boolean moreVerbose();

    boolean mostVerbose();

    default String getUsername() {
        return "";
    }

    default String getPassword() {
        return "";
    }

    default String getNexusBaseUrl() {
        return "";
    }

    default String getMavenCentralBaseUrl() {
        return "https://repo1.maven.org/maven2/";
    }

    default String getGithubPackagesBaseUrl() {
        return "https://maven.pkg.github.com/";
    }

    String getGithubOwner();

    String getGithubRepo();

    String getGithubToken();

    default String getNexusPathRestSearch() {
        return "service/rest/beta/search";
    }

    boolean createRelativeSymlinks();

    boolean deleteOldEntries();

    int getKeepNumberOfOldEntries();

    Iterable<ArtifactConfiguration> getArtifactConfigurations();

    default Path getWorkingPath() {
        return Paths.get(".");
    }
}
