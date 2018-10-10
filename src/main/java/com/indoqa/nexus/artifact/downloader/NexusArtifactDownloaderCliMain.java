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
package com.indoqa.nexus.artifact.downloader;

import com.indoqa.nexus.artifact.downloader.configuration.ArtifactConfiguration;
import com.indoqa.nexus.artifact.downloader.configuration.CommandlineDownloaderConfiguration;
import com.indoqa.nexus.artifact.downloader.configuration.DownloaderConfiguration;
import com.indoqa.nexus.artifact.downloader.configuration.FileDownloaderConfiguration;
import com.indoqa.nexus.artifact.downloader.result.DownloadResult;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NexusArtifactDownloaderCliMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(NexusArtifactDownloaderCliMain.class);

    public static void main(String[] args) {
        DownloaderConfiguration configuration = FileDownloaderConfiguration.create(args);

        if (configuration == null) {
            FileDownloaderConfiguration.printHelp();
            configuration = CommandlineDownloaderConfiguration.create(args);
        }

        if (configuration == null) {
            CommandlineDownloaderConfiguration.printHelp();
            System.exit(-1);
        }

        configureLogging(configuration);

        NexusArtifactDownloader downloader = new NexusArtifactDownloader(configuration);
        try {
            for (ArtifactConfiguration artifactConfiguration : configuration.getArtifactConfigurations()) {
                DownloadResult download = downloader.download(artifactConfiguration);
                LOGGER.info(download.getMessage());
            }
        } catch (DownloaderException e) {
            LOGGER.error("Error type: {}\n\t {}", e.getType(), e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Stacktrace: ", e.getCause());
            }
            System.exit(-1);
        }
    }

    private static void configureLogging(DownloaderConfiguration configuration) {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration loggerConfig = context.getConfiguration();

        if (configuration.verbose()) {
            loggerConfig.getRootLogger().getAppenders().put("Console", loggerConfig.getAppender("ConsoleVerbose"));
            loggerConfig.getRootLogger().setLevel(Level.DEBUG);

            if (configuration.moreVerbose()) {
                loggerConfig.getRootLogger().setLevel(Level.TRACE);
            }
        }
        context.updateLoggers();
    }
}
