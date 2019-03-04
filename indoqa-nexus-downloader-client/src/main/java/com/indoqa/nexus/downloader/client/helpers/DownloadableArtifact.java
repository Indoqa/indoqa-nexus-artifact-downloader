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

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class DownloadableArtifact implements Comparable<DownloadableArtifact> {

    private ArtifactVersion version;
    private String artifactName;
    private String downloadUrl;
    private String sha1;

    public static DownloadableArtifact of(String version, String downloadUrl, String sha1) {
        DownloadableArtifact result = new DownloadableArtifact();
        result.setVersion(new DefaultArtifactVersion(version));
        result.setDownloadUrl(downloadUrl);
        result.setArtifactName(extractArtifactName(downloadUrl));
        result.setSha1(sha1);
        return result;
    }

    private static String extractArtifactName(String downloadUrl) {
        if (downloadUrl == null){
            return null;
        }
        return downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
    }

    public ArtifactVersion getVersion() {
        return version;
    }

    public void setVersion(ArtifactVersion version) {
        this.version = version;
    }

    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getSha1() {
        return sha1;
    }

    @Override
    public int compareTo(DownloadableArtifact artifact) {
        return version.compareTo(artifact.getVersion());
    }
}
