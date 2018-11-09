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

public final class ConfigurationHolder {

    private DownloaderConfiguration downloaderConfiguration;

    private String helpMessage;

    private Exception exception;
    private String errorMessage;

    public static ConfigurationHolder config(DownloaderConfiguration configuration) {
        ConfigurationHolder result = new ConfigurationHolder();
        result.setDownloaderConfiguration(configuration);
        return result;
    }

    public static ConfigurationHolder help(String helpMessage) {
        ConfigurationHolder result = new ConfigurationHolder();
        result.setHelpMessage(helpMessage);
        return result;
    }

    public static ConfigurationHolder error(String error, Exception exception) {
        ConfigurationHolder result = new ConfigurationHolder();
        result.setErrorMessage(error);
        result.setException(exception);
        return result;
    }

    private ConfigurationHolder() {
        super();
    }

    public boolean hasConfiguration() {
        return downloaderConfiguration != null;
    }

    public boolean isErroneous() {
        return this.errorMessage != null;
    }

    public DownloaderConfiguration getDownloaderConfiguration() {
        return this.downloaderConfiguration;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public Exception getException() {
        return this.exception;
    }

    public String getHelpMessage() {
        return helpMessage;
    }

    public void setDownloaderConfiguration(DownloaderConfiguration downloaderConfiguration) {
        this.downloaderConfiguration = downloaderConfiguration;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setHelpMessage(String helpMessage) {
        this.helpMessage = helpMessage;
    }
}
