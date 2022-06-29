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
package com.indoqa.nexus.downloader.integration.tests;

import static com.indoqa.nexus.downloader.integration.tests.utilities.PathMatchers.*;
import static com.indoqa.system.test.tools.JarRunnerUtils.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.indoqa.nexus.downloader.client.NexusDownloaderClient;
import com.indoqa.nexus.downloader.integration.tests.utilities.MavenCentral;
import com.indoqa.nexus.downloader.integration.tests.utilities.TestAppender;
import com.indoqa.system.test.tools.JarRunner;
import com.indoqa.system.test.tools.JarRunnerBuilder;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.*;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

public class IntegrationTest {

    private static final int PORT = 25001;
    private static final String BASE_URL = "http://localhost:" + PORT;
    private static final String CHECK_ADDRESS = BASE_URL + "/system-info";

    private static final Path RUNNABLE_DIR = Paths.get("../indoqa-nexus-downloader-main/target/");
    private static final Path RUNNABLE_MAIN = searchJavaRunnable(RUNNABLE_DIR, endsWithRunnableJar());

    private static final String UUID = "5d77a3ee-720a-49a0-9380-23e025a3e565";
    private static final String VARIANT = "testvariant";
    public static final String TESTHOST = "testhost";

    private static TestAppender appender = new TestAppender();

    @Rule
    public final ExpectedSystemExit expectedSystemExit = ExpectedSystemExit.none();

    @ClassRule
    public static final JarRunner MAIN_RUNNER = new JarRunnerBuilder(RUNNABLE_MAIN)
        .setCheckAdress(CHECK_ADDRESS)
        .preInitialization(IntegrationTest::createLogPath)
        .addSysProp("port", PORT)
        .addSysProp("admin.port", PORT + 1000)
        .addSysProp("log-path", "./target/logs")
        .addSysProp("assignments.path", Paths.get("./src/test/resources/assignments").toString())
        .addSysProp("configurations.path", Paths.get("./src/test/resources/configurations").toString())
        .build();

    private static void createLogPath() {
        try {
            Files.createDirectories(Paths.get("./target/logs"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeClass
    public static void setupLogAppender() {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration loggerConfig = context.getConfiguration();
        appender.start();
        loggerConfig.getRootLogger().addAppender(appender, Level.TRACE, null);
    }

    @AfterClass
    public static void tearDownLogAppender() {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration loggerConfig = context.getConfiguration();
        appender.stop();
        loggerConfig.getRootLogger().removeAppender(appender.getName());
    }

    @Before
    public void setup() {
        System.setProperty("disable-ssl", Boolean.TRUE.toString());
        appender.clear();
    }

    @Test
    public void testClientMain() throws ParseException {
        org.junit.Assume.assumeThat(MavenCentral.isReachable(), is(true));

        String[] arguments = {"-n", "testhost", "-d", "localhost:" + PORT, UUID, VARIANT};
        NexusDownloaderClient.main(arguments);

        Path path = Paths.get("./target/client-downloads/artifact");
        assertThat(path, both(exists()).and(isDirectory()));

        Path artifactSymlink = path.resolve("maven-artifact.pom");
        assertThat(artifactSymlink, both(exists()).and(isFile()).and(isSymbolicLink()));

        Path artifactArchive = path.resolve("archive/releases/maven-artifact-3.6.0.pom");
        assertThat(artifactArchive, both(exists()).and(isFile()).and(not(isSymbolicLink())));

        assertThat(
            appender.getMessage(),
            both(containsString("Will download  org.apache.maven:maven-artifact")).and(containsString("Symlink created")));
    }

    @Test
    public void wrongUuid() throws ParseException {
        expectedSystemExit.expectSystemExitWithStatus(-1);
        expectedSystemExit.checkAssertionAfterwards(() -> assertThat(appender.getMessage(),
            both(containsString("Could not download configuration")).and(containsString("status code: 404"))));

        String[] arguments = {"-v", "-n", TESTHOST, "-d", "localhost:" + PORT, "wrongUUID", VARIANT};
        NexusDownloaderClient.main(arguments);
    }

    @Test
    public void defaultDoesNotExist() throws ParseException {
        expectedSystemExit.expectSystemExitWithStatus(-1);
        expectedSystemExit.checkAssertionAfterwards(() -> assertThat(appender.getMessage(),
            both(containsString("Could not download configuration")).and(containsString("status code: 404"))));

        String[] arguments = {"-v", "-n", TESTHOST, "-d", "localhost:" + PORT, UUID};
        NexusDownloaderClient.main(arguments);
    }

    @Test
    public void unknownDownloaderMain() throws ParseException {
        String domain = java.util.UUID.randomUUID().toString();
        expectedSystemExit.expectSystemExitWithStatus(-1);
        expectedSystemExit.checkAssertionAfterwards(() -> assertThat(appender.getMessage(),
            both(containsString("Could not download configuration")).and(containsString("UnknownHostException: " + domain))));

        String[] arguments = {"-v", "-n", TESTHOST, "-d", domain, UUID, VARIANT};
        NexusDownloaderClient.main(arguments);
    }

    @Test
    public void wrongHost() throws ParseException {
        expectedSystemExit.expectSystemExitWithStatus(-1);
        expectedSystemExit.checkAssertionAfterwards(() -> assertThat(appender.getMessage(),
            both(containsString("Could not download configuration")).and(containsString("status code: 404"))));

        String[] arguments = {"-v", "-n", "other", "-d", "localhost:" + PORT, UUID, VARIANT};
        NexusDownloaderClient.main(arguments);
    }
}
