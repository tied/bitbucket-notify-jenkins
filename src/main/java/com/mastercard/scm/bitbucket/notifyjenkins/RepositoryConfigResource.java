/*
 * Copyright (c) 2017 Mastercard Worldwide
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mastercard.scm.bitbucket.notifyjenkins;


import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;

/**
 * Exposes HTTP endpoint allowing Javascript clients to post and read plugin settings.
 */
@Path("/{project}/{repo}/config")
public class RepositoryConfigResource {

    private static final Logger log = LoggerFactory.getLogger(RepositoryConfigResource.class);

    @ComponentImport
    private final PermissionService permissionService;
    @ComponentImport
    private final AuthenticationContext authContext;
    private final PluginConfigService pluginConfigService;
    private final RepositoryConfigService repoConfigService;
    private final RepositoryService repositoryService;

    @Autowired
    public RepositoryConfigResource(
            PluginConfigService configService,
            RepositoryConfigService repoConfigService,
            RepositoryService repositoryService,
            PermissionService permissionService,
            AuthenticationContext context) {

        this.pluginConfigService = configService;
        this.repoConfigService = repoConfigService;
        this.repositoryService = repositoryService;
        this.permissionService = permissionService;
        this.authContext = context;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("project") String project, @PathParam("repo") String repoSlug) {
        Repository repo = repositoryService.getBySlug(project, repoSlug);

        if (repo == null) {
            log.error("Unable to find repo for Project: {}, Slug: {}", project, repoSlug);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        if (!permissionService.hasRepositoryPermission(authContext.getCurrentUser(), repo.getId(), Permission.REPO_ADMIN)) {
            log.error("User: {} is not an admin for Project: {}, Slug: {}", authContext.getCurrentUser().getId(), project, repoSlug);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        RepositoryConfigEntity entity = repoConfigService.getConfig(repo.getId());

        if (entity == null) {
            log.info("Did not find configuration, returning default");
            return Response.ok(getDefaultRepoConfig()).build();
        } else {
            RepositoryConfigDTO config = new RepositoryConfigDTO();
            config.setActive(entity.isActive());
            config.setJenkinsInstance(entity.getJenkinsKey());
            config.setJenkinsTargetPlugin(entity.getJenkinsTargetPlugin());
            return Response.ok(config).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(final RepositoryConfigDTO config, @PathParam("project") String project, @PathParam("repo") String repoSlug) {
        Repository repo = repositoryService.getBySlug(project, repoSlug);

        if (repo == null) {
            log.error("Unable to find repo for Project: {}, Slug: {}", project, repoSlug);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        if (!permissionService.hasRepositoryPermission(authContext.getCurrentUser(), repo.getId(), Permission.REPO_ADMIN)) {
            log.error("User: {} is not an admin for Project: {}, Slug: {}", authContext.getCurrentUser().getId(), project, repoSlug);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        List<String> errors = validate(config);

        if (errors.isEmpty()) {
            repoConfigService.createOrUpdateConfig(repo.getId(), config);
            return Response.noContent().build();
        } else {
            return Response.status(SC_UNPROCESSABLE_ENTITY).entity(errors).build();
        }
    }

    private RepositoryConfigDTO getDefaultRepoConfig() {
        return new RepositoryConfigDTO();
    }

    private List<String> validate(RepositoryConfigDTO config) {
        List<String> errors = new ArrayList<>();

        if (config.isActive() && !this.pluginConfigService.getConfig().getInstances().stream().anyMatch(x -> x.getCode().equals(config.getJenkinsInstance()))) {
            errors.add("Please select a Jenkins Instance");
        }

        return errors;
    }
}