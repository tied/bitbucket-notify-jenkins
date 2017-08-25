package com.mastercard.scm.bitbucket.notifyjenkins;

import com.atlassian.bitbucket.event.repository.RepositoryRefsChangedEvent;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.repository.MinimalRef;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.Repository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RefChangeResourceAssemblerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    RepositoryRefsChangedEvent event;

    @Mock
    Repository repository;

    @Mock
    Project project;

    @Mock
    RefChange refChange;

    @Mock
    MinimalRef ref;

    @Mock
    PluginConfigService pluginConfigService;

    @InjectMocks
    RefChangeResourceAssembler resourceAssembler;

    @Before
    public void before() {
        PluginConfig config = new PluginConfig();
        config.setRepoUrlPattern("http://server/{{PROJECT_KEY}}/{{REPO_SLUG}}");
        when(pluginConfigService.getConfig()).thenReturn(config);

        when(project.getKey()).thenReturn("PROJ_KEY");
        when(project.getName()).thenReturn("PROJ_NAME");
        when(repository.getProject()).thenReturn(project);
        when(repository.getSlug()).thenReturn("REPO_SLUG");
        when(repository.isPublic()).thenReturn(true);
        when(refChange.getFromHash()).thenReturn("7acea690f93c6bebad2da449f26117ae77e2aae6");
        when(refChange.getToHash()).thenReturn("1bcba9ffac5d375a0027da0bc3705cf3a6a1b447");
        when(ref.getDisplayId()).thenReturn("master");
        when(refChange.getRef()).thenReturn(ref);
        when(event.getRepository()).thenReturn(repository);
        when(event.getRefChanges()).thenReturn(Arrays.asList(refChange));
    }

    @Test
    public void assemble_throwsIllegalArgumentException_whenEventIsNull() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("event must not be null");

        resourceAssembler.assemble(null);
    }

    @Test
    public void assemble_returnsRefChangeResource_whenRefAddedAdded() {
        when(refChange.getType()).thenReturn(RefChangeType.ADD);

        RefChangeResource resource = resourceAssembler.assemble(event);

        assertNotNull(resource);
        assertThat(resource.getRepository().getScmId(), is("git"));
        assertThat(resource.getRepository().getSlug(), is("REPO_SLUG"));
        assertThat(resource.getRepository().getProject().getKey(), is("PROJ_KEY"));
        assertThat(resource.getRepository().getProject().getName(), is("PROJ_NAME"));
        assertThat(resource.getRepository().getLinks().get("self").get(0).getHref(), is("http://server/PROJ_KEY/REPO_SLUG"));
        assertThat(resource.getPush().getChanges().get(0).isClosed(), is(false));
        assertThat(resource.getPush().getChanges().get(0).isCreated(), is(true));
        assertThat(resource.getPush().getChanges().get(0).getOldRef(), is(nullValue()));
        assertThat(resource.getPush().getChanges().get(0).getNewRef().getName(), is("master"));
        assertThat(resource.getPush().getChanges().get(0).getNewRef().getTarget().getHash(), is("1bcba9ffac5d375a0027da0bc3705cf3a6a1b447"));
    }

    @Test
    public void assemble_returnsRefChangeResource_whenRefDeleted() {
        when(refChange.getType()).thenReturn(RefChangeType.DELETE);

        RefChangeResource resource = resourceAssembler.assemble(event);

        assertNotNull(resource);
        assertThat(resource.getRepository().getScmId(), is("git"));
        assertThat(resource.getRepository().getSlug(), is("REPO_SLUG"));
        assertThat(resource.getRepository().getProject().getKey(), is("PROJ_KEY"));
        assertThat(resource.getRepository().getProject().getName(), is("PROJ_NAME"));
        assertThat(resource.getRepository().getLinks().get("self").get(0).getHref(), is("http://server/PROJ_KEY/REPO_SLUG"));
        assertThat(resource.getPush().getChanges().get(0).isClosed(), is(true));
        assertThat(resource.getPush().getChanges().get(0).isCreated(), is(false));
        assertThat(resource.getPush().getChanges().get(0).getOldRef().getName(), is("master"));
        assertThat(resource.getPush().getChanges().get(0).getOldRef().getTarget().getHash(), is("7acea690f93c6bebad2da449f26117ae77e2aae6"));
        assertThat(resource.getPush().getChanges().get(0).getNewRef(), is(nullValue()));
    }

    @Test
    public void assemble_returnsRefChangeResource_whenRefUpdate() {
        when(refChange.getType()).thenReturn(RefChangeType.UPDATE);

        RefChangeResource resource = resourceAssembler.assemble(event);

        assertNotNull(resource);
        assertThat(resource.getRepository().getScmId(), is("git"));
        assertThat(resource.getRepository().getSlug(), is("REPO_SLUG"));
        assertThat(resource.getRepository().getProject().getKey(), is("PROJ_KEY"));
        assertThat(resource.getRepository().getProject().getName(), is("PROJ_NAME"));
        assertThat(resource.getRepository().getLinks().get("self").get(0).getHref(), is("http://server/PROJ_KEY/REPO_SLUG"));
        assertThat(resource.getPush().getChanges().get(0).isClosed(), is(false));
        assertThat(resource.getPush().getChanges().get(0).isCreated(), is(false));
        assertThat(resource.getPush().getChanges().get(0).getOldRef().getName(), is("master"));
        assertThat(resource.getPush().getChanges().get(0).getOldRef().getTarget().getHash(), is("7acea690f93c6bebad2da449f26117ae77e2aae6"));
        assertThat(resource.getPush().getChanges().get(0).getNewRef().getName(), is("master"));
        assertThat(resource.getPush().getChanges().get(0).getNewRef().getTarget().getHash(), is("1bcba9ffac5d375a0027da0bc3705cf3a6a1b447"));
    }
}