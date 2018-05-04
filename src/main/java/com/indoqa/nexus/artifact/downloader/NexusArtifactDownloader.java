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
package com.indoqa.nexus.artifact.downloader;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.indoqa.nexus.artifact.downloader.result.DownloadResult;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NexusArtifactDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(NexusArtifactDownloader.class);

    private static final String REPOSITORY = "repository";
    private static final String CONTINUATION_TOKEN = "continuationToken";
    private static final String MAVEN_GROUP_ID = "maven.groupId";
    private static final String MAVEN_ARTIFACT_ID = "maven.artifactId";

    private static final String PATH_REST_SEARCH = "service/rest/beta/search";
    public static final String ARCHIVE_PATH = "archive";

    private final String nexusBaseUrl;
    private final Executor executor;
    private final String repository;

    private boolean createRelativeSymlinks = false;

    public NexusArtifactDownloader(String nexusBaseUrl, String username, String password, String repository) {
        this.nexusBaseUrl = nexusBaseUrl;
        this.executor = Executor.newInstance().auth(username, password).authPreemptive(nexusBaseUrl);
        this.repository = repository;
    }

    public DownloadResult download(String mavenGroupId, String mavenArtifactId, String mavenType)
        throws DownloaderException {
        List<DownloadableArtifact> downloadableArtifacts = getDownloadableArtifacts(mavenGroupId, mavenArtifactId, mavenType);
        Optional<DownloadableArtifact> first = downloadableArtifacts.stream().max(DownloadableArtifact::compareTo);

        if (!first.isPresent()) {
            throw DownloaderException.notFound(mavenGroupId, mavenArtifactId, mavenType);
        }
        DownloadableArtifact downloadableArtifact = first.get();

        Request get = Request.Get(downloadableArtifact.getDownloadUrl());

        Path workingDirectory = getWorkingDirectory();
        Path artifactPath = createArtifactPath(workingDirectory, downloadableArtifact);
        if (!Files.exists(artifactPath)) {
            try {
                executeRequest(get).saveContent(artifactPath.toFile());
            } catch (IOException e) {
                throw DownloaderException.errorStoringArtifact(artifactPath, e);
            }
        } else {
            LOGGER.debug("Artifact already exists {}", artifactPath);
        }

        String calc = createSha1(artifactPath);
        if (!calc.equals(downloadableArtifact.getSha1())) {
            throw DownloaderException.mismatchSha1(downloadableArtifact.getSha1(), calc);
        }

        Path link = createLink(mavenArtifactId, mavenType, workingDirectory, artifactPath);
        return () -> "Symlink created " + link + " target: " + artifactPath;
    }

    public void createRelativeSymLinks() {
        this.createRelativeSymlinks = true;
    }

    private Path createLink(String mavenArtifactId, String mavenType, Path workingDirectory, Path artifactPath) throws DownloaderException {
        String name = createName(mavenArtifactId, mavenType);
        Path link = workingDirectory.resolve(name).toAbsolutePath();
        try{
            if (Files.exists(link)) {
                Files.delete(link);
            }
            if (this.createRelativeSymlinks) {
                Files.createSymbolicLink(link, artifactPath);
            } else {
                Files.createSymbolicLink(link, artifactPath.toAbsolutePath());
            }
            return link;
        }catch (IOException e){
            throw DownloaderException.errorCreatingLink(link, artifactPath, e);
        }
    }

    private String createName(String mavenArtifactId, String mavenType) {
        if (mavenType.contains(".")) {
            return mavenArtifactId + "-" + mavenType;
        }
        return mavenArtifactId + "." + mavenType;
    }

    private String createSha1(Path artifactPath) throws DownloaderException {
        try {
            return DigestUtils.sha1Hex(new FileInputStream(artifactPath.toFile()));
        } catch (IOException e) {
            throw DownloaderException.errorCalculatingSha1(artifactPath, e);
        }
    }

    private Path createArtifactPath(Path working, DownloadableArtifact downloadableArtifact) throws DownloaderException {
        Path repositoryPath;
        if (this.createRelativeSymlinks) {
            repositoryPath = Paths.get(".").resolve(ARCHIVE_PATH).resolve(this.repository);
        } else {
            repositoryPath = working.resolve(ARCHIVE_PATH).resolve(this.repository);
        }
        try{
            Files.createDirectories(repositoryPath);
            return repositoryPath.resolve(downloadableArtifact.getArtifactName());
        }catch (IOException e) {
            throw DownloaderException.errorCreatingArtifactPath(repositoryPath, e);
        }
    }

    private Path getWorkingDirectory() {
        return Paths.get(".").toAbsolutePath().normalize();
    }

    private List<DownloadableArtifact> getDownloadableArtifacts(String mavenGroupId, String mavenArtifactId, String mavenType)
        throws DownloaderException {
        List<DownloadableArtifact> result = new ArrayList<>();

        JSONObject jsonObject = getJsonObject(buildUri(mavenGroupId, mavenArtifactId, Optional.empty()));
        result.addAll(extractDownloadableArtifacts(jsonObject, mavenType));
        Optional<String> continuationToken = getContinuationToken(jsonObject);

        while (continuationToken.isPresent()) {
            jsonObject = getJsonObject(buildUri(mavenGroupId, mavenArtifactId, continuationToken));
            result.addAll(extractDownloadableArtifacts(jsonObject, mavenType));
            continuationToken = getContinuationToken(jsonObject);
        }

        return result;
    }

    private Optional<String> getContinuationToken(JSONObject jsonObject) {
        return Optional.ofNullable(JsonExtractor.getContinuationToken(jsonObject));
    }

    private List<DownloadableArtifact> extractDownloadableArtifacts(JSONObject jsonObject, String mavenType) {
        List<DownloadableArtifact> downloadableArtifacts = new ArrayList();
        ArtifactType requestedArtifactType = ArtifactType.extractFromClassifierExtension(mavenType);
        List<JSONObject> items = JsonExtractor.getItems(jsonObject);
        for (JSONObject item : items) {
            String version = JsonExtractor.getVersion(item);
            List<JSONObject> assets = JsonExtractor.getAssets(item);
            for (JSONObject asset : assets) {
                String downloadUrl = JsonExtractor.getDownloadUrl(asset);
                ArtifactType artifactType = extractArtifactType(version, downloadUrl);
                if (filterArtifactType(artifactType)) {
                    continue;
                }
                if (!requestedArtifactType.includes(artifactType)) {
                    continue;
                }
                String sha1 = JsonExtractor.getSha1(asset);
                downloadableArtifacts.add(DownloadableArtifact.of(version, downloadUrl, sha1));
            }
        }
        return downloadableArtifacts;
    }

    private boolean filterArtifactType(ArtifactType artifactType) {
        if (StringUtils.containsAny(artifactType.getExtension(), "md5", "sha1")) {
            return true;
        }
        if (StringUtils.containsAny(artifactType.getClassifier(), "javadoc", "sources")) {
            return true;
        }
        return false;
    }

    private ArtifactType extractArtifactType(String version, String downloadUrl) {
        String classifierExtension = substringAfterLast(downloadUrl, version);
        return ArtifactType.extractFromClassifierExtension(classifierExtension);
    }

    private JSONObject getJsonObject(URI uri) throws DownloaderException {
        LOGGER.trace("Will use the following uri to search for artifacts '{}'", uri);
        Request get = Request.Get(uri);
        get.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        try{
            Content content = executeRequest(get).returnContent();
            if (!ContentType.APPLICATION_JSON.getMimeType().equals(content.getType().getMimeType())) {
                throw DownloaderException.wrongMimeType(ContentType.APPLICATION_JSON, content.getType());
            }
            return new JSONObject(content.asString());
        }catch (IOException e) {
            throw DownloaderException.errorExecutingRequest(get, e);
        }
    }

    private Response executeRequest(Request get) throws DownloaderException {
        try {
            return executor.execute(get);
        } catch (IOException e) {
            throw DownloaderException.errorExecutingRequest(get, e);
        }
    }

    private URI buildUri(String mavenGroupId, String mavenArtifactId, Optional<String> continuationToken) throws DownloaderException {
        try{
            URIBuilder uriBuilder = new URIBuilder(this.nexusBaseUrl)
                .setPath(PATH_REST_SEARCH)
                .addParameter(MAVEN_GROUP_ID, mavenGroupId)
                .addParameter(MAVEN_ARTIFACT_ID, mavenArtifactId)
                .addParameter(REPOSITORY, this.repository);

            continuationToken.ifPresent(token -> uriBuilder.addParameter(CONTINUATION_TOKEN, token));
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw DownloaderException.errorBuildingUri(e);
        }
    }
}
