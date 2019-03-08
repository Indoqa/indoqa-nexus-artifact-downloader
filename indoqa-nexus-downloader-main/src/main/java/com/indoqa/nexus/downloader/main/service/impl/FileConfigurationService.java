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
package com.indoqa.nexus.downloader.main.service.impl;

import static com.indoqa.nexus.downloader.main.utils.FileHelper.getChildren;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.indoqa.nexus.downloader.main.ConfigurationIdentifier;
import com.indoqa.nexus.downloader.main.Elements;
import com.indoqa.nexus.downloader.main.resources.NotFoundException;
import com.indoqa.nexus.downloader.main.service.ConfigurationService;
import com.indoqa.nexus.downloader.main.service.ServiceException;

public class FileConfigurationService implements ConfigurationService {

    private Path configurationsPath;

    private static String normalize(String pathElement) {
        return pathElement.replaceAll("[:/]", "+");
    }

    @Override
    public void deleteConfiguration(ConfigurationIdentifier identifier) {
        Path path = this.getConfigurationPath(identifier);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new ServiceException("Could not delete configuration.", e);
        }
    }

    @Override
    public Optional<byte[]> getConfiguration(ConfigurationIdentifier identifier) {
        return Optional.of(this.getConfigurationPath(identifier)).filter(Files::exists).map(this::readConfiguration);
    }

    @Override
    public Elements getHosts(String repo, int start, int count) {
        Path path = this.getPath(repo);
        if (Files.notExists(path)) {
            throw NotFoundException.resourceNotFound();
        }

        return getChildren(path, Files::isDirectory, start, count);
    }

    @Override
    public Elements getRepos(int start, int count) {
        Path path = this.configurationsPath;
        return getChildren(path, Files::isDirectory, start, count);
    }

    @Override
    public Elements getVariants(String repo, String host, int start, int count) {
        Path path = this.getPath(repo, host);
        if (Files.notExists(path)) {
            throw NotFoundException.resourceNotFound();
        }

        return getChildren(path, Files::isRegularFile, start, count);
    }

    @Override
    public void saveConfiguration(ConfigurationIdentifier identifier, byte[] data) {
        Path path = this.getConfigurationPath(identifier);

        this.writeConfiguration(path, data);
    }

    public void setConfigurationsPath(Path configurationsPath) {
        this.configurationsPath = configurationsPath;
    }

    public void writeConfiguration(Path path, byte[] data) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, data);
        } catch (IOException e) {
            throw new ServiceException("Could not write configuration", e);
        }
    }

    private Path getConfigurationPath(ConfigurationIdentifier identifier) {
        return this.getPath(identifier.getRepo(), identifier.getHost()).resolve(resolveJson(normalize(identifier.getVariant())));
    }

    private String resolveJson(String variant) {
        if (!variant.contains(".json")) {
            return variant + ".json";
        }
        return variant;
    }

    private Path getPath(String project) {
        return this.configurationsPath.resolve(normalize(project));
    }

    private Path getPath(String project, String host) {
        return this.getPath(project).resolve(normalize(host));
    }

    private byte[] readConfiguration(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ServiceException("Could not read configuration.", e);
        }
    }
}
