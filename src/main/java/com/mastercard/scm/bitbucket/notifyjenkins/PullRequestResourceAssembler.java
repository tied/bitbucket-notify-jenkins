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

import com.atlassian.bitbucket.event.pull.PullRequestEvent;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.user.ApplicationUser;
import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Build a PullRequestResource using the Bitbucket API
 */
@Component
public class PullRequestResourceAssembler {

    private final PluginConfigService pluginConfigService;

    @Autowired
    public PullRequestResourceAssembler(PluginConfigService pluginConfigService) {
        this.pluginConfigService = pluginConfigService;
    }

    public PullRequestResource assemble(PullRequestEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }

        return new PullRequestResource(
                assembleActor(getEventUser(event)),
                assemblePullRequest(event.getPullRequest()),
                assembleRepository(event.getPullRequest().getToRef().getRepository()));
    }

    /**
     * Since the user is final, must provide a way for unit tests to override this value
     */
    protected ApplicationUser getEventUser(PullRequestEvent event) {
        return event.getUser();
    }
    private PullRequestResource.Actor assembleActor(ApplicationUser user) {
        return new PullRequestResource.Actor(user.getName(), user.getDisplayName());
    }

    private PullRequestResource.PullRequest assemblePullRequest(PullRequest pullRequest) {
        PullRequestResource.PullRequest resource = new PullRequestResource.PullRequest();
        resource.setTitle(pullRequest.getTitle());
        resource.setAuthorLogin(pullRequest.getAuthor().getUser().getDisplayName());
        resource.setId(String.valueOf(pullRequest.getId()));
        resource.setFromRef(assembleRef(pullRequest.getFromRef()));
        resource.setToRef(assembleRef(pullRequest.getToRef()));

        return resource;
    }

    private PullRequestResource.Ref assembleRef(PullRequestRef ref) {
        PullRequestResource.Ref resource = new PullRequestResource.Ref();
        resource.setCommit(new PullRequestResource.Commit(ref.getLatestCommit(), null, null, 0));
        resource.setBranch(new PullRequestResource.Branch(ref.getLatestCommit(), ref.getDisplayId()));
        resource.setRepository(assembleRepository(ref.getRepository()));

        return resource;
    }

    private PullRequestResource.Repository assembleRepository(Repository repository) {
        PullRequestResource.Repository resource = new PullRequestResource.Repository();
        resource.setScmId("git");
        resource.setSlug(repository.getSlug());
        resource.setPub(repository.isPublic());
        resource.setProject(assembleProject(repository.getProject()));
        resource.setFullName(repository.getProject().getKey() + "/" + repository.getSlug());
        resource.setOwnerName(repository.getProject().getKey());
        resource.setOwner(assembleOwner(repository.getProject()));

        String repoUrl = pluginConfigService.getConfig().buildRepoUrlString(repository.getProject().getKey(), repository.getSlug());
        Map<String, List<PullRequestResource.Link>> links = new HashedMap();
        links.put("self", Arrays.asList(new PullRequestResource.Link(repoUrl)));
        resource.setLinks(links);

        return resource;
    }

    private PullRequestResource.Owner assembleOwner(Project project) {
        return new PullRequestResource.Owner(project.getKey(), project.getKey());
    }

    private PullRequestResource.Project assembleProject(Project project) {
        return new PullRequestResource.Project(project.getKey(), project.getName());
    }
}
