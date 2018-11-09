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
package com.indoqa.nexus.artifact.downloader.helpers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.indoqa.nexus.artifact.downloader.configuration.ArtifactConfiguration;
import com.indoqa.nexus.artifact.downloader.configuration.DownloaderConfiguration;
import com.indoqa.nexus.artifact.downloader.configuration.RepositoryStrategy;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenCentralDownloader extends AbstractDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenCentralDownloader.class);

    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";
    private static final String SHA1_EXTENSION = ".sha1";

    private final String mavenCentralBase;

    public MavenCentralDownloader(DownloaderConfiguration downloaderConfiguration) {
        super(Executor.newInstance());
        this.mavenCentralBase = downloaderConfiguration.getMavenCentralBaseUrl();
    }

    @Override
    public boolean handles(RepositoryStrategy strategy) {
        return RepositoryStrategy.MAVEN_CENTRAL.equals(strategy);
    }

    @Override
    public List<DownloadableArtifact> getDownloadableArtifacts(ArtifactConfiguration artifactConfiguration)
        throws DownloaderException {
        Optional<String> version = artifactConfiguration.getArtifactVersion();
        if (!version.isPresent()) {
            Optional<String> latestVersion = this.getLatestVersion(artifactConfiguration);
            if (!latestVersion.isPresent()) {
                throw DownloaderException.errorCouldNotFindLatestVersion(
                    artifactConfiguration.getMavenGroupId(),
                    artifactConfiguration.getMavenArtifactId(),
                    artifactConfiguration.getMavenType());
            }
            version = latestVersion;
        }

        String groupId = artifactConfiguration.getMavenGroupId();
        String artifactId = artifactConfiguration.getMavenArtifactId();
        ArtifactType artifactType = ArtifactType.extractFromClassifierExtension(artifactConfiguration.getMavenType());
        String assetBase = this.createAssetBaseName(artifactId, version, artifactType);
        String assetBaseUrl = this.createAssertUrl(groupId, artifactId, version, assetBase);
        String sha1sum = this.getAssetSha1sum(assetBaseUrl);

        return Collections.singletonList(DownloadableArtifact.of(version.get(), assetBaseUrl, sha1sum));
    }

    private Optional<String> getLatestVersion(ArtifactConfiguration artifactConfiguration) throws DownloaderException {
        Request get = Request.Get(createMavenMetadataUrl(artifactConfiguration));
        try {
            Response response = this.executeRequest(get);
            String mavenMetadata = response.returnContent().asString();
            String version = StringUtils.substringBetween(mavenMetadata, "<latest>", "</latest>");
            LOGGER.trace("Will use the following version '{}' as found in <latest> tag.", version);

            return Optional.ofNullable(version);
        } catch (IOException e) {
            throw DownloaderException.errorExecutingRequest(get, e);
        }
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
            .append(this.mavenCentralBase)
            .append(this.convertToMavenUrlPath(groupId))
            .append('/')
            .append(this.convertToMavenUrlPath(artifactId))
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

    private String convertToMavenUrlPath(String path) {
        return StringUtils.replaceChars(path, '.', '/');
    }
}
