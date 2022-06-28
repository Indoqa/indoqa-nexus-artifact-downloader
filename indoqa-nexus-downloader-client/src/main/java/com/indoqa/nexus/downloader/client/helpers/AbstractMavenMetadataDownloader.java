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
import java.util.Optional;

import com.indoqa.nexus.downloader.client.configuration.ArtifactConfiguration;
import org.apache.http.client.fluent.Request;

public abstract class AbstractMavenMetadataDownloader extends AbstractDownloader {

    protected static final String MAVEN_METADATA_XML = "maven-metadata.xml";
    protected static final String SHA1_EXTENSION = ".sha1";

    protected AbstractMavenMetadataDownloader() {
        super();
    }

    protected AbstractMavenMetadataDownloader(String user, String password, String url) {
        super(user, password, url);
    }

    protected Optional<String> getBaseVersion(ArtifactConfiguration artifactConfiguration) throws DownloaderException {
        Optional<String> baseVersion = artifactConfiguration.getArtifactVersion();
        if (baseVersion.isPresent()) {
            return baseVersion;
        }

        Optional<String> latestVersion = this.downloadLatestVersion(artifactConfiguration);
        if (!latestVersion.isPresent()) {
            throw DownloaderException.errorCouldNotFindLatestVersion(
                artifactConfiguration.getMavenGroupId(),
                artifactConfiguration.getMavenArtifactId(),
                artifactConfiguration.getMavenType());
        }
        return latestVersion;
    }

    protected String getAssetSha1sum(String assetBaseUrl) throws DownloaderException {
        Request get = Request.Get(assetBaseUrl + SHA1_EXTENSION);
        try {
            return this.executeRequest(get).returnContent().asString();
        } catch (IOException e) {
            throw DownloaderException.errorExecutingRequest(get, e);
        }
    }

    protected String createAssetBaseName(String artifactId, Optional<String> version, ArtifactType artifactType) {
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

    protected String createMavenMetadataUrl(ArtifactConfiguration configuration) {
        return this.createAssertUrl(configuration.getMavenGroupId(),
            configuration.getMavenArtifactId(),
            Optional.empty(),
            MAVEN_METADATA_XML);
    }

    protected abstract String createAssertUrl(String groupId, String artifactId, Optional<String> version, String asset);

    protected abstract Optional<String> downloadLatestVersion(ArtifactConfiguration artifactConfiguration) throws DownloaderException;
}
