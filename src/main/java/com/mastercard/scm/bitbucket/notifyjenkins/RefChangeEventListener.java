package com.mastercard.scm.bitbucket.notifyjenkins;

import com.atlassian.bitbucket.event.repository.RepositoryRefsChangedEvent;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.event.api.EventListener;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * Listens for any repo ref changes and will send notification to Jenkins
 * when the repository is configured for it.
 */
@Component
public class RefChangeEventListener {
    private static final Logger log = LoggerFactory.getLogger(RefChangeEventListener.class);

    private static final String REPO_PUSH = "repo:push";

    private final PluginConfigService pluginConfigService;
    private final RepositoryConfigService repositoryConfigService;
    private final ExecutorService executorService;
    private final RefChangeResourceAssembler resourceAssembler;
    private final JenkinsClient jenkinsClient;

    @Autowired
    public RefChangeEventListener(
            PluginConfigService pluginConfigService,
            @ComponentImport ExecutorService executorService,
            RepositoryConfigService repositoryConfigService,
            RefChangeResourceAssembler resourceAssembler,
            JenkinsClient jenkinsClient) {

        this.pluginConfigService = pluginConfigService;
        this.executorService = executorService;
        this.repositoryConfigService = repositoryConfigService;
        this.resourceAssembler = resourceAssembler;
        this.jenkinsClient = jenkinsClient;
    }

    @EventListener
    public void onRepositoryRefsChanged(RepositoryRefsChangedEvent event) {
        log.debug("onRepositoryRefsChanged");

        Repository repository = event.getRepository();

        RepositoryConfigEntity config = repositoryConfigService.getConfig(repository.getId());

        if (config == null) {
            log.debug("Unable to find config for repository: {}", repository.getId());
            return;
        }

        if (!config.isActive()) {
            log.debug("Plugin inactive for repository: {}", repository.getId());
            return;
        }

        if (JenkinsTargetPlugin.GIT.name().equals(config.getJenkinsTargetPlugin())) {
            log.info("Schedule Git notification for event: {} on repo: {}", REPO_PUSH, repository.getId());

            // move heavy lifting off of the event thread
            executorService.submit(() -> {
                String projectKey = event.getRepository().getProject().getKey();
                String repoSlug = event.getRepository().getSlug();
                String repoUrl = pluginConfigService.getConfig().buildRepoUrlString(projectKey, repoSlug);
                jenkinsClient.notifyGit(config.getJenkinsKey(), repoUrl);

            });

        } else if (JenkinsTargetPlugin.BRANCH_SOURCE.name().equals(config.getJenkinsTargetPlugin())) {
            log.info("Schedule Branch Source notification for event: {} on repo: {}", REPO_PUSH, repository.getId());

            // move heavy lifting off of the event thread
            executorService.submit(() -> jenkinsClient.notifyBranchSource(config.getJenkinsKey(), REPO_PUSH, resourceAssembler.assemble(event)));

        } else {
            log.error("Unknown JenkinsPluginTarget: {}", config.getJenkinsTargetPlugin());
        }
    }
}

