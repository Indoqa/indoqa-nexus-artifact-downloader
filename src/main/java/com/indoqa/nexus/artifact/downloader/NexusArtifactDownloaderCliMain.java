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

import com.indoqa.nexus.artifact.downloader.result.DownloadResult;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NexusArtifactDownloaderCliMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(NexusArtifactDownloaderCliMain.class);

    private static final String OPTION_DELETE = "d";
    private static final String OPTION_COUNT = "c";
    private static final String OPTION_RELATIVE_SYMLINK = "rsym";

    private static final String OPTION_NEXUS_BASE_URL = "url";
    private static final String OPTION_MAVEN_GROUP_ID = "mvng";
    private static final String OPTION_MAVEN_ARTIFACT_ID = "mvna";
    private static final String OPTION_MAVEN_REPOSITORY = "mvnr";
    private static final String OPTION_MAVEN_TYPE = "mvnt";
    private static final String OPTION_VERSION = "v";
    private static final String OPTION_USERNAME = "u";
    private static final String OPTION_PASSWORD = "p";

    private static final String OPTION_VERBOSE = "verbose";
    private static final String DEFAULT_MAVEN_TYPE = "jar";
    private static final String DEFAULT_MAVEN_REPOSITORY = "releases";
    private static final String DEFAULT_KEEP_ARTIFACTS_COUNT = "3";

    public static void main(String[] args) {
        CommandLine commandLine;
        Options options = getOptions();

        try {
            DefaultParser parser = new DefaultParser();
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            LOGGER.error("Failed to parse command line: " + e.getMessage());

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("NexusArtifactDownloaderCli", options);
            System.exit(-1);
            return;
        }

        String username = commandLine.getOptionValue(OPTION_USERNAME);
        String password = commandLine.getOptionValue(OPTION_PASSWORD);

        String nexusBaseUrl = commandLine.getOptionValue(OPTION_NEXUS_BASE_URL);
        String mavenGroupId = commandLine.getOptionValue(OPTION_MAVEN_GROUP_ID);
        String artifactId = commandLine.getOptionValue(OPTION_MAVEN_ARTIFACT_ID);
        String repository = commandLine.getOptionValue(OPTION_MAVEN_REPOSITORY, DEFAULT_MAVEN_REPOSITORY);
        String mavenType = commandLine.getOptionValue(OPTION_MAVEN_TYPE, DEFAULT_MAVEN_TYPE);

        configureLogging(commandLine);

        NexusArtifactDownloader downloader = new NexusArtifactDownloader(nexusBaseUrl, username, password, repository);

        if (commandLine.hasOption(OPTION_RELATIVE_SYMLINK)) {
            downloader.createRelativeSymLinks();
        }
        if (commandLine.hasOption(OPTION_DELETE)) {
            downloader.deleteOldEntries(getArtifactsToKeepCount(commandLine));
        }
        try {
            DownloadResult download = downloader.download(mavenGroupId, artifactId, mavenType);
            LOGGER.info(download.getMessage());
        } catch (DownloaderException e) {
            LOGGER.error("Error type: {}\n\t {}", e.getType(), e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Stacktrace: ", e.getCause());
            }
            System.exit(-1);
        }
    }

    private static int getArtifactsToKeepCount(CommandLine commandLine) {
        String optionValue = commandLine.getOptionValue(OPTION_COUNT, DEFAULT_KEEP_ARTIFACTS_COUNT);
        try {
            int result = Integer.parseInt(optionValue);
            if (result < 0) {
                LOGGER.debug("Negative value {} as count of artifacts to keep supplied. Will be ignored.", result);
            }
            return result;
        } catch (NumberFormatException e) {
            LOGGER.error("Could not parse count of ertifacts to keep '{}' \n\t {}.", optionValue, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Stacktrace: ", e.getCause());
            }
            System.exit(-1);
        }
        return -1;
    }

    private static void configureLogging(CommandLine commandLine) {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration config = context.getConfiguration();

        if (commandLine.hasOption(OPTION_VERBOSE)) {
            config.getRootLogger().getAppenders().put("Console", config.getAppender("ConsoleVerbose"));
            config.getRootLogger().setLevel(Level.DEBUG);

            if (commandLine.getOptionValue(OPTION_VERBOSE) != null) {
                config.getRootLogger().setLevel(Level.TRACE);
            }
        }
        context.updateLoggers();
    }

    private static Options getOptions() {
        Options options = new Options();

        Option nexusBaseUrlOption = new Option(OPTION_NEXUS_BASE_URL, "baseurl", true, "Nexus baseurl");
        nexusBaseUrlOption.setRequired(true);
        options.addOption(nexusBaseUrlOption);

        Option nexusUsernameOption = new Option(OPTION_USERNAME, "username", true, "Nexus username");
        nexusUsernameOption.setRequired(true);
        options.addOption(nexusUsernameOption);

        Option nexusPasswordOption = new Option(OPTION_PASSWORD, "password", true, "Nexus password");
        nexusPasswordOption.setRequired(true);
        options.addOption(nexusPasswordOption);

        Option mavenGroupIdOption = new Option(OPTION_MAVEN_GROUP_ID, "maven-group-id", true, "Group-Id of the maven artifact");
        mavenGroupIdOption.setRequired(true);
        options.addOption(mavenGroupIdOption);

        Option mavenArtifactIdOption = new Option(OPTION_MAVEN_ARTIFACT_ID,
            "maven-artifact-id",
            true,
            "Artifact-Id of the maven artifact");
        mavenArtifactIdOption.setRequired(true);
        options.addOption(mavenArtifactIdOption);

        Option mavenRepositoryOption = new Option(OPTION_MAVEN_REPOSITORY,
            "maven-repository",
            true,
            "Repository to search in for artifact");
        options.addOption(mavenRepositoryOption);

        Option mavenArtifactTypeOption = new Option(OPTION_MAVEN_TYPE, "maven-type", true, "Type of the maven artifact");
        options.addOption(mavenArtifactTypeOption);

        options.addOption(new Option(OPTION_DELETE, "delete", false, "Remove old artifacts"));
        options.addOption(new Option(OPTION_COUNT, "count", true, "Count of artifacts to keep"));
        options.addOption(new Option(OPTION_RELATIVE_SYMLINK, "relative-symlink", false, "Create relative symlinks"));

        Option artifactVersionOption = new Option(OPTION_VERSION, "version", true, "Version of the artifact");
        options.addOption(artifactVersionOption);

        Option verboseOption = new Option(OPTION_VERBOSE, "verbose output");
        options.addOption(verboseOption);

        return options;
    }

}
