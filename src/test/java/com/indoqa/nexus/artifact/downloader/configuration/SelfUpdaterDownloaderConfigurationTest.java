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

import static org.junit.Assert.*;

import org.junit.Test;

public class SelfUpdaterDownloaderConfigurationTest {

    @Test
    public void nullParameter() {
        ConfigurationHolder configurationHolder = SelfUpdaterDownloaderConfiguration.createSelfUpdateConfig(null);
        assertNotNull(configurationHolder);
        assertFalse("Null args should not lead to an error.", configurationHolder.isErroneous());
        assertNotNull("Should lead to help message", configurationHolder.getHelpMessage());
        assertTrue("Help message should contain update command", configurationHolder.getHelpMessage().contains("selfupdate"));
    }

    @Test
    public void tooManyParameters() {
        String[] args = new String[] {"a", "B"};
        ConfigurationHolder configurationHolder = SelfUpdaterDownloaderConfiguration.createSelfUpdateConfig(args);
        assertNotNull(configurationHolder);
        assertFalse("Too many args should not lead to an error.", configurationHolder.isErroneous());
        assertNotNull("Should lead to help message", configurationHolder.getHelpMessage());
        assertTrue("Help message should contain update command", configurationHolder.getHelpMessage().contains("selfupdate"));
    }

    @Test
    public void wrongParameter() {
        String[] args = new String[] {"a"};
        ConfigurationHolder configurationHolder = SelfUpdaterDownloaderConfiguration.createSelfUpdateConfig(args);
        assertNotNull(configurationHolder);
        assertTrue("Wrong parameter should lead to an error.", configurationHolder.isErroneous());
    }

    @Test
    public void ok() {
        String[] args = new String[] {"selfupdate"};
        ConfigurationHolder configurationHolder = SelfUpdaterDownloaderConfiguration.createSelfUpdateConfig(args);
        assertNotNull(configurationHolder);
        assertFalse("Wright parameter should not lead to an error.", configurationHolder.isErroneous());
        DownloaderConfiguration downloaderConfiguration = configurationHolder.getDownloaderConfiguration();
        assertNotNull(downloaderConfiguration);
        ArtifactConfiguration artifactConfiguration = downloaderConfiguration.getArtifactConfigurations().iterator().next();
        assertNotNull(artifactConfiguration);
        assertEquals("com.indoqa", artifactConfiguration.getMavenGroupId());
        assertEquals("indoqa-nexus-artifact-downloader", artifactConfiguration.getMavenArtifactId());
        assertEquals("runnable.jar", artifactConfiguration.getMavenType());
        assertEquals(RepositoryStrategy.MAVEN_CENTRAL, artifactConfiguration.getRepositoryStrategy());
    }
}
