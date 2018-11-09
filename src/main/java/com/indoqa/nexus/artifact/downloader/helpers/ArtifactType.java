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

public class ArtifactType {

    private String classifier;
    private String extension;

    private boolean isEmpty = true;

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
        this.isEmpty = false;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
        this.isEmpty = false;
    }

    public static ArtifactType extractFromClassifierExtension(String mavenType) {
        ArtifactType result = new ArtifactType();

        if (mavenType == null){
            return result;
        }

        int extensionIndex = mavenType.lastIndexOf('.');
        if (extensionIndex > -1) {
            String classifier = mavenType.substring(0, extensionIndex);
            if (classifier.startsWith("-")) {
                result.setClassifier(classifier.substring(1));
            } else {
                result.setClassifier(classifier);
            }
        }
        result.setExtension(mavenType.substring(extensionIndex + 1));

        return result;
    }

    public boolean includes(ArtifactType artifactType) {
        if (this.isEmpty) {
            return true;
        }
        return sameClassifier(artifactType) && sameExtension(artifactType);
    }

    private boolean sameExtension(ArtifactType artifactType) {
        if (this.extension == null) {
            return true;
        }

        return this.extension.equals(artifactType.getExtension());
    }

    private boolean sameClassifier(ArtifactType artifactType) {
        if (this.classifier == null) {
            return true;
        }
        return this.classifier.equals(artifactType.getClassifier());
    }

    @Override
    public String toString() {
        if (isEmpty) {
            return "not set";
        }
        return "ArtifactType{" + "classifier='" + classifier + '\'' + ", extension='" + extension + '\'' + "}";
    }
}
