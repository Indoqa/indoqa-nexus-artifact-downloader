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
package com.indoqa.nexus.downloader.client.configuration;

import static org.apache.commons.cli.HelpFormatter.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Optional;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandlineDownloaderConfiguration implements DownloaderConfiguration, ArtifactConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandlineDownloaderConfiguration.class);

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
    private static final String OPTION_MAVEN_REPOSITORY_STRATEGY = "mvnrs";

    private static final String OPTION_VERBOSE = "verbose";
    private static final String DEFAULT_MAVEN_TYPE = "jar";
    private static final String DEFAULT_MAVEN_REPOSITORY = "releases";
    private static final String DEFAULT_KEEP_ARTIFACTS_COUNT = "3";

    private CommandLine commandLine;

    public static ConfigurationHolder create(String[] args) {
        try {
            DefaultParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(getOptions(), args);

            CommandlineDownloaderConfiguration configuration = new CommandlineDownloaderConfiguration();
            configuration.setCommandLine(commandLine);
            return ConfigurationHolder.config(configuration);
        } catch (ParseException e) {
            ConfigurationHolder error = ConfigurationHolder.error("Failed to parse command line: " + e.getMessage(), e);

            String helpMessage = createHelpMessage();
            error.setHelpMessage(helpMessage);
            return error;
        }
    }

    private static String createHelpMessage() {
        StringWriter writer = new StringWriter();
        new HelpFormatter().printHelp(new PrintWriter(writer, true),
            DEFAULT_WIDTH, "indoqa-nexus-com.indoqa.nexus.downloader", null,
            getOptions(), DEFAULT_LEFT_PAD, DEFAULT_DESC_PAD, null, false);
        return writer.toString();
    }

    private void setCommandLine(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    public static void printHelp() {
        new HelpFormatter().printHelp("NexusArtifactDownloaderCli", getOptions());
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

        Option repositoryStrategyOption = new Option(OPTION_MAVEN_REPOSITORY_STRATEGY, "maven-repository-strategy",true, "Repository strategy");
        options.addOption(repositoryStrategyOption);

        options.addOption(new Option(OPTION_DELETE, "delete", false, "Remove old artifacts"));
        options.addOption(new Option(OPTION_COUNT, "count", true, "Count of artifacts to keep"));
        options.addOption(new Option(OPTION_RELATIVE_SYMLINK, "relative-symlink", false, "Create relative symlinks"));

        Option artifactVersionOption = new Option(OPTION_VERSION, "version", true, "Version of the artifact");
        options.addOption(artifactVersionOption);

        Option verboseOption = new Option(OPTION_VERBOSE, "verbose output");
        options.addOption(verboseOption);

        return options;
    }

    public boolean verbose() {
        return this.commandLine.hasOption(OPTION_VERBOSE);
    }

    public boolean moreVerbose() {
        return this.commandLine.getOptionValue(OPTION_VERBOSE) != null;
    }

    @Override
    public String getUsername() {
        return this.commandLine.getOptionValue(OPTION_USERNAME);
    }

    @Override
    public String getPassword() {
        return this.commandLine.getOptionValue(OPTION_PASSWORD);
    }

    @Override
    public String getNexusBaseUrl() {
        return this.commandLine.getOptionValue(OPTION_NEXUS_BASE_URL);
    }

    @Override
    public String getMavenGroupId() {
        return this.commandLine.getOptionValue(OPTION_MAVEN_GROUP_ID);
    }

    @Override
    public String getMavenArtifactId() {
        return this.commandLine.getOptionValue(OPTION_MAVEN_ARTIFACT_ID);
    }

    @Override
    public String getRepository() {
        return this.commandLine.getOptionValue(OPTION_MAVEN_REPOSITORY, DEFAULT_MAVEN_REPOSITORY);
    }

    @Override
    public String getMavenType() {
        return this.commandLine.getOptionValue(OPTION_MAVEN_TYPE, DEFAULT_MAVEN_TYPE);
    }

    @Override
    public Optional<String> getArtifactVersion() {
        return Optional.ofNullable(this.commandLine.getOptionValue(OPTION_VERSION));
    }

    @Override
    public Optional<String> getName() {
        return Optional.empty();
    }

    @Override
    public RepositoryStrategy getRepositoryStrategy() {
        String optionValue = this.commandLine.getOptionValue(OPTION_MAVEN_REPOSITORY_STRATEGY, RepositoryStrategy.NEXUS.name());
        try{
            return RepositoryStrategy.valueOf(optionValue);
        }catch(Exception e) {
            LOGGER.error("'{}' is no valid repository strategy. Valid are {} ", optionValue, RepositoryStrategy.values());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Stacktrace: ", e.getCause());
            }
        }
        return RepositoryStrategy.NEXUS;
    }

    @Override
    public boolean createRelativeSymlinks() {
        return this.commandLine.hasOption(OPTION_RELATIVE_SYMLINK);
    }

    @Override
    public boolean deleteOldEntries() {
        return this.commandLine.hasOption(OPTION_DELETE);
    }

    @Override
    public int getKeepNumberOfOldEntries() {
        String optionValue = this.commandLine.getOptionValue(OPTION_COUNT, DEFAULT_KEEP_ARTIFACTS_COUNT);
        try {
            int result = Integer.parseInt(optionValue);
            if (result < 0) {
                LOGGER.debug("Negative value {} as count of artifacts to keep supplied. Will be ignored.", result);
            }
            return result;
        } catch (NumberFormatException e) {
            LOGGER.error("Could not parse count of artifacts to keep '{}' \n\t {}.", optionValue, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Stacktrace: ", e.getCause());
            }
        }
        return -1;
    }

    @Override
    public Iterable<ArtifactConfiguration> getArtifactConfigurations() {
        return Collections.singleton(this);
    }
}
