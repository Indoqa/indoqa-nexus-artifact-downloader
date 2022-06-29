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
import java.util.*;

import com.indoqa.nexus.downloader.client.configuration.ArtifactConfiguration;
import com.indoqa.nexus.downloader.client.configuration.DownloaderConfiguration;
import com.indoqa.nexus.downloader.client.configuration.RepositoryStrategy;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GithubPackagesDownloader extends AbstractMavenMetadataDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(GithubPackagesDownloader.class);
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
        Optional<String> baseVersion = getBaseVersion(artifactConfiguration);

        String groupId = artifactConfiguration.getMavenGroupId();
        String artifactId = artifactConfiguration.getMavenArtifactId();

        Set<String> versions = getLatestArtifactVersions(groupId, artifactId, baseVersion);

        List<DownloadableArtifact> result = new ArrayList<>(versions.size());
        for (String version : versions) {
            ArtifactType artifactType = ArtifactType.extractFromClassifierExtension(artifactConfiguration.getMavenType());
            String assetBase = this.createAssetBaseName(artifactId, Optional.of(version), artifactType);
            String assetBaseUrl = this.createAssertUrl(groupId, artifactId, baseVersion, assetBase);
            String sha1sum = this.getAssetSha1sum(assetBaseUrl);

            result.add(DownloadableArtifact.of(version, assetBaseUrl, sha1sum));
        }
        return result;
    }

    private Set<String> getLatestArtifactVersions(String groupId, String artifactId, Optional<String> version)
        throws DownloaderException {
        Request get = Request.Get(this.createAssertUrl(groupId, artifactId, version, MAVEN_METADATA_XML));
        try {
            Response response = this.executeRequest(get);
            String mavenMetadata = response.returnContent().asString();
            return new MavenMetadataHelper(mavenMetadata).getVersions();
        } catch (IOException e) {
            throw DownloaderException.errorExecutingRequest(get, e);
        }
    }

    protected Optional<String> downloadLatestVersion(ArtifactConfiguration artifactConfiguration) throws DownloaderException {
        Request get = Request.Get(createMavenMetadataUrl(artifactConfiguration));
        try {
            Response response = this.executeRequest(get);
            String mavenMetadata = response.returnContent().asString();

            MavenMetadataHelper mavenMetadataHelper = new MavenMetadataHelper(mavenMetadata);
            Optional<String> latest = mavenMetadataHelper.getLatest();

            if (latest.isPresent()) {
                LOGGER.debug("Will use the following version '{}' as found in <latest> tag.", latest.get());
                return latest;
            }
        } catch (IOException e) {
            throw DownloaderException.errorExecutingRequest(get, e);
        }
        return Optional.empty();
    }

    protected String createAssertUrl(String groupId, String artifactId, Optional<String> version, String asset) {
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
}
