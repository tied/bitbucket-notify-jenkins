package com.mastercard.scm.bitbucket.notifyjenkins;

import com.atlassian.bitbucket.event.branch.BranchCreatedEvent;
import com.atlassian.bitbucket.event.branch.BranchDeletedEvent;
import com.atlassian.bitbucket.event.content.FileEditedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestMergedEvent;
import com.atlassian.bitbucket.event.repository.RepositoryPushEvent;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.event.api.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Listens for events of interest and initiates Request to Jenkins
 */
@Component
public class EventBroker {
    private static final Logger log = LoggerFactory.getLogger(EventBroker.class);

    private final PluginConfigService pluginConfigService;
    private final RepositoryConfigService repositoryConfigService;
    private final JenkinsClient jenkinsClient;

    @Autowired
    public EventBroker(PluginConfigService pluginConfigService, RepositoryConfigService repositoryConfigService, JenkinsClient jenkinsClient) {
        this.pluginConfigService = pluginConfigService;
        this.repositoryConfigService = repositoryConfigService;
        this.jenkinsClient = jenkinsClient;
    }

    @EventListener
    public void onPush(RepositoryPushEvent event) {
        RepositoryConfigEntity config = getConfig(event.getRepository());

        if (config != null && config.isTriggerOnRepoPush()) {
            notifyJenkins(event.getRepository(), config);
        }
    }

    @EventListener
    public void onFileEdit(FileEditedEvent event) {
        RepositoryConfigEntity config = getConfig(event.getRepository());

        if (config != null && config.isTriggerOnFileEdit()) {
            notifyJenkins(event.getRepository(), config);
        }
    }

    @EventListener
    public void onPullRequestMerge(PullRequestMergedEvent event) {
        RepositoryConfigEntity config = getConfig(event.getRepository());

        if (config != null && config.isTriggerOnPullRequestMerge()) {
            notifyJenkins(event.getRepository(), config);
        }
    }

    @EventListener
    public void onBranchCreated(BranchCreatedEvent event) {
        RepositoryConfigEntity config = getConfig(event.getRepository());

        if (config != null && config.isTriggerOnBranchCreated()) {
            notifyJenkins(event.getRepository(), config);
        }
    }

    @EventListener
    public void onBranchDeleted(BranchDeletedEvent event) {
        RepositoryConfigEntity config = getConfig(event.getRepository());

        if (config != null && config.isTriggerOnBranchDeleted()) {
            notifyJenkins(event.getRepository(), config);
        }
    }

    protected RepositoryConfigEntity getConfig(Repository repository) {
        if (repository == null) {
            log.error("Repository must not be null");
            return null;
        }

        RepositoryConfigEntity config = this.repositoryConfigService.getConfig(repository.getId());

        if (config == null) {
            log.error("Unable to find configure for repository: {} ", repository.getId());
            return null;
        }

        return config;
    }

    private void notifyJenkins(Repository repository, RepositoryConfigEntity config) {
        if (config == null || !config.isActive()) {
            log.debug("config is null or inactive for repo: {}", repository.getId());
            return;
        }

        log.debug("Searching for Jenkins Instance with code", config.getJenkinsKey());

        PluginConfig pluginConfig = this.pluginConfigService.getConfig();
        PluginConfig.JenkinsInstance jenkinsInstance = pluginConfig.findInstance(config.getJenkinsKey());

        if (jenkinsInstance == null) {
            log.error("Unable to find Jenkins instance for code: {}", config.getJenkinsKey());
            return;
        }

        try {
            URL jenkinsUrl = new URL(jenkinsInstance.getUrl());
            URL repoUrl = pluginConfig.buildRepoUrl(repository.getProject().getKey(), repository.getSlug());

            log.info("Notify Jenkins: {}, for Repo: {}", jenkinsUrl, repoUrl);

            jenkinsClient.notify(jenkinsUrl, repoUrl);
        } catch (MalformedURLException ex) {
            log.error("Error building JenkinsUrl and/or RepoUrl", ex);
        } catch (IOException ex) {
            log.error("Error sending request to Jenkins", ex);
        }
    }
}
