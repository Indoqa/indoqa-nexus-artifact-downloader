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

import org.junit.BeforeClass;
import org.junit.Test;

public class CommandlineDownloaderConfigurationTest {

    @Test
    public void emptyArgs() {
        String args[] = new String[] {""};
        ConfigurationHolder configurationHolder = CommandlineDownloaderConfiguration.create(args);
        assertNotNull(configurationHolder);
        assertTrue("Empty args should lead to an error.", configurationHolder.isErroneous());
    }

    @Test
    public void missingRequiredParameter() {
        String args[] = new String[] {"-url "};
        ConfigurationHolder configurationHolder = CommandlineDownloaderConfiguration.create(args);
        assertNotNull(configurationHolder);
        assertTrue("Empty args should lead to an error.", configurationHolder.isErroneous());
        assertEquals("Failed to parse command line: Missing required options: url, p, mvng, mvna",
            configurationHolder.getErrorMessage());
    }

    @Test
    public void ok() {
        String args[] = new String[] {"-url", "http://example.com", "-u", "user", "-p", "pass", "-mvng", "org.example", "-mvna", "test"};
        ConfigurationHolder configurationHolder = CommandlineDownloaderConfiguration.create(args);
        assertNotNull(configurationHolder);
        assertFalse("Args should not lead to an error.", configurationHolder.isErroneous());

        assertEquals("http://example.com", configurationHolder.getDownloaderConfiguration().getNexusBaseUrl());
        assertEquals("user", configurationHolder.getDownloaderConfiguration().getUsername());
        assertEquals("pass", configurationHolder.getDownloaderConfiguration().getPassword());
    }
}