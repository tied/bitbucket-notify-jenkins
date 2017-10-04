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
import com.atlassian.bitbucket.pull.PullRequestParticipant;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.user.ApplicationUser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestResourceAssemblerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    ApplicationUser user;

    @Mock
    PullRequestParticipant participant;

    @Mock
    PullRequest pullRequest;

    @Mock
    Repository fromRepository;

    @Mock
    Repository toRepository;

    @Mock
    PullRequestRef fromRef;

    @Mock
    PullRequestRef toRef;

    @Mock
    Project fromProject;

    @Mock
    Project toProject;

    @Mock
    PullRequestEvent event;

    @Mock
    PluginConfigService pluginConfigService;

    @InjectMocks
    TestPullRequestResourceAssembler resourceAssembler;

    @Before
    public void before() throws NoSuchMethodException, NoSuchFieldException {
        PluginConfig config = new PluginConfig();
        config.setRepoUrlPattern("http://server/{{PROJECT_KEY}}/{{REPO_SLUG}}");
        when(pluginConfigService.getConfig()).thenReturn(config);

        // user
        when(user.getName()).thenReturn("username");
        when(user.getDisplayName()).thenReturn("user_display_name");
        when(participant.getUser()).thenReturn(user);

        // from ref
        when(fromProject.getKey()).thenReturn("FROM_PROJ_KEY");
        when(fromProject.getName()).thenReturn("FROM_PROJ_NAME");
        when(fromRepository.getProject()).thenReturn(fromProject);
        when(fromRepository.getSlug()).thenReturn("FROM_REPO_SLUG");
        when(fromRepository.isPublic()).thenReturn(false);
        when(fromRef.getLatestCommit()).thenReturn("7acea690f93c6bebad2da449f26117ae77e2aae6");
        when(fromRef.getDisplayId()).thenReturn("master");
        when(fromRef.getRepository()).thenReturn(fromRepository);

        // to ref
        when(toProject.getKey()).thenReturn("TO_PROJ_KEY");
        when(toProject.getName()).thenReturn("TO_PROJ_NAME");
        when(toRepository.getProject()).thenReturn(toProject);
        when(toRepository.getSlug()).thenReturn("TO_REPO_SLUG");
        when(toRepository.isPublic()).thenReturn(true);
        when(toRef.getLatestCommit()).thenReturn("21b46b4fbb85de2f4fe2f96c7ffe7a2a6791ba52");
        when(toRef.getDisplayId()).thenReturn("master");
        when(toRef.getRepository()).thenReturn(toRepository);

        // pull request
        when(pullRequest.getAuthor()).thenReturn(participant);
        when(pullRequest.getFromRef()).thenReturn(fromRef);
        when(pullRequest.getToRef()).thenReturn(toRef);
        when(pullRequest.getId()).thenReturn(1L);
        when(pullRequest.getTitle()).thenReturn("title");

        when(event.getPullRequest()).thenReturn(pullRequest);
    }


    @Test
    public void assemble_throwsIllegalArgumentException_whenEventIsNull() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("event must not be null");

        resourceAssembler.assemble(null);
    }

    @Test
    public void assemble_returnsRefChangeResource() {
        PullRequestResource resource = resourceAssembler.assemble(event);

        assertNotNull(resource);
        assertThat(resource.getActor().getUsername(), is("username"));
        assertThat(resource.getActor().getDisplayName(), is("user_display_name"));
        assertThat(resource.getRepository().getProject().getKey(), is("TO_PROJ_KEY"));
        assertThat(resource.getRepository().getProject().getName(), is("TO_PROJ_NAME"));
        assertThat(resource.getRepository().getSlug(), is("TO_REPO_SLUG"));
        assertThat(resource.getRepository().isPub(), is(true));
        assertThat(resource.getPullRequest().getAuthorLogin(), is("user_display_name"));
        assertThat(resource.getPullRequest().getId(), is("1"));
        assertThat(resource.getPullRequest().getTitle(), is("title"));
        assertThat(resource.getPullRequest().getFromRef().getBranch().getName(), is("master"));
        assertThat(resource.getPullRequest().getFromRef().getBranch().getRawNode(), is("7acea690f93c6bebad2da449f26117ae77e2aae6"));
        assertThat(resource.getPullRequest().getFromRef().getCommit().getHash(), is("7acea690f93c6bebad2da449f26117ae77e2aae6"));
        assertThat(resource.getPullRequest().getFromRef().getRepository().getProject().getKey(), is("FROM_PROJ_KEY"));
        assertThat(resource.getPullRequest().getFromRef().getRepository().getProject().getName(), is("FROM_PROJ_NAME"));
        assertThat(resource.getPullRequest().getFromRef().getRepository().getSlug(), is("FROM_REPO_SLUG"));
        assertThat(resource.getPullRequest().getFromRef().getRepository().isPub(), is(false));
        assertThat(resource.getPullRequest().getToRef().getBranch().getName(), is("master"));
        assertThat(resource.getPullRequest().getToRef().getBranch().getRawNode(), is("21b46b4fbb85de2f4fe2f96c7ffe7a2a6791ba52"));
        assertThat(resource.getPullRequest().getToRef().getCommit().getHash(), is("21b46b4fbb85de2f4fe2f96c7ffe7a2a6791ba52"));
        assertThat(resource.getPullRequest().getToRef().getRepository().getProject().getKey(), is("TO_PROJ_KEY"));
        assertThat(resource.getPullRequest().getToRef().getRepository().getProject().getName(), is("TO_PROJ_NAME"));
        assertThat(resource.getPullRequest().getToRef().getRepository().getSlug(), is("TO_REPO_SLUG"));
        assertThat(resource.getPullRequest().getToRef().getRepository().isPub(), is(true));
    }

    /**
     * Wrap object under test to allow mocking the user object
     */
    public static class TestPullRequestResourceAssembler extends PullRequestResourceAssembler {

        private final ApplicationUser user;

        public TestPullRequestResourceAssembler(ApplicationUser user, PluginConfigService pluginConfigService) {
            super(pluginConfigService);
            this.user = user;
        }

        protected ApplicationUser getEventUser(PullRequestEvent event) {
            return user;
        }
    }
}