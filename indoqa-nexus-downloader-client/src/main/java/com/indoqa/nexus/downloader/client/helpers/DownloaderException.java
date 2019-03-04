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
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Locale;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

public final class DownloaderException extends Exception {

    private static Locale DEFAULT = Locale.GERMAN;

    private final Type type;

    private DownloaderException(Type type, String message) {
        super(message);
        this.type = type;
    }

    private DownloaderException(Type type, String message, Exception e) {
        super(message, e);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public static DownloaderException errorCouldNotFindLatestVersion(String mavenGroupId, String mavenArtifactId, String mavenType) {
        return new DownloaderException(Type.NOT_FOUND,
            String.format(DEFAULT, "No latest version of artifact found for %s %s %s", mavenGroupId, mavenArtifactId, mavenType));
    }

    public static DownloaderException notFound(String mavenGroupId, String mavenArtifactId, String mavenType) {
        return new DownloaderException(Type.NOT_FOUND,
            String.format(DEFAULT, "No artifact present for %s %s %s", mavenGroupId, mavenArtifactId, mavenType));
    }

    public static DownloaderException errorBuildingUri(URISyntaxException e) {
        return new DownloaderException(Type.INTERNAL_ERROR, "Could not create URI.", e);
    }

    public static DownloaderException errorCreatingArtifactPath(Path repositoryPath, IOException e) {
        return new DownloaderException(Type.FILESYSTEM_ERROR, "Could not create " + repositoryPath, e);
    }

    public static DownloaderException errorExecutingRequest(Request get, IOException e) {
        return new DownloaderException(Type.NETWORK_ERROR, "Error executing " + get, e);
    }

    public static DownloaderException wrongMimeType(ContentType requestedContentType, ContentType type) {
        return new DownloaderException(Type.NETWORK_ERROR,
            String.format(DEFAULT, "Requested content type %s does not match returned %s.", requestedContentType, type));

    }

    public static DownloaderException errorCalculatingSha1(Path path, IOException e) {
        return new DownloaderException(Type.FILESYSTEM_ERROR, "Error calculating sha1 for " + path, e);
    }

    public static DownloaderException errorStoringArtifact(Path artifactPath, IOException e) {
        return new DownloaderException(Type.FILESYSTEM_ERROR, "Could not store artifact at " + artifactPath, e);
    }

    public static DownloaderException mismatchSha1(String requestedSha1, String calculatedSha1) {
        return new DownloaderException(Type.INTERNAL_ERROR,
            String.format(DEFAULT, "Sha1 requested: \n\t%s does not match calculated: \n\t%s", requestedSha1, calculatedSha1));
    }

    public static DownloaderException errorCreatingLink(Path link, Path artifactPath, IOException e) {
        return new DownloaderException(Type.FILESYSTEM_ERROR, "Could not create link " + link + " for target " + artifactPath, e);
    }

    private enum Type {
        NOT_FOUND, INTERNAL_ERROR, FILESYSTEM_ERROR, NETWORK_ERROR;
    }
}
