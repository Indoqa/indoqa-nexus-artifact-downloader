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

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class MavenMetadataHelperTest {

    private String getMavenMetadata(String resource) {
        try {
            URL url = getClass().getClassLoader().getResource("com/indoqa/nexus/downloader/client/helpers/" + resource);
            return new String(Files.readAllBytes(Paths.get(url.toURI())), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Assert.fail("Error getting maven metadata for resource: " + resource);
        }
        return null;
    }

    @Test
    public void testGithubMavenMetadata() {
        MavenMetadataHelper mavenMetadataHelper = new MavenMetadataHelper(getMavenMetadata("github-maven-metadata.xml"));

        Optional<String> lastUpdated = mavenMetadataHelper.getLastUpdated();
        Assert.assertNotNull(lastUpdated);
        Assert.assertTrue("LastUpdated must be present.", lastUpdated.isPresent());
        Assert.assertEquals("20220615095653", lastUpdated.get());

        Optional<String> latest = mavenMetadataHelper.getLatest();
        Assert.assertNotNull(latest);
        Assert.assertTrue("Latest must be present.", latest.isPresent());
        Assert.assertEquals("2.193.0-SNAPSHOT", latest.get());
    }

    @Test
    public void testGithubVersionedMavenMetadata() {
        MavenMetadataHelper mavenMetadataHelper = new MavenMetadataHelper(getMavenMetadata("github-versioned-maven-metadata.xml"));

        Optional<String> updated = mavenMetadataHelper.getUpdated(Optional.of(""));
        Assert.assertNotNull(updated);
        Assert.assertFalse("Updated must not be present.", updated.isPresent());

        updated = mavenMetadataHelper.getUpdated(Optional.of("20220510083735"));
        Assert.assertNotNull(updated);
        Assert.assertTrue("Updated must be present.", updated.isPresent());
        Assert.assertEquals("2.193.0-20220510.083733-4", updated.get());
    }

    @Test
    public void testMavenCentralMavenMetadata() {
        MavenMetadataHelper mavenMetadataHelper = new MavenMetadataHelper(getMavenMetadata("central-maven-metadata.xml"));

        Optional<String> latest = mavenMetadataHelper.getLatest();
        Assert.assertNotNull(latest);
        Assert.assertTrue("Latest must be present.", latest.isPresent());
        Assert.assertEquals("0.2.1", latest.get());
    }
}