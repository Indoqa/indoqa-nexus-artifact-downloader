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

import static org.apache.commons.lang3.StringUtils.substringAfterLast;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.indoqa.nexus.downloader.client.configuration.ArtifactConfiguration;
import com.indoqa.nexus.downloader.client.configuration.DownloaderConfiguration;
import com.indoqa.nexus.downloader.client.configuration.RepositoryStrategy;
import com.indoqa.nexus.downloader.client.json.JsonDownloadExtractor;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NexusDownloader extends AbstractDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(NexusDownloader.class);

    private static final String REPOSITORY = "repository";
    private static final String CONTINUATION_TOKEN = "continuationToken";
    private static final String MAVEN_GROUP_ID = "maven.groupId";
    private static final String MAVEN_ARTIFACT_ID = "maven.artifactId";

    private final DownloaderConfiguration configuration;
    private final String restSearchPath;

    public NexusDownloader(DownloaderConfiguration configuration) {
        super(configuration.getUsername(), configuration.getPassword(), configuration.getNexusBaseUrl());
        this.restSearchPath = configuration.getNexusPathRestSearch();
        this.configuration = configuration;
    }

    @Override
    public boolean canHandle(RepositoryStrategy strategy) {
        return RepositoryStrategy.NEXUS.equals(strategy);
    }

    @Override
    public List<DownloadableArtifact> getDownloadableArtifacts(ArtifactConfiguration artifactConfiguration)
        throws DownloaderException {
        List<DownloadableArtifact> result = new ArrayList<>();

        JSONObject jsonObject = this.getJsonObject(buildUri(Optional.empty(), artifactConfiguration));
        result.addAll(this.extractDownloadableArtifacts(jsonObject, artifactConfiguration));
        Optional<String> continuationToken = this.getContinuationToken(jsonObject);

        while (continuationToken.isPresent()) {
            jsonObject = this.getJsonObject(buildUri(continuationToken, artifactConfiguration));
            result.addAll(this.extractDownloadableArtifacts(jsonObject, artifactConfiguration));
            continuationToken = this.getContinuationToken(jsonObject);
        }

        return result;
    }

    private JSONObject getJsonObject(URI uri) throws DownloaderException {
        LOGGER.debug("Will use the following uri to search for artifacts '{}'", uri);
        Request get = Request.Get(uri);
        get.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        try {
            Content content = this.executeRequest(get).returnContent();
            if (!ContentType.APPLICATION_JSON.getMimeType().equals(content.getType().getMimeType())) {
                throw DownloaderException.wrongMimeType(ContentType.APPLICATION_JSON, content.getType());
            }
            return new JSONObject(content.asString());
        } catch (IOException e) {
            throw DownloaderException.errorExecutingRequest(get, e);
        }
    }

    private URI buildUri(Optional<String> continuationToken, ArtifactConfiguration artifactConfiguration) throws DownloaderException {
        try {
            URIBuilder uriBuilder = new URIBuilder(this.configuration.getNexusBaseUrl())
                .setPath(restSearchPath)
                .addParameter(MAVEN_GROUP_ID, artifactConfiguration.getMavenGroupId())
                .addParameter(MAVEN_ARTIFACT_ID, artifactConfiguration.getMavenArtifactId())
                .addParameter(REPOSITORY, artifactConfiguration.getRepository());

            continuationToken.ifPresent(token -> uriBuilder.addParameter(CONTINUATION_TOKEN, token));
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw DownloaderException.errorBuildingUri(e);
        }
    }

    private Optional<String> getContinuationToken(JSONObject jsonObject) {
        return Optional.ofNullable(JsonDownloadExtractor.getContinuationToken(jsonObject));
    }

    private List<DownloadableArtifact> extractDownloadableArtifacts(JSONObject jsonObject,
        ArtifactConfiguration artifactConfiguration) {
        List<DownloadableArtifact> downloadableArtifacts = new ArrayList();
        ArtifactType requestedArtifactType = ArtifactType.extractFromClassifierExtension(artifactConfiguration.getMavenType());
        List<JSONObject> items = JsonDownloadExtractor.getItems(jsonObject);
        for (JSONObject item : items) {
            String version = JsonDownloadExtractor.getVersion(item);
            List<JSONObject> assets = JsonDownloadExtractor.getAssets(item);
            for (JSONObject asset : assets) {
                String downloadUrl = JsonDownloadExtractor.getDownloadUrl(asset);
                ArtifactType artifactType = extractArtifactType(version, downloadUrl);
                if (filterArtifactType(artifactType)) {
                    continue;
                }
                if (!requestedArtifactType.includes(artifactType)) {
                    continue;
                }
                String sha1 = JsonDownloadExtractor.getSha1(asset);
                downloadableArtifacts.add(DownloadableArtifact.of(version, downloadUrl, sha1));
            }
        }
        return downloadableArtifacts;
    }

    private boolean filterArtifactType(ArtifactType artifactType) {
        if (StringUtils.containsAny(artifactType.getExtension(), "md5", "sha1")) {
            return true;
        }
        return StringUtils.containsAny(artifactType.getClassifier(), "javadoc", "sources");
    }

    private ArtifactType extractArtifactType(String version, String downloadUrl) {
        String classifierExtension = substringAfterLast(downloadUrl, version);
        return ArtifactType.extractFromClassifierExtension(classifierExtension);
    }
}
