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

import com.atlassian.bitbucket.ServiceException;
import com.atlassian.bitbucket.event.pull.PullRequestDeclinedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestEvent;
import com.atlassian.bitbucket.event.pull.PullRequestMergedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestOpenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestReopenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestRescopedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestUpdatedEvent;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.scm.Command;
import com.atlassian.bitbucket.scm.ScmService;
import com.atlassian.bitbucket.scm.pull.ScmPullRequestCommandFactory;
import com.atlassian.event.api.EventListener;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * Listens for Pull Request events and initiates needed Jenkins notifications.
 */
@Component
public class PullRequestEventListener {
    private static final Logger log = LoggerFactory.getLogger(PullRequestEventListener.class);

    private static final String PULL_REQUEST_CREATED = "pullrequest:created";
    private static final String PULL_REQUEST_UPDATED = "pullrequest:updated";
    private static final String PULL_REQUEST_FULFILLED = "pullrequest:fulfilled";
    private static final String PULL_REQUEST_REJECTED = "pullrequest:rejected";

    private final RepositoryConfigService repositoryConfigService;
    private final ExecutorService executorService;
    private final PullRequestResourceAssembler resourceAssembler;
    private final JenkinsClient jenkinsClient;
    private final ScmService scmService;

    @Autowired
    public PullRequestEventListener(
            @ComponentImport ExecutorService executorService,
            RepositoryConfigService repositoryConfigService,
            PullRequestResourceAssembler resourceAssembler,
            JenkinsClient jenkinsClient,
            @ComponentImport ScmService scmService) {

        this.executorService = executorService;
        this.repositoryConfigService = repositoryConfigService;
        this.resourceAssembler = resourceAssembler;
        this.jenkinsClient = jenkinsClient;
        this.scmService = scmService;
    }


    @EventListener
    public void onPullRequestCreated(PullRequestOpenedEvent event) {
        handleEvent(PULL_REQUEST_CREATED, event, true);
    }

    @EventListener
    public void onPullRequestUpdated(PullRequestUpdatedEvent event) {
        handleEvent(PULL_REQUEST_UPDATED, event);
    }

    @EventListener
    public void onPullRequestMerged(PullRequestMergedEvent event) {
        handleEvent(PULL_REQUEST_FULFILLED, event);
    }

    @EventListener
    public void onPullRequestDeclined(PullRequestDeclinedEvent event) {
        handleEvent(PULL_REQUEST_REJECTED, event);
    }

    @EventListener
    public void onPullRequestReopened(PullRequestReopenedEvent event) {
        PullRequest pullRequest = event.getPullRequest();

        boolean refsNotUpdated =
                pullRequest.getFromRef().getLatestCommit().equals(event.getPreviousFromHash()) &&
                        pullRequest.getToRef().getLatestCommit().equals(event.getPreviousToHash());

        if (refsNotUpdated) {
            // re-scope event will handle case when either ref has changed
            handleEvent(PULL_REQUEST_UPDATED, event);
        }
    }

    @EventListener
    public void onPullRequestReScoped(PullRequestRescopedEvent event) {
        if (event.isFromHashUpdated()) {
            handleEvent(PULL_REQUEST_UPDATED, event, true);
        }
    }

    private void handleEvent(String eventCode, PullRequestEvent event) {
        handleEvent(eventCode, event, false);
    }

    private void handleEvent(String eventCode, PullRequestEvent event, boolean updateRefs) {
        Repository repository = event.getPullRequest().getToRef().getRepository();
        RepositoryConfigEntity config = repositoryConfigService.getConfig(repository.getId());

        if (config == null) {
            log.debug("Unable to find config for repository: {}", repository.getId());
            return;
        }

        if (!config.isActive()) {
            log.debug("Plugin inactive for repository: {}", repository.getId());
            return;
        }

        if (JenkinsTargetPlugin.BRANCH_SOURCE.name().equals(config.getJenkinsTargetPlugin())) {
            log.info("Schedule notification for event: {} on repo: {}", eventCode, repository.getId());

            // move heavy lifting off of the event thread
            executorService.submit(() -> {
                if (updateRefs) {
                    forceRefUpdate(event.getPullRequest());
                }
                jenkinsClient.notifyBranchSource(config.getJenkinsKey(), eventCode, resourceAssembler.assemble(event));
            });
        }
    }

    /**
     * For performance reasons, Bitbucket Server will lazily update PR refs. For example,
     * it will trigger an update when a user requests the PR overview page. This is needed
     * so that it can determine if no merge conflict.
     * <p>
     * In order for Jenkins to properly build the PR refs, we must force the update
     * before sending the request to Jenkins. The call below will force the update.
     * The link below discussed the issue and the implications of doing this.
     * <p>
     * https://answers.atlassian.com/questions/239988
     */
    private void forceRefUpdate(PullRequest pullRequest) {
        try {
            ScmPullRequestCommandFactory pullRequestCommandFactory = scmService.getPullRequestCommandFactory(pullRequest);
            Command<?> command = pullRequestCommandFactory.tryMerge(pullRequest);
            command.call();
        } catch (ServiceException e) {
            log.warn("{}: Merge check failed; pull request refs may not be up-to-date", pullRequest, e);
        }
    }
}

