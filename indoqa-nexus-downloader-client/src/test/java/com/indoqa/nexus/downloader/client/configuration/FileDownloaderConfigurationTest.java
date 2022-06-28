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
package com.indoqa.nexus.downloader.client.configuration;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class FileDownloaderConfigurationTest {

    @Test
    public void testCompleteFile() throws IOException {
        File file = File.createTempFile("downloader-test", "json");
        file.deleteOnExit();

        JSONObject oldEntries = new JSONObject();
        oldEntries.put("countToKeep", 2);
        oldEntries.put("delete", true);

        Map<String, Object> baseConfigParameter = new HashMap<>();
        baseConfigParameter.put("nexusUsername", "user");
        baseConfigParameter.put("nexusPassword", "pass");
        baseConfigParameter.put("nexusUrl", "url");
        baseConfigParameter.put("createRelativeSymlinks", false);
        baseConfigParameter.put("verbose", false);
        baseConfigParameter.put("moreVerbose", false);
        baseConfigParameter.put("oldEntries", oldEntries);

        JSONObject mavenArtifact = new JSONObject();
        mavenArtifact.put("groupId", "org.example");
        mavenArtifact.put("artifactId", "testing");
        mavenArtifact.put("repo", "releases");
        mavenArtifact.put("type", "jar");
        mavenArtifact.put("name", "test");
        mavenArtifact.put("version", "1.0.0");
        mavenArtifact.put("repoStrategy", "NEXUS");

        JSONArray mavenArtifacts = new JSONArray();
        mavenArtifacts.put(mavenArtifact);

        JSONObject object = new JSONObject("{ }");
        object.put("baseConfig", baseConfigParameter);
        object.put("mavenArtifacts", mavenArtifacts);
        writeJsonToFile(file, object);

        String path = file.getAbsolutePath();
        ConfigurationHolder configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue(configurationHolder.hasConfiguration());
        assertFalse("Complete config should not lead to an error.", configurationHolder.isErroneous());
        DownloaderConfiguration downloaderConfiguration = configurationHolder.getDownloaderConfiguration();
        assertNotNull(downloaderConfiguration);
        assertEquals(2, downloaderConfiguration.getKeepNumberOfOldEntries());
        assertTrue("Delete was set to true.", downloaderConfiguration.deleteOldEntries());
        assertEquals("url", downloaderConfiguration.getNexusBaseUrl());
        assertEquals("user", downloaderConfiguration.getUsername());
        assertEquals("pass", downloaderConfiguration.getPassword());
        assertEquals("service/rest/beta/search", downloaderConfiguration.getNexusPathRestSearch());

        Iterable<ArtifactConfiguration> artifactConfigurations = downloaderConfiguration.getArtifactConfigurations();
        assertNotNull(artifactConfigurations);
        ArtifactConfiguration artifactConfiguration = artifactConfigurations.iterator().next();
        assertNotNull(artifactConfiguration);
        assertEquals("org.example", artifactConfiguration.getMavenGroupId());
        assertEquals("testing", artifactConfiguration.getMavenArtifactId());
        assertEquals("releases", artifactConfiguration.getRepository());
        assertEquals("jar", artifactConfiguration.getMavenType());
        assertEquals("test", artifactConfiguration.getName().get());
        assertEquals("1.0.0", artifactConfiguration.getArtifactVersion().get());
        assertEquals(RepositoryStrategy.NEXUS, artifactConfiguration.getRepositoryStrategy());


        baseConfigParameter.put("nexusPathRestSearch", "service/rest/api/search");
        object.put("baseConfig", baseConfigParameter);
        writeJsonToFile(file, object);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue(configurationHolder.hasConfiguration());
        downloaderConfiguration = configurationHolder.getDownloaderConfiguration();
        assertNotNull(downloaderConfiguration);
        assertEquals("service/rest/api/search", downloaderConfiguration.getNexusPathRestSearch());
    }

    @Test
    public void testWrongArtifactConfigFile() throws IOException {
        File file = File.createTempFile("downloader-test", "json");
        file.deleteOnExit();

        JSONObject oldEntries = new JSONObject();
        oldEntries.put("countToKeep", 2);
        oldEntries.put("delete", true);

        Map<String, Object> baseConfigParameter = new HashMap<>();
        baseConfigParameter.put("nexusUsername", "user");
        baseConfigParameter.put("nexusPassword", "pass");
        baseConfigParameter.put("nexusUrl", "url");
        baseConfigParameter.put("createRelativeSymlinks", false);
        baseConfigParameter.put("verbose", false);
        baseConfigParameter.put("moreVerbose", false);
        baseConfigParameter.put("oldEntries", oldEntries);

        JSONObject mavenArtifact = new JSONObject();
        mavenArtifact.put("groupId", "org.example");
        mavenArtifact.put("artifactId", "testing");
        mavenArtifact.put("repo", "releases");
        mavenArtifact.put("type", "jar");
        mavenArtifact.put("name", "test");
        mavenArtifact.put("version", "1.0.0");
        mavenArtifact.put("repoStrategy", "UNKNOWN");

        JSONArray mavenArtifacts = new JSONArray();
        mavenArtifacts.put(mavenArtifact);

        JSONObject object = new JSONObject("{ }");
        object.put("baseConfig", baseConfigParameter);
        object.put("mavenArtifacts", mavenArtifacts);
        writeJsonToFile(file, object);

        String path = file.getAbsolutePath();
        ConfigurationHolder configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue(configurationHolder.hasConfiguration());
        assertFalse("Complete config should not lead to an error.", configurationHolder.isErroneous());
        DownloaderConfiguration downloaderConfiguration = configurationHolder.getDownloaderConfiguration();
        assertNotNull(downloaderConfiguration);
        assertEquals(2, downloaderConfiguration.getKeepNumberOfOldEntries());
        assertTrue("Delete was set to true.", downloaderConfiguration.deleteOldEntries());
        assertEquals("url", downloaderConfiguration.getNexusBaseUrl());
        assertEquals("user", downloaderConfiguration.getUsername());
        assertEquals("pass", downloaderConfiguration.getPassword());

        Iterable<ArtifactConfiguration> artifactConfigurations = downloaderConfiguration.getArtifactConfigurations();
        assertNotNull(artifactConfigurations);
        assertFalse(artifactConfigurations.iterator().hasNext());
    }

    @Test
    public void emptyFileWithPathHelp() throws IOException {
        File file = File.createTempFile("downloader-test", "json");
        file.deleteOnExit();
        ConfigurationHolder configurationHolder = FileDownloaderConfiguration.create(new String[] {file.getAbsolutePath()});
        assertNotNull(configurationHolder);
        assertTrue("Empty file should lead to an error.", configurationHolder.isErroneous());
        assertNotNull("Should lead to error message", configurationHolder.getErrorMessage());
    }

    @Test
    public void erroneousFileWithPath() throws IOException {
        File file = File.createTempFile("downloader-test", "json");
        file.deleteOnExit();
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8)) {
            writer.write("{ test: {} }");
        }
        String path = file.getAbsolutePath();
        ConfigurationHolder configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue("Empty file should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'baseConfig' is missing in 'Configuration file'",
            configurationHolder.getErrorMessage());
        assertNotNull(configurationHolder.getException());
    }

    private String format(String text, String path) {
        return String.format(Locale.GERMAN, text, path);
    }

    @Test
    public void testMissingParametersFileWithPath() throws IOException {
        File file = File.createTempFile("downloader-test", "json");
        file.deleteOnExit();

        Map<String, Object> baseConfigParameter = new HashMap<>();
        writeParameter(file, baseConfigParameter);

        String path = file.getAbsolutePath();
        ConfigurationHolder configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue("Missing parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'createRelativeSymlinks' is missing in 'baseConfig'",
            configurationHolder.getErrorMessage());

        baseConfigParameter.put("createRelativeSymlinks", false);
        writeParameter(file, baseConfigParameter);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue("Missing parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'verbose' is missing in 'baseConfig'",
            configurationHolder.getErrorMessage());

        baseConfigParameter.put("verbose", false);
        writeParameter(file, baseConfigParameter);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue("Missing parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'moreVerbose' is missing in 'baseConfig'",
            configurationHolder.getErrorMessage());

        baseConfigParameter.put("moreVerbose", false);
        writeParameter(file, baseConfigParameter);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue("Missing required parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'mavenArtifacts' is missing in 'config'",
            configurationHolder.getErrorMessage());

        Map<String, Object> mavenArtifact = new HashMap<>();
        mavenArtifact.put("groupId", "org.example");
        mavenArtifact.put("artifactId", "testing");
        mavenArtifact.put("repo", "releases");
        mavenArtifact.put("type", "jar");
        mavenArtifact.put("name", "test");
        mavenArtifact.put("version", "1.0.0");
        mavenArtifact.put("repoStrategy", "NEXUS");

        writeParameter(file, baseConfigParameter, mavenArtifact);

        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue("Missing required parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'nexusUsername' is missing in 'baseConfig'",
            configurationHolder.getErrorMessage());

        baseConfigParameter.put("nexusUsername", "user");
        writeParameter(file, baseConfigParameter, mavenArtifact);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue("Missing parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'nexusPassword' is missing in 'baseConfig'",
            configurationHolder.getErrorMessage());

        baseConfigParameter.put("nexusPassword", "pass");
        writeParameter(file, baseConfigParameter, mavenArtifact);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue("Missing parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'nexusUrl' is missing in 'baseConfig'",
            configurationHolder.getErrorMessage());

        baseConfigParameter.put("nexusUrl", "url");
        writeParameter(file, baseConfigParameter, mavenArtifact);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertFalse("All required parameters should not lead to an error.", configurationHolder.isErroneous());


        mavenArtifact.put("repoStrategy", "GITHUB_PACKAGES");

        writeParameter(file, baseConfigParameter, mavenArtifact);

        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue("Missing required parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'githubOwner' is missing in 'baseConfig'",
            configurationHolder.getErrorMessage());

        baseConfigParameter.put("githubOwner", "owner");
        writeParameter(file, baseConfigParameter, mavenArtifact);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue("Missing parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'githubToken' is missing in 'baseConfig'",
            configurationHolder.getErrorMessage());

        baseConfigParameter.put("githubToken", "token");
        writeParameter(file, baseConfigParameter, mavenArtifact);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue("Missing parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'githubRepo' is missing in 'baseConfig'",
            configurationHolder.getErrorMessage());

        baseConfigParameter.put("githubRepo", "repo");
        writeParameter(file, baseConfigParameter, mavenArtifact);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertFalse("All required parameters should not lead to an error.", configurationHolder.isErroneous());
    }

    @Test
    public void testMissingNexusParametersFileWithPath() throws IOException {
        File file = File.createTempFile("downloader-test", "json");
        file.deleteOnExit();

        Map<String, Object> baseConfigParameter = new HashMap<>();
        baseConfigParameter.put("nexusUsername", "user");
        writeParameter(file, baseConfigParameter);

        String path = file.getAbsolutePath();
        ConfigurationHolder configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue("Missing parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'nexusPassword' is missing in 'baseConfig'",
            configurationHolder.getErrorMessage());

        baseConfigParameter.put("nexusPassword", "pass");
        writeParameter(file, baseConfigParameter);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue("Missing parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'nexusUrl' is missing in 'baseConfig'",
            configurationHolder.getErrorMessage());

        baseConfigParameter.put("nexusUrl", "url");
        writeParameter(file, baseConfigParameter);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);


        baseConfigParameter = new HashMap<>();
        baseConfigParameter.put("nexusUrl", "url");
        writeParameter(file, baseConfigParameter);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);

        assertTrue("Missing parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'nexusUsername' is missing in 'baseConfig'",
            configurationHolder.getErrorMessage());
    }
    @Test
    public void testMissingGithubParametersFileWithPath() throws IOException {
        File file = File.createTempFile("downloader-test", "json");
        file.deleteOnExit();

        Map<String, Object> baseConfigParameter = new HashMap<>();
        baseConfigParameter.put("githubToken", "token");
        writeParameter(file, baseConfigParameter);

        String path = file.getAbsolutePath();
        ConfigurationHolder configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue("Missing parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'githubOwner' is missing in 'baseConfig'",
            configurationHolder.getErrorMessage());

        baseConfigParameter.put("githubOwner", "owner");
        writeParameter(file, baseConfigParameter);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);
        assertTrue("Missing parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'githubRepo' is missing in 'baseConfig'",
            configurationHolder.getErrorMessage());

        baseConfigParameter.put("githubRepo", "url");
        writeParameter(file, baseConfigParameter);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);


        baseConfigParameter = new HashMap<>();
        baseConfigParameter.put("githubOwner", "owner");
        writeParameter(file, baseConfigParameter);
        configurationHolder = FileDownloaderConfiguration.create(new String[] {path});
        assertNotNull(configurationHolder);

        assertTrue("Missing parameter should lead to an error.", configurationHolder.isErroneous());
        assertEquals(
            "Failed to parse configuration file: Parameter 'githubToken' is missing in 'baseConfig'",
            configurationHolder.getErrorMessage());
    }

    private void writeJsonToFile(File file, JSONObject object) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(object.toString());
        }
    }

    private void writeParameter(File file, Map<String, Object> baseConfigParameter, Map<String, Object> mavenArtifact) throws IOException {
        JSONObject object = new JSONObject("{ }");
        object.put("baseConfig", baseConfigParameter);
        JSONArray value = new JSONArray();
        value.put(mavenArtifact);
        object.put("mavenArtifacts", value);
        writeJsonToFile(file, object);
    }

    private void writeParameter(File file, Map<String, Object> baseConfigParameter) throws IOException {
        JSONObject object = new JSONObject("{ }");
        object.put("baseConfig", baseConfigParameter);
        writeJsonToFile(file, object);
    }
}
