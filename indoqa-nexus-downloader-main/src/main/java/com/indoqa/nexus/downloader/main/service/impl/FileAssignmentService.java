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
package com.indoqa.nexus.downloader.main.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indoqa.nexus.downloader.main.Assignment;
import com.indoqa.nexus.downloader.main.Elements;
import com.indoqa.nexus.downloader.main.service.AssignmentService;
import com.indoqa.nexus.downloader.main.service.ServiceException;
import com.indoqa.nexus.downloader.main.utils.FileHelper;

public class FileAssignmentService implements AssignmentService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Logger LOGGER = LoggerFactory.getLogger(FileAssignmentService.class);

    private Path assignmentsPath;

    @Override
    public void deleteAssignment(String project) {
        Path path = this.getAssignmentPath(project);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new ServiceException("Could not delete assignment.", e);
        }

    }

    @Override
    public Optional<Assignment> getAssignment(String project) {
        return Optional.of(this.getAssignmentPath(project))
            .filter(Files::exists)
            .flatMap(this::readAssignment);
    }

    @Override
    public Optional<Assignment> getAssignment(String project, String host) {
        return this.getAssignment(project).filter(assigment -> assigment.hasHost(host));
    }

    @Override
    public Elements getAssignments(int start, int count) {
        return FileHelper.getChildren(this.assignmentsPath, Files::isRegularFile, start, count);
    }

    @Override
    public void saveAssignment(String project, Assignment assignment) {
        Path path = this.getAssignmentPath(project);

        this.writeAssignment(path, assignment);
    }

    public void setAssignmentsPath(Path assignmentsPath) {
        this.assignmentsPath = assignmentsPath;
    }

    private Path getAssignmentPath(String project) {
        return this.assignmentsPath.resolve(project + ".json");
    }

    private Optional<Assignment> readAssignment(Path assignmentPath) {
        try {
            return Optional.of(OBJECT_MAPPER.readValue(assignmentPath.toFile(), Assignment.class));
        } catch (IOException e) {
            LOGGER.error("Could not read assignment from path '{}'", assignmentPath, e);
            return Optional.empty();
        }
    }

    private void writeAssignment(Path path, Assignment assignment) {
        try {
            OBJECT_MAPPER.writeValue(path.toFile(), assignment);
        } catch (IOException e) {
            throw new ServiceException("Could not write assigment.", e);
        }
    }
}
