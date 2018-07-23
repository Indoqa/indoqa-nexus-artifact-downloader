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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.indoqa.nexus.artifact.downloader.configuration.ArtifactConfiguration;
import com.indoqa.nexus.artifact.downloader.configuration.DownloaderConfiguration;
import com.indoqa.nexus.artifact.downloader.json.JsonDownloadExtractor;
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

    private final DownloaderConfiguration configuration;

    private final Executor executor;

    public NexusArtifactDownloader(DownloaderConfiguration configuration) {
        this.configuration = configuration;
        this.executor = Executor
            .newInstance()
            .auth(configuration.getUsername(), configuration.getPassword())
            .authPreemptive(configuration.getNexusBaseUrl());
    }

    public DownloadResult download(ArtifactConfiguration artifactConfiguration) throws DownloaderException {
        List<DownloadableArtifact> downloadableArtifacts = getDownloadableArtifacts(artifactConfiguration);
        Optional<DownloadableArtifact> first = downloadableArtifacts.stream().max(DownloadableArtifact::compareTo);

        if (!first.isPresent()) {
            throw DownloaderException.notFound(
                artifactConfiguration.getMavenGroupId(),
                artifactConfiguration.getMavenArtifactId(),
                artifactConfiguration.getMavenType());
        }
        DownloadableArtifact downloadableArtifact = first.get();

        Request get = Request.Get(downloadableArtifact.getDownloadUrl());

        Path workingDirectory = this.getWorkingDirectory(artifactConfiguration);
        Path artifactPath = this.createArtifactPath(workingDirectory, downloadableArtifact, artifactConfiguration);
        if (!Files.exists(artifactPath)) {
            try {
                executeRequest(get).saveContent(artifactPath.toFile());
            } catch (IOException e) {
                throw DownloaderException.errorStoringArtifact(artifactPath, e);
            }
        } else {
            LOGGER.debug("Artifact already exists {}", artifactPath);
        }

        String calc = this.createSha1(artifactPath);
        if (!calc.equals(downloadableArtifact.getSha1())) {
            throw DownloaderException.mismatchSha1(downloadableArtifact.getSha1(), calc);
        }

        if (this.configuration.deleteOldEntries()) {
            this.deleteEntries(artifactPath.getParent(), artifactConfiguration);
        }

        Path link = createLink(workingDirectory, artifactPath, artifactConfiguration);
        return () -> "Symlink created " + link + " target: " + artifactPath;
    }

    private void deleteEntries(Path artifactParentPath, ArtifactConfiguration artifactConfiguration) {
        String mavenType = artifactConfiguration.getMavenType();
        try {
            long currentTimeInMillis = System.currentTimeMillis();
            List<Path> collect = Files
                .list(artifactParentPath)
                .filter(path -> path.getFileName().toString().endsWith(mavenType))
                .sorted(new Comparator<Path>() {

                    @Override
                    public int compare(Path o1, Path o2) {
                        int compare = this.getLastModified(o2).compareTo(this.getLastModified(o1));
                        if (compare == 0) {
                            return o2.getFileName().compareTo(o1.getFileName());
                        }
                        return compare;
                    }

                    private FileTime getLastModified(Path path) {
                        try {
                            return Files.getLastModifiedTime(path);
                        } catch (IOException e) {
                            return FileTime.fromMillis(currentTimeInMillis);
                        }
                    }
                })
                .skip(this.configuration.getKeepNumberOfOldEntries())
                .collect(Collectors.toList());

            for (Path path : collect) {
                Files.deleteIfExists(path);
            }
        } catch (Exception e) {
            LOGGER.error("Could not determine artifact count in {} for type {}. {}", artifactParentPath, mavenType, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Stacktrace: ", e.getCause());
            }
        }
    }

    private Path createLink(Path workingDirectory, Path artifactPath, ArtifactConfiguration artifactConfiguration)
        throws DownloaderException {
        String name = createName(artifactConfiguration.getMavenArtifactId(), artifactConfiguration.getMavenType());
        Path link = workingDirectory.resolve(name).toAbsolutePath();
        try {
            if (Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
                Files.delete(link);
            }
            if (this.configuration.createRelativeSymlinks()) {
                if (artifactConfiguration.getName().isPresent()) {
                    int index = this.findPathIndex(artifactPath, artifactConfiguration.getName().get());
                    Files.createSymbolicLink(link, Paths.get(".").resolve(artifactPath.subpath(index, artifactPath.getNameCount())));
                } else {
                    Files.createSymbolicLink(link, artifactPath);
                }
            } else {
                Files.createSymbolicLink(link, artifactPath.toAbsolutePath());
            }
            return link;
        } catch (IOException e) {
            throw DownloaderException.errorCreatingLink(link, artifactPath, e);
        }
    }

    private int findPathIndex(Path path, String name) {
        for (int i = 0; i < path.getNameCount(); i++) {
            if (path.getName(i).toString().equals(name)) {
                return i + 1;
            }
        }
        return 0;
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

    private Path createArtifactPath(Path working, DownloadableArtifact downloadableArtifact,
        ArtifactConfiguration artifactConfiguration) throws DownloaderException {
        Path repositoryPath;
        if (this.configuration.createRelativeSymlinks()) {
            Path basePath = this.configuration.getWorkingPath();
            if (artifactConfiguration.getName().isPresent()) {
                basePath = basePath.resolve(artifactConfiguration.getName().get());
            }
            repositoryPath = basePath.resolve(ARCHIVE_PATH).resolve(artifactConfiguration.getRepository());
        } else {
            repositoryPath = working.resolve(ARCHIVE_PATH).resolve(artifactConfiguration.getRepository());
        }
        try {
            Files.createDirectories(repositoryPath);
            return repositoryPath.resolve(downloadableArtifact.getArtifactName());
        } catch (IOException e) {
            throw DownloaderException.errorCreatingArtifactPath(repositoryPath, e);
        }
    }

    private Path getWorkingDirectory(ArtifactConfiguration artifactConfiguration) {
        Path basePath = this.configuration.getWorkingPath();

        if (artifactConfiguration.getName().isPresent()) {
            basePath = basePath.resolve(artifactConfiguration.getName().get());
        }
        return basePath.toAbsolutePath().normalize();
    }

    private List<DownloadableArtifact> getDownloadableArtifacts(ArtifactConfiguration artifactConfiguration)
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
        try {
            Content content = executeRequest(get).returnContent();
            if (!ContentType.APPLICATION_JSON.getMimeType().equals(content.getType().getMimeType())) {
                throw DownloaderException.wrongMimeType(ContentType.APPLICATION_JSON, content.getType());
            }
            return new JSONObject(content.asString());
        } catch (IOException e) {
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

    private URI buildUri(Optional<String> continuationToken, ArtifactConfiguration artifactConfiguration) throws DownloaderException {
        try {
            URIBuilder uriBuilder = new URIBuilder(this.configuration.getNexusBaseUrl())
                .setPath(PATH_REST_SEARCH)
                .addParameter(MAVEN_GROUP_ID, artifactConfiguration.getMavenGroupId())
                .addParameter(MAVEN_ARTIFACT_ID, artifactConfiguration.getMavenArtifactId())
                .addParameter(REPOSITORY, artifactConfiguration.getRepository());

            continuationToken.ifPresent(token -> uriBuilder.addParameter(CONTINUATION_TOKEN, token));
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw DownloaderException.errorBuildingUri(e);
        }
    }
}
