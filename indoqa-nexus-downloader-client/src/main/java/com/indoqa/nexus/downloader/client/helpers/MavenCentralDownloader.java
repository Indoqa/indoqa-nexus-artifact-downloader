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
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenCentralDownloader extends AbstractMavenMetadataDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenCentralDownloader.class);

    private final String mavenCentralBase;

    public MavenCentralDownloader(DownloaderConfiguration downloaderConfiguration) {
        super();
        this.mavenCentralBase = downloaderConfiguration.getMavenCentralBaseUrl();
    }

    @Override
    public boolean canHandle(RepositoryStrategy strategy) {
        return RepositoryStrategy.MAVEN_CENTRAL.equals(strategy);
    }

    @Override
    public List<DownloadableArtifact> getDownloadableArtifacts(ArtifactConfiguration artifactConfiguration)
        throws DownloaderException {
        Optional<String> version = getBaseVersion(artifactConfiguration);

        String groupId = artifactConfiguration.getMavenGroupId();
        String artifactId = artifactConfiguration.getMavenArtifactId();
        ArtifactType artifactType = ArtifactType.extractFromClassifierExtension(artifactConfiguration.getMavenType());
        String assetBase = this.createAssetBaseName(artifactId, version, artifactType);
        String assetBaseUrl = this.createAssertUrl(groupId, artifactId, version, assetBase);
        String sha1sum = this.getAssetSha1sum(assetBaseUrl);

        return Collections.singletonList(DownloadableArtifact.of(version.get(), assetBaseUrl, sha1sum));
    }

    protected Optional<String> downloadLatestVersion(ArtifactConfiguration artifactConfiguration) throws DownloaderException {
        Request get = Request.Get(createMavenMetadataUrl(artifactConfiguration));
        try {
            Response response = this.executeRequest(get);
            String mavenMetadata = response.returnContent().asString();
            MavenMetadataHelper mavenMetadataHelper = new MavenMetadataHelper(mavenMetadata);
            Optional<String> version = mavenMetadataHelper.getLatest();
            LOGGER.debug("Will use the following version '{}' as found in <latest> tag.", version.orElse("NOT FOUND"));
            return version;
        } catch (IOException e) {
            throw DownloaderException.errorExecutingRequest(get, e);
        }
    }

    protected String createAssertUrl(String groupId, String artifactId, Optional<String> version, String asset) {
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

    private String convertToMavenUrlPath(String path) {
        return StringUtils.replaceChars(path, '.', '/');
    }
}
