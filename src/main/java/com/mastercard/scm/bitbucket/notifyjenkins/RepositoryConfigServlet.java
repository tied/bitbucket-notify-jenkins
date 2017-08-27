package com.mastercard.scm.bitbucket.notifyjenkins;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Renders a Repository Configuration form for Repository Administrators
 *
 *     Uses following resources:
 *       - /resources/repo-config.js
 *       - /resources/repo-config.vm
 */
@Component
public class RepositoryConfigServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(RepositoryConfigServlet.class);
    public static final String TEMPLATE_NAME = "repo-config.vm";

    @ComponentImport
    private final AuthenticationContext authContext;

    @ComponentImport
    private final PermissionService permissionService;

    @ComponentImport
    private final RepositoryService repositoryService;

    @ComponentImport
    private final TemplateRenderer renderer;

    @Autowired
    public RepositoryConfigServlet(
            AuthenticationContext authContext,
            PermissionService permissionService,
            TemplateRenderer renderer,
            RepositoryService repositoryService) {

        this.authContext = authContext;
        this.permissionService = permissionService;
        this.renderer = renderer;
        this.repositoryService = repositoryService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Repository repo = getRepository(request);

        if (repo == null) {
            log.error("Unable to find repo");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        if (!permissionService.hasRepositoryPermission(authContext.getCurrentUser(), repo.getId(), Permission.REPO_ADMIN)) {
            log.error("User: {} is not an admin", authContext.getCurrentUser().getId());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        response.setContentType("text/html;charset=utf-8");
        ImmutableMap.Builder<String, Object> properties = ImmutableMap.<String, Object>builder().put("repository", repo);
        renderer.render(TEMPLATE_NAME, properties.build(), response.getWriter());
    }


    private Repository getRepository(HttpServletRequest req) throws IOException {
        String pathInfo = req.getPathInfo();
        String[] components = pathInfo.split("/");

        if (components.length < 3) {
            return null;
        }

        return repositoryService.getBySlug(components[1], components[2]);
    }
}