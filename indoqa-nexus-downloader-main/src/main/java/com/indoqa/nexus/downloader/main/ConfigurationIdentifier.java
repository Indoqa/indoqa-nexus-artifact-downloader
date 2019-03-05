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
package com.indoqa.nexus.downloader.main;

public class ConfigurationIdentifier {

    private String repo;
    private String host;
    private String variant;

    public static ConfigurationIdentifier create(String repo, String host, String variant) {
        ConfigurationIdentifier configurationIdentifier = new ConfigurationIdentifier();

        configurationIdentifier.setRepo(repo);
        configurationIdentifier.setHost(host);
        configurationIdentifier.setVariant(variant);

        return configurationIdentifier;
    }

    public String getHost() {
        return this.host;
    }

    public String getRepo() {
        return this.repo;
    }

    public String getVariant() {
        return this.variant;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }
}
