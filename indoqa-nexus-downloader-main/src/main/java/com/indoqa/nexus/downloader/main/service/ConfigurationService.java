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
package com.indoqa.nexus.downloader.main.service;

import java.util.Optional;

import com.indoqa.nexus.downloader.main.ConfigurationIdentifier;
import com.indoqa.nexus.downloader.main.Elements;

public interface ConfigurationService {

    void deleteConfiguration(ConfigurationIdentifier identifier);

    Optional<byte[]> getConfiguration(ConfigurationIdentifier identifier);

    Elements getHosts(String repo, int start, int count);

    Elements getRepos(int start, int count);

    Elements getVariants(String repo, String host, int start, int count);

    void saveConfiguration(ConfigurationIdentifier identifier, byte[] data);

}
