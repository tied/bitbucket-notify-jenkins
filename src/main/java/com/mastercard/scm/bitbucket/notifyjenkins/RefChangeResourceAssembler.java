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

import com.atlassian.bitbucket.event.repository.RepositoryRefsChangedEvent;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.Repository;
import com.mastercard.scm.bitbucket.notifyjenkins.PluginConfig;
import com.mastercard.scm.bitbucket.notifyjenkins.PluginConfigService;
import com.mastercard.scm.bitbucket.notifyjenkins.RefChangeResource;
import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Build a RefChangeResource using the Bitbucket API
 */
@Component
public class RefChangeResourceAssembler {

    private final PluginConfigService pluginConfigService;

    @Autowired
    public RefChangeResourceAssembler(PluginConfigService pluginConfigService) {
        this.pluginConfigService = pluginConfigService;
    }

    public RefChangeResource assemble(RepositoryRefsChangedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }

        return new RefChangeResource(
                assembleRepository(event.getRepository()),
                assemblePush(event.getRefChanges()));
    }

    private RefChangeResource.Push assemblePush(Collection<RefChange> refChanges) {
        return new RefChangeResource.Push(
                refChanges.stream()
                        .map(this::getPushChange)
                        .collect(Collectors.toList()));
    }

    private RefChangeResource.PushChange getPushChange(RefChange change) {
        RefChangeResource.PushChange pushChange = new RefChangeResource.PushChange();
        RefChangeResource.Ref oldRef = null;
        RefChangeResource.Ref newRef = null;

        pushChange.setCreated(change.getType() == RefChangeType.ADD);
        pushChange.setClosed(change.getType() == RefChangeType.DELETE);

        switch (change.getType()) {
            case ADD:
                newRef = new RefChangeResource.Ref();
                newRef.setName(change.getRef().getDisplayId());
                newRef.setTarget(new RefChangeResource.Target(change.getToHash()));
                break;
            case DELETE:
                oldRef = new RefChangeResource.Ref();
                oldRef.setName(change.getRef().getDisplayId());
                oldRef.setTarget(new RefChangeResource.Target(change.getFromHash()));
                break;
            case UPDATE:
                newRef = new RefChangeResource.Ref();
                newRef.setName(change.getRef().getDisplayId());
                newRef.setTarget(new RefChangeResource.Target(change.getToHash()));
                oldRef = new RefChangeResource.Ref();
                oldRef.setName(change.getRef().getDisplayId());
                oldRef.setTarget(new RefChangeResource.Target(change.getFromHash()));
                break;
        }

        pushChange.setOldRef(oldRef);
        pushChange.setNewRef(newRef);

        return pushChange;
    }

    private RefChangeResource.Repository assembleRepository(Repository repository) {
        RefChangeResource.Repository resource = new RefChangeResource.Repository();
        resource.setScmId("git");
        resource.setSlug(repository.getSlug());
        resource.setPub(repository.isPublic());
        resource.setProject(assembleProject(repository.getProject()));
        resource.setLinks(assembleRepositoryLink(repository));

        return resource;
    }

    private Map<String, List<RefChangeResource.Link>> assembleRepositoryLink(Repository repository) {
        PluginConfig config = this.pluginConfigService.getConfig();
        String repoUrl = config.buildRepoUrlString(repository.getProject().getKey(), repository.getSlug());

        Map<String, List<RefChangeResource.Link>> links = new HashedMap();
        links.put("self", Arrays.asList(new RefChangeResource.Link(repoUrl)));
        return links;
    }

    private RefChangeResource.Project assembleProject(Project project) {
        return new RefChangeResource.Project(project.getKey(), project.getName());
    }
}
