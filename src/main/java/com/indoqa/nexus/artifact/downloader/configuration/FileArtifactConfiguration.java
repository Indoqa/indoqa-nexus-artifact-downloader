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
package com.indoqa.nexus.artifact.downloader.configuration;

import java.util.Optional;

import com.indoqa.nexus.artifact.downloader.json.JsonHelper;
import org.json.JSONException;
import org.json.JSONObject;

public class FileArtifactConfiguration implements ArtifactConfiguration {

    private String mavenType;
    private String repository;
    private String mavenGroupId;
    private String mavenArtifactId;
    private Optional<String> name;

    public static FileArtifactConfiguration create(JSONObject jsonObject) throws ConfigurationException {
        FileArtifactConfiguration result = new FileArtifactConfiguration();

        result.setMavenArtifactId(getConfigParameter(jsonObject, "artifactId"));
        result.setMavenGroupId(getConfigParameter(jsonObject, "groupId"));
        result.setRepository(JsonHelper.getString(jsonObject, "repo", "releases"));
        result.setMavenType(getConfigParameter(jsonObject, "type"));
        result.setName(JsonHelper.getOptionalString(jsonObject, "name"));

        return result;
    }

    private static String getConfigParameter(JSONObject config, String parameter) throws ConfigurationException {
        try {
            return config.getString(parameter);
        } catch (JSONException e) {
            throw ConfigurationException.missingParameter(parameter, "artifact configuration", e);
        }
    }

    public void setMavenGroupId(String mavenGroupId) {
        this.mavenGroupId = mavenGroupId;
    }

    @Override
    public String getMavenGroupId() {
        return this.mavenGroupId;
    }

    public void setMavenArtifactId(String mavenArtifactId) {
        this.mavenArtifactId = mavenArtifactId;
    }

    @Override
    public String getMavenArtifactId() {
        return this.mavenArtifactId;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    @Override
    public String getRepository() {
        return this.repository;
    }

    public void setMavenType(String mavenType) {
        this.mavenType = mavenType;
    }

    @Override
    public String getMavenType() {
        return this.mavenType;
    }

    @Override
    public Optional<String> getArtifactVersion() {
        return Optional.empty();
    }

    public void setName(Optional<String> name) {
        this.name = name;
    }

    @Override
    public Optional<String> getName() {
        return this.name;
    }
}
