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
package com.indoqa.nexus.downloader.client;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.indoqa.nexus.downloader.client.configuration.ArtifactConfiguration;
import com.indoqa.nexus.downloader.client.configuration.DownloaderConfiguration;
import com.indoqa.nexus.downloader.client.configuration.RepositoryStrategy;
import com.indoqa.nexus.downloader.client.helpers.*;
import com.indoqa.nexus.downloader.client.result.DownloadResult;

public class ArtifactHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactHandler.class);

    public static final String ARCHIVE_PATH = "archive";

    private final DownloaderConfiguration configuration;

    private List<AbstractDownloader> downloaders;

    public ArtifactHandler(DownloaderConfiguration configuration) {
        this.configuration = configuration;
        this.downloaders = new ArrayList<>(3);
        this.downloaders.add(new NexusDownloader(configuration));
        this.downloaders.add(new MavenCentralDownloader(configuration));
        this.downloaders.add(new GithubPackagesDownloader(configuration));
    }

    public DownloadResult download(ArtifactConfiguration artifactConfiguration) throws DownloaderException {
        List<DownloadableArtifact> downloadableArtifacts = this.getDownloadableArtifacts(artifactConfiguration);
        Optional<DownloadableArtifact> first = downloadableArtifacts.stream().max(DownloadableArtifact::compareTo);

        if (!first.isPresent()) {
            throw DownloaderException.notFound(
                artifactConfiguration.getMavenGroupId(),
                artifactConfiguration.getMavenArtifactId(),
                artifactConfiguration.getMavenType());
        }
        DownloadableArtifact downloadableArtifact = first.get();

        Path workingDirectory = this.getWorkingDirectory(artifactConfiguration);
        Path artifactPath = this.createArtifactPath(workingDirectory, downloadableArtifact, artifactConfiguration);
        if (!Files.exists(artifactPath)) {
            this.saveToFile(artifactConfiguration.getRepositoryStrategy(), downloadableArtifact, artifactPath);
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

        Path link = this.createLink(workingDirectory, artifactPath, artifactConfiguration);
        return () -> "Symlink created " + link + " target: " + artifactPath;
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

    private Path createLink(Path workingDirectory, Path artifactPath, ArtifactConfiguration artifactConfiguration)
            throws DownloaderException {
        String name = this.createName(artifactConfiguration.getMavenArtifactId(), artifactConfiguration.getMavenType());
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

    private String createName(String mavenArtifactId, String mavenType) {
        if (mavenType.contains(".")) {
            return mavenArtifactId + "-" + mavenType;
        }
        return mavenArtifactId + "." + mavenType;
    }

    private String createSha1(Path artifactPath) throws DownloaderException {
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(artifactPath.toFile().toPath()))) {
            return DigestUtils.sha1Hex(inputStream);
        } catch (IOException e) {
            throw DownloaderException.errorCalculatingSha1(artifactPath, e);
        }
    }

    private void deleteEntries(Path artifactParentPath, ArtifactConfiguration artifactConfiguration) {
        String mavenType = artifactConfiguration.getMavenType();
        try {
            long currentTimeInMillis = System.currentTimeMillis();
            try (Stream<Path> files = Files.list(artifactParentPath)) {
                List<Path> collect = files
                    .filter(path -> path.getFileName().toString().endsWith(mavenType))
                    .sorted(new PathComparator(currentTimeInMillis))
                    .skip(this.configuration.getKeepNumberOfOldEntries())
                    .collect(Collectors.toList());

                for (Path path : collect) {
                    Files.deleteIfExists(path);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not determine artifact count in {} for type {}. {}", artifactParentPath, mavenType, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Stacktrace: ", e.getCause());
            }
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

    private List<DownloadableArtifact> getDownloadableArtifacts(ArtifactConfiguration artifact)
            throws DownloaderException {
        LOGGER.debug(
            "Will download {}:{}:{} from {}",
            artifact.getMavenGroupId(),
            artifact.getMavenArtifactId(),
            artifact.getArtifactVersion().orElse("LATEST"),
            artifact.getRepository());
        return this.getDownloader(artifact.getRepositoryStrategy()).getDownloadableArtifacts(artifact);
    }

    private AbstractDownloader getDownloader(RepositoryStrategy strategy) throws DownloaderException {
        Optional<AbstractDownloader> first = this.downloaders.stream().filter(downloader -> downloader.handles(strategy)).findFirst();
        if (!first.isPresent()) {
            throw DownloaderException.errorMisconfiguration(strategy);
        }
        return first.get();
    }

    private Path getWorkingDirectory(ArtifactConfiguration artifactConfiguration) {
        Path basePath = this.configuration.getWorkingPath();

        if (artifactConfiguration.getName().isPresent()) {
            basePath = basePath.resolve(artifactConfiguration.getName().get());
        }
        return basePath.toAbsolutePath().normalize();
    }

    private void saveToFile(RepositoryStrategy strategy, DownloadableArtifact downloadableArtifact, Path artifactPath)
            throws DownloaderException {
        this.getDownloader(strategy).saveArtifactToPath(downloadableArtifact, artifactPath);
    }

    private static class PathComparator implements Comparator<Path> {

        private final long currentTimeInMillis;

        public PathComparator(long currentTimeInMillis) {
            this.currentTimeInMillis = currentTimeInMillis;
        }

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
                return FileTime.fromMillis(this.currentTimeInMillis);
            }
        }
    }
}
