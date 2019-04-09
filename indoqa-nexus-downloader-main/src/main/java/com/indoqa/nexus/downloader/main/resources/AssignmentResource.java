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
package com.indoqa.nexus.downloader.main.resources;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.indoqa.boot.json.resources.AbstractJsonResourcesBase;
import com.indoqa.boot.resources.exception.HttpStatusCode;
import com.indoqa.nexus.downloader.main.Assignment;
import com.indoqa.nexus.downloader.main.Elements;
import com.indoqa.nexus.downloader.main.service.AssignmentService;

import spark.Request;
import spark.Response;

public class AssignmentResource extends AbstractJsonResourcesBase {

    private static final String PARAM_PROJECT = "project";
    @Inject
    private AssignmentService assignmentService;

    @PostConstruct
    public void initialize() {
        this.get("/assignments", (req, res) -> this.getAssignements(req));

        this.get("/assignments/:" + PARAM_PROJECT, (req, res) -> this.getAssignement(req));
        this.put("/assignments/:" + PARAM_PROJECT, (req, res) -> this.putAssignement(req, res));
        this.delete("/assignments/:" + PARAM_PROJECT, (req, res) -> this.deleteAssignment(req, res));

        this.get("/assignment-overview", (req, res) -> this.getAssignmentOverview(req));
    }

    private String deleteAssignment(Request req, Response res) {
        String project = req.params(PARAM_PROJECT);
        this.assignmentService.deleteAssignment(project);

        res.status(HttpStatusCode.NO_CONTENT.getCode());
        return null;
    }

    private Assignment getAssignement(Request req) {
        String project = req.params(PARAM_PROJECT);

        return this.assignmentService.getAssignment(project).orElseThrow(NotFoundException::resourceNotFound);
    }

    private Elements getAssignements(Request req) {
        int start = QueryParameterHelper.getStart(req);
        int count = QueryParameterHelper.getCount(req);

        return this.assignmentService.getAssignments(start, count);
    }

    private Elements getAssignmentOverview(Request req) {
        int start = QueryParameterHelper.getStart(req);
        int count = QueryParameterHelper.getCount(req);

        return this.assignmentService.getOverview(start, count);
    }

    private String putAssignement(Request req, Response res) {
        String project = req.params(PARAM_PROJECT);

        Assignment assignment = this.getTransformer().toObject(req.body(), Assignment.class);

        if (StringUtils.isBlank(assignment.getRepo())) {
            throw BadRequestException.badRequest("Repo must not be empty");
        }

        if (StringUtils.isBlank(assignment.getHost())) {
            throw BadRequestException.badRequest("Host must not be empty");
        }

        this.assignmentService.saveAssignment(project, assignment);

        res.status(HttpStatusCode.NO_CONTENT.getCode());
        return null;
    }
}
