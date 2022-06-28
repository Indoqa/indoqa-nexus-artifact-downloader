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
package com.indoqa.nexus.downloader.client.helpers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.indoqa.nexus.downloader.client.configuration.ArtifactConfiguration;
import com.indoqa.nexus.downloader.client.configuration.DownloaderConfiguration;
import com.indoqa.nexus.downloader.client.configuration.RepositoryStrategy;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.joox.JOOX;
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GithubPackagesDownloader extends AbstractDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(GithubPackagesDownloader.class);

    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";
    private static final String SHA1_EXTENSION = ".sha1";

    private final String githubPackagesBaseUrl;
    private final String githubOwner;
    private final String githubRepo;

    public GithubPackagesDownloader(DownloaderConfiguration configuration) {
        super(configuration.getGithubOwner(), configuration.getGithubToken(), configuration.getGithubPackagesBaseUrl());
        this.githubPackagesBaseUrl = configuration.getGithubPackagesBaseUrl();
        this.githubOwner = configuration.getGithubOwner();
        this.githubRepo = configuration.getGithubRepo();
    }

    @Override
    public boolean canHandle(RepositoryStrategy strategy) {
        return RepositoryStrategy.GITHUB_PACKAGES.equals(strategy);
    }

    @Override
    public List<DownloadableArtifact> getDownloadableArtifacts(ArtifactConfiguration artifactConfiguration)
        throws DownloaderException {
        Optional<VersionUpdate> versionUpdate = Optional.empty();
        Optional<String> baseVersion = artifactConfiguration.getArtifactVersion();
        if (!baseVersion.isPresent()) {
            Optional<VersionUpdate> latestVersion = this.getLatestVersion(artifactConfiguration);
            if (!latestVersion.isPresent()) {
                throw DownloaderException.errorCouldNotFindLatestVersion(
                    artifactConfiguration.getMavenGroupId(),
                    artifactConfiguration.getMavenArtifactId(),
                    artifactConfiguration.getMavenType());
            }
            versionUpdate = latestVersion;
        }

        String groupId = artifactConfiguration.getMavenGroupId();
        String artifactId = artifactConfiguration.getMavenArtifactId();

        Optional<String> version = getLatestArtifactVersion(groupId, artifactId, versionUpdate);

        ArtifactType artifactType = ArtifactType.extractFromClassifierExtension(artifactConfiguration.getMavenType());
        String assetBase = this.createAssetBaseName(artifactId, version, artifactType);
        String assetBaseUrl = this.createAssertUrl(groupId, artifactId, versionUpdate.map(VersionUpdate::getVersion), assetBase);
        String sha1sum = this.getAssetSha1sum(assetBaseUrl);

        return Collections.singletonList(DownloadableArtifact.of(versionUpdate.map(VersionUpdate::getVersion).get(), assetBaseUrl, sha1sum));
    }

    private Optional<String> getLatestArtifactVersion(String groupId, String artifactId, Optional<VersionUpdate> versionUpdate)
        throws DownloaderException {
        Request get = Request.Get(this.createAssertUrl(groupId, artifactId, versionUpdate.map(VersionUpdate::getVersion), MAVEN_METADATA_XML));
        try {
            Response response = this.executeRequest(get);
            String mavenMetadata = response.returnContent().asString();
            Optional<String> updated = new MavenMetadataHelper(mavenMetadata).getUpdated(versionUpdate.map(VersionUpdate::getLastUpdated));
            if (updated.isPresent()) {
                String buildVersion = updated.get();
                LOGGER.trace("Will use the following version '{}' as found in <updated> tag.", buildVersion);
                return Optional.ofNullable(buildVersion);
            }
            return Optional.empty();
        } catch (IOException e) {
            throw DownloaderException.errorExecutingRequest(get, e);
        }
    }

    private Optional<VersionUpdate> getLatestVersion(ArtifactConfiguration artifactConfiguration) throws DownloaderException {
        Request get = Request.Get(createMavenMetadataUrl(artifactConfiguration));
        try {
            Response response = this.executeRequest(get);
            String mavenMetadata = response.returnContent().asString();

            MavenMetadataHelper mavenMetadataHelper = new MavenMetadataHelper(mavenMetadata);
            Optional<String> latest = mavenMetadataHelper.getLatest();
            Optional<String> updated = mavenMetadataHelper.getLastUpdated();

            String version = null;
            if (latest.isPresent()) {
                version = latest.get();
                LOGGER.trace("Will use the following version '{}' as found in <latest> tag.", version);
            }
            String lastUpdated = null;
            if (updated.isPresent()) {
                lastUpdated = updated.get();
                LOGGER.trace("Will use the following lastUpdated '{}' as found in <lastUpdated> tag.", lastUpdated);
            }
            if (version != null && lastUpdated != null) {
                return Optional.ofNullable(createVersionUpdate(version, lastUpdated));
            }
        } catch (IOException e) {
            throw DownloaderException.errorExecutingRequest(get, e);
        }
        return Optional.empty();
    }

    private String getAssetSha1sum(String assetBaseUrl) throws DownloaderException {
        Request get = Request.Get(assetBaseUrl + SHA1_EXTENSION);
        try {
            return this.executeRequest(get).returnContent().asString();
        } catch (IOException e) {
            throw DownloaderException.errorExecutingRequest(get, e);
        }
    }

    private String createAssetBaseName(String artifactId, Optional<String> version, ArtifactType artifactType) {
        StringBuilder result = new StringBuilder();
        result
            .append(artifactId)
            .append('-')
            .append(version.get());
        if (artifactType.getClassifier() != null) {
            result
                .append('-')
                .append(artifactType.getClassifier());
        }
        result
            .append('.')
            .append(artifactType.getExtension());
        return result.toString();
    }

    private String createAssertUrl(String groupId, String artifactId, Optional<String> version, String asset) {
        StringBuilder result = new StringBuilder();
        result
            .append(this.githubPackagesBaseUrl)
            .append(this.githubOwner)
            .append('/')
            .append(this.githubRepo)
            .append('/')
            .append(groupId)
            .append('/')
            .append(artifactId)
            .append('/');

        version.ifPresent(value -> result.append(value).append('/'));

        result.append(asset);
        return result.toString();
    }

    private String createMavenMetadataUrl(ArtifactConfiguration configuration) {
        return this.createAssertUrl(configuration.getMavenGroupId(),
            configuration.getMavenArtifactId(),
            Optional.empty(),
            MAVEN_METADATA_XML);
    }
    private VersionUpdate createVersionUpdate(String version, String lastUpdated) {
        if (version == null || lastUpdated == null) {
            return null;
        }
        return new VersionUpdate(version, lastUpdated);
    }

    private class VersionUpdate {
        private final String version;
        private final String lastUpdated;

        private VersionUpdate(String version, String lastUpdated) {
            this.version = version;
            this.lastUpdated = lastUpdated;
        }

        public String getVersion() {
            return version;
        }

        public String getLastUpdated() {
            return lastUpdated;
        }
    }
}
