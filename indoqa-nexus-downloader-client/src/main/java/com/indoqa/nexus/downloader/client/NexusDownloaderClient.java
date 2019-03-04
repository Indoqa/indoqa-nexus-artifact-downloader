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
package com.indoqa.nexus.downloader.client;

import static org.apache.commons.cli.HelpFormatter.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.indoqa.nexus.downloader.client.configuration.*;
import com.indoqa.nexus.downloader.client.helpers.DownloaderException;
import com.indoqa.nexus.downloader.client.result.DownloadResult;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NexusDownloaderClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NexusDownloaderClient.class);

    private static final String OPTION_VERBOSE = "v";
    private static final String OPTION_HELP = "h";
    private static final String OPTION_DNS_NAME_SERVER = "d";
    private static final String OPTION_HOSTNAME = "n";

    public static void main(String[] args) throws ParseException {
        Options options = getOptions();

        DefaultParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);

        if (commandLine.hasOption(OPTION_HELP)) {
            printHelp(options);
            System.exit(0);
        }

        List<String> argList = commandLine.getArgList();
        if (argList.isEmpty()) {
            printHelp(options);
            System.exit(-1);
        }

        configureLogging(commandLine);

        ConfigurationHolder holder = SelfUpdaterDownloaderConfiguration.createSelfUpdateConfig(commandLine.getArgs());

        if (!holder.hasConfiguration()) {
            holder = FileDownloaderConfiguration.create(commandLine.getArgs());
        }

        if (!holder.hasConfiguration()) {
            holder = HttpDownloaderConfiguration.create(
                commandLine.getArgs(),
                Optional.ofNullable(commandLine.getOptionValue(OPTION_DNS_NAME_SERVER)),
                Optional.ofNullable(commandLine.getOptionValue(OPTION_HOSTNAME)));
        }

        if (!holder.hasConfiguration()) {
            if (holder.isErroneous()) {
                LOGGER.error(holder.getErrorMessage(), holder.getException());
            }

            LOGGER.info(holder.getHelpMessage());
            System.exit(-1);
        }

        DownloaderConfiguration configuration = holder.getDownloaderConfiguration();
        configureLogging(configuration);
        // commandline overwrites configuration logging
        configureLogging(commandLine);

        ArtifactHandler downloader = new ArtifactHandler(configuration);
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

    private static void configureLogging(CommandLine commandLine) {
        if (commandLine.hasOption(OPTION_VERBOSE)) {
            boolean multipleTimes =
                Arrays.stream(commandLine.getOptions()).map(Option::getOpt).filter(v -> v.equalsIgnoreCase(OPTION_VERBOSE)).count()
                    > 1;

            configureLogging(true, multipleTimes);
        }
    }

    private static void configureLogging(boolean verbose, boolean moreVerbose) {
        if (!verbose) {
            return;
        }

        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration loggerConfig = context.getConfiguration();

        loggerConfig.getRootLogger().getAppenders().put("Console", loggerConfig.getAppender("ConsoleVerbose"));

        Level level = Level.DEBUG;
        if (moreVerbose) {
            level = Level.TRACE;
        }

        Configurator.setLevel("com.indoqa", level);
    }

    private static void configureLogging(DownloaderConfiguration configuration) {
        configureLogging(configuration.verbose(), configuration.moreVerbose());
    }

    private static void printHelp(Options options) {
        StringWriter writer = new StringWriter();
        new HelpFormatter().printHelp(
            new PrintWriter(writer, true),
            DEFAULT_WIDTH,
            "indoqa-nexus-downloader [SELFUPDATE | UUID | CONFIG] [VARIANT]",
            null,
            getOptions(),
            DEFAULT_LEFT_PAD,
            DEFAULT_DESC_PAD,
            null,
            true);
        LOGGER.info(writer.toString());
    }

    private static Options getOptions() {
        Options result = new Options();

        result.addOption(Option.builder(OPTION_HELP).longOpt("help").desc("Print this help screen").build());
        result.addOption(Option.builder(OPTION_VERBOSE).longOpt("verbose").desc("Print verbose messages").build());
        result.addOption(Option
            .builder(OPTION_DNS_NAME_SERVER)
            .longOpt("dns")
            .desc("DNS-Name of the host serving configurations")
            .build());
        result.addOption(Option.builder(OPTION_HOSTNAME).longOpt("hostname").desc("Hostname of this server").hasArg().build());

        return result;
    }
}
