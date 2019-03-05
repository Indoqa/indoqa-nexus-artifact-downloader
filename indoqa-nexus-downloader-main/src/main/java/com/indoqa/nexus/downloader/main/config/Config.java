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
package com.indoqa.nexus.downloader.main.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.indoqa.lang.exception.InitializationFailedException;
import com.indoqa.nexus.downloader.main.resources.AssignmentResource;
import com.indoqa.nexus.downloader.main.resources.ConfigurationResource;
import com.indoqa.nexus.downloader.main.service.AssignmentService;
import com.indoqa.nexus.downloader.main.service.ConfigurationService;
import com.indoqa.nexus.downloader.main.service.impl.FileAssignmentService;
import com.indoqa.nexus.downloader.main.service.impl.FileConfigurationService;

@Configuration
@PropertySource("classpath:/application.properties")
public class Config {

    @Value("${assignments.path}")
    private String assignmentsPath;

    @Value("${configurations.path}")
    private String configurationsPath;

    @Bean
    public AssignmentResource assignmentResource() {
        return new AssignmentResource();
    }

    @Bean
    public AssignmentService assignmentService() {
        Path path = Paths.get(this.assignmentsPath).toAbsolutePath().normalize();
        if (Files.notExists(path)) {
            throw new InitializationFailedException(
                "Assignments path '" + path + "' does not exist. Did you configure 'assignments.path' properly?");
        }

        FileAssignmentService assignmentService = new FileAssignmentService();
        assignmentService.setAssignmentsPath(path);
        return assignmentService;
    }

    @Bean
    public ConfigurationResource configurationResource() {
        return new ConfigurationResource();
    }

    @Bean
    public ConfigurationService configurationService() {
        Path path = Paths.get(this.configurationsPath).toAbsolutePath().normalize();
        if (Files.notExists(path)) {
            throw new InitializationFailedException(
                "Configurations path '" + path + "' does not exist. Did you configure 'configurations.path' properly?");
        }

        FileConfigurationService configurationService = new FileConfigurationService();
        configurationService.setConfigurationsPath(path);
        return configurationService;
    }
}
