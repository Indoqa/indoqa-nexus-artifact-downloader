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

import java.util.Optional;

import org.joox.JOOX;
import org.joox.Match;
import org.w3c.dom.Element;

public class MavenMetadataHelper {

    private final Match metadata;

    public MavenMetadataHelper(String mavenMetadata) {
        metadata = JOOX.$(mavenMetadata);
    }

    public Optional<String> getLatest() {
        Match match = metadata.find("latest");
        if (match.isEmpty() || match.get().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(match.get(0).getTextContent());
    }

    public Optional<String> getLastUpdated() {
        Match match = metadata.find("lastUpdated");
        if (match.isEmpty() || match.get().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(match.get(0).getTextContent());
    }

    public Optional<String> getUpdated(Optional<String> updated) {
        if (!updated.isPresent()) {
            return Optional.empty();
        }
        Match match = metadata.find("updated");
        if (match.isEmpty() || match.get().isEmpty()) {
            return Optional.empty();
        }

        String updatedValue = updated.get();
        for (Element element : match.get()) {
            if (!updatedValue.equals(element.getTextContent())) {
                continue;
            }

            Match value = JOOX.$(element.getParentNode()).find("value");
            if (value.isEmpty() || value.get().isEmpty()) {
                return Optional.empty();
            }

            return Optional.ofNullable(value.get(0).getTextContent());
        }

        return Optional.empty();
    }
}
