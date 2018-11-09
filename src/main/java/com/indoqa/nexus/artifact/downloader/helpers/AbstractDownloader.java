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
import java.nio.file.Path;
import java.util.List;

import com.indoqa.nexus.artifact.downloader.configuration.ArtifactConfiguration;
import com.indoqa.nexus.artifact.downloader.configuration.RepositoryStrategy;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

public abstract class AbstractDownloader {

    private final Executor executor;

    protected AbstractDownloader(Executor executor) {
        this.executor = executor;
    }

    public abstract boolean handles(RepositoryStrategy strategy);

    protected Response executeRequest(Request get) throws DownloaderException {
        try {
            return executor.execute(get);
        } catch (IOException e) {
            throw DownloaderException.errorExecutingRequest(get, e);
        }
    }

    public void saveArtifactToPath(DownloadableArtifact artifact, Path path) throws DownloaderException {
        Request get = Request.Get(artifact.getDownloadUrl());
        try {
            this.executeRequest(get).saveContent(path.toFile());
        } catch (IOException e) {
            throw DownloaderException.errorStoringArtifact(path, e);
        }
    }

    public abstract List<DownloadableArtifact> getDownloadableArtifacts(ArtifactConfiguration artifactConfiguration)
        throws DownloaderException;
}
