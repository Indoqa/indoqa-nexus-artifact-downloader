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

import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.indoqa.boot.json.resources.AbstractJsonResourcesBase;
import com.indoqa.boot.resources.exception.HttpStatusCode;
import com.indoqa.nexus.downloader.main.ConfigurationIdentifier;
import com.indoqa.nexus.downloader.main.Elements;
import com.indoqa.nexus.downloader.main.service.AssignmentService;
import com.indoqa.nexus.downloader.main.service.ConfigurationService;

import spark.Request;
import spark.Response;
import spark.ResponseTransformer;

public class ConfigurationResource extends AbstractJsonResourcesBase {

    private static final String HEADER_PROJECT = "IDQ-NEXUS-DL-PROJECT";

    private static final String HEADER_HOST = "IDQ-NEXUS-DL-HOST";
    private static final String HEADER_VARIANT = "IDQ-NEXUS-DL-VARIANT";
    private static final String PARAM_REPO = "repo";

    private static final String PARAM_HOST = "host";
    private static final String PARAM_VARIANT = "variant";
    @Inject
    private AssignmentService assignmentService;

    @Inject
    private ConfigurationService configurationsService;

    private static ConfigurationIdentifier createIdentifier(Request req) {
        String repo = req.params(PARAM_REPO);
        String host = req.params(PARAM_HOST);
        String variant = req.params(PARAM_VARIANT);

        return ConfigurationIdentifier.create(repo, host, variant);
    }

    @PostConstruct
    public void initialize() {
        ByteArrayResponseTransformer byteArrayResponseTransformer = new ByteArrayResponseTransformer();

        this.get("/configuration", (req, res) -> this.getConfigurationForProject(req), byteArrayResponseTransformer);

        this.get("/configurations", (req, res) -> this.getRepos(req));
        this.get("/configurations/:" + PARAM_REPO, (req, res) -> this.getHosts(req));
        this.get("/configurations/:" + PARAM_REPO + "/:" + PARAM_HOST, (req, res) -> this.getVariants(req));

        this.get(
            "/configurations/:" + PARAM_REPO + "/:" + PARAM_HOST + "/:" + PARAM_VARIANT,
            (req, res) -> this.getConfiguration(req),
            byteArrayResponseTransformer);
        this.put("/configurations/:" + PARAM_REPO + "/:" + PARAM_HOST + "/:" + PARAM_VARIANT, this::putConfiguration);
        this.delete("/configurations/:" + PARAM_REPO + "/:" + PARAM_HOST + "/:" + PARAM_VARIANT, this::deleteConfiguration);
    }

    private String deleteConfiguration(Request req, Response res) {
        ConfigurationIdentifier identifier = createIdentifier(req);
        this.configurationsService.deleteConfiguration(identifier);

        res.status(HttpStatusCode.NO_CONTENT.getCode());
        return null;
    }

    private byte[] getConfiguration(Request req) {
        ConfigurationIdentifier identifier = createIdentifier(req);
        return this.configurationsService.getConfiguration(identifier).orElseThrow(NotFoundException::resourceNotFound);
    }

    private byte[] getConfigurationForProject(Request req) {
        String project = req.headers(HEADER_PROJECT);
        String host = req.headers(HEADER_HOST);
        String variant = req.headers(HEADER_VARIANT);

        return this.assignmentService.getAssignment(project, host)
            .map(assignment -> assignment.createIdentifier(variant))
            .flatMap(identifier -> this.configurationsService.getConfiguration(identifier))
            .orElseThrow(NotFoundException::resourceNotFound);
    }

    private Elements getHosts(Request req) {
        ConfigurationIdentifier identifier = createIdentifier(req);
        int start = QueryParameterHelper.getStart(req);
        int count = QueryParameterHelper.getCount(req);

        return this.configurationsService.getHosts(identifier.getRepo(), start, count);
    }

    private Elements getRepos(Request req) {
        int start = QueryParameterHelper.getStart(req);
        int count = QueryParameterHelper.getCount(req);

        return this.configurationsService.getRepos(start, count);
    }

    private Elements getVariants(Request req) {
        ConfigurationIdentifier identifier = createIdentifier(req);
        int start = QueryParameterHelper.getStart(req);
        int count = QueryParameterHelper.getCount(req);

        return this.configurationsService.getVariants(identifier.getRepo(), identifier.getHost(), start, count);
    }

    private String putConfiguration(Request req, Response res) {
        ConfigurationIdentifier identifier = createIdentifier(req);
        this.configurationsService.saveConfiguration(identifier, req.bodyAsBytes());

        res.status(HttpStatusCode.NO_CONTENT.getCode());
        return null;
    }

    protected static class ByteArrayResponseTransformer implements ResponseTransformer {

        @Override
        public String render(Object model) throws Exception {
            return new String((byte[]) model, StandardCharsets.UTF_8);
        }
    }
}
