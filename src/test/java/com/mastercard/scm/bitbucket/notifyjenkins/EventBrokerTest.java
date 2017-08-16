package com.mastercard.scm.bitbucket.notifyjenkins;

import com.atlassian.bitbucket.event.branch.BranchCreatedEvent;
import com.atlassian.bitbucket.event.branch.BranchDeletedEvent;
import com.atlassian.bitbucket.event.content.FileEditedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestMergedEvent;
import com.atlassian.bitbucket.event.repository.RepositoryPushEvent;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.repository.Repository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URL;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO: this test has too many dependencies, investigate refactoring EventBroker
@RunWith(MockitoJUnitRunner.class)
public class EventBrokerTest {


    public static final int REPO_ID = 1;
    public static final String REPO_SLUG = "MY_SLUG";
    public static final String PROJ_KEY = "MY_PROJ";
    public static final String JENKINS_URL = "http://jenkins";
    public static final String REPO_URL = "http://jenkins/notify";


    @Mock
    RepositoryPushEvent repositoryPushEvent;

    @Mock
    FileEditedEvent fileEditedEvent;

    @Mock
    PullRequestMergedEvent pullRequestMergedEvent;

    @Mock
    BranchCreatedEvent branchCreatedEvent;

    @Mock
    BranchDeletedEvent branchDeletedEvent;

    @Mock
    PluginConfigService pluginConfigService;

    @Mock
    RepositoryConfigService repositoryConfigService;

    @Mock
    JenkinsClient jenkinsClient;

    @Mock
    Repository repository;

    @Mock
    Project project;

    @Mock
    PluginConfig pluginConfig;

    @Mock
    RepositoryConfigEntity repositoryConfig;

    @InjectMocks
    EventBroker eventBroker;


    @Before
    public void before() throws IOException {
        // ensure repository/project return configured values
        when(project.getKey()).thenReturn(PROJ_KEY);
        when(repository.getId()).thenReturn(REPO_ID);
        when(repository.getSlug()).thenReturn(REPO_SLUG);
        when(repository.getProject()).thenReturn(project);

        // ensure each event type returns the repository
        when(repositoryPushEvent.getRepository()).thenReturn(repository);
        when(fileEditedEvent.getRepository()).thenReturn(repository);
        when(pullRequestMergedEvent.getRepository()).thenReturn(repository);
        when(branchCreatedEvent.getRepository()).thenReturn(repository);
        when(branchDeletedEvent.getRepository()).thenReturn(repository);

        when(pluginConfigService.getConfig()).thenReturn(pluginConfig);
        when(repositoryConfigService.getConfig(REPO_ID)).thenReturn(repositoryConfig);
        when(pluginConfig.findInstance(anyString())).thenReturn(new PluginConfig.JenkinsInstance("PROD", "Production", JENKINS_URL));
        when(pluginConfig.buildRepoUrl(PROJ_KEY, REPO_SLUG)).thenReturn(new URL(REPO_URL));
        when(jenkinsClient.notify(any(), any())).thenReturn(new JenkinsResponse());

        when(repositoryConfig.isActive()).thenReturn(true);
        when(repositoryConfig.getJenkinsKey()).thenReturn("KEY");
        when(repositoryConfig.isTriggerOnBranchCreated()).thenReturn(true);
        when(repositoryConfig.isTriggerOnBranchDeleted()).thenReturn(true);
        when(repositoryConfig.isTriggerOnFileEdit()).thenReturn(true);
        when(repositoryConfig.isTriggerOnPullRequestMerge()).thenReturn(true);
        when(repositoryConfig.isTriggerOnRepoPush()).thenReturn(true);
    }


    @Test
    public void onPush_doesNotNotifyJenkins_whenTriggerDisabled() throws IOException {
        when(repositoryConfig.isTriggerOnRepoPush()).thenReturn(false);

        eventBroker.onPush(repositoryPushEvent);

        verify(jenkinsClient, times(0)).notify(any(), any());
    }

    @Test
    public void onPush_notifiesJenkins_whenTriggerEnabled() throws IOException {
        eventBroker.onPush(repositoryPushEvent);

        verify(jenkinsClient, times(1)).notify(new URL(JENKINS_URL), new URL(REPO_URL));
    }

    @Test
    public void onFileEdit_doesNotNotifyJenkins_whenTriggerDisabled() throws IOException {
        when(repositoryConfig.isTriggerOnFileEdit()).thenReturn(false);

        eventBroker.onFileEdit(fileEditedEvent);

        verify(jenkinsClient, times(0)).notify(any(), any());
    }

    @Test
    public void onFileEdit_notifiesJenkins_whenTriggerEnabled() throws IOException {
        eventBroker.onPush(repositoryPushEvent);

        verify(jenkinsClient, times(1)).notify(new URL(JENKINS_URL), new URL(REPO_URL));
    }

    @Test
    public void onPullRequestMerge_doesNotNotifyJenkins_whenTriggerDisabled() throws IOException {
        when(repositoryConfig.isTriggerOnPullRequestMerge()).thenReturn(false);

        eventBroker.onPullRequestMerge(pullRequestMergedEvent);

        verify(jenkinsClient, times(0)).notify(any(), any());
    }

    @Test
    public void onPullRequestMerge_notifiesJenkins_whenTriggerEnabled() throws IOException {
        eventBroker.onPullRequestMerge(pullRequestMergedEvent);

        verify(jenkinsClient, times(1)).notify(new URL(JENKINS_URL), new URL(REPO_URL));
    }

    @Test
    public void onBranchCreated_doesNotNotifyJenkins_whenTriggerDisabled() throws IOException {
        when(repositoryConfig.isTriggerOnBranchCreated()).thenReturn(false);

        eventBroker.onBranchCreated(branchCreatedEvent);

        verify(jenkinsClient, times(0)).notify(any(), any());
    }

    @Test
    public void onBranchCreated_notifiesJenkins_whenTriggerEnabled() throws IOException {
        when(repositoryConfig.isTriggerOnBranchCreated()).thenReturn(true);

        eventBroker.onBranchCreated(branchCreatedEvent);

        verify(jenkinsClient, times(1)).notify(new URL(JENKINS_URL), new URL(REPO_URL));
    }

    @Test
    public void onBranchDeleted_doesNotNotifyJenkins_whenTriggerDisabled() throws IOException {
        when(repositoryConfig.isTriggerOnBranchDeleted()).thenReturn(false);

        eventBroker.onBranchDeleted(branchDeletedEvent);

        verify(jenkinsClient, times(0)).notify(any(), any());
    }

    @Test
    public void onBranchDeleted_notifiesJenkins_whenTriggerEnabled() throws IOException {
        when(repositoryConfig.isTriggerOnBranchDeleted()).thenReturn(true);

        eventBroker.onBranchDeleted(branchDeletedEvent);

        verify(jenkinsClient, times(1)).notify(new URL(JENKINS_URL), new URL(REPO_URL));
    }

    @Test
    public void anyEvent_doesNotNotifyJenkins_whenRepoConfigInactive() throws IOException {
        when(repositoryConfig.isActive()).thenReturn(false);

        eventBroker.onBranchDeleted(branchDeletedEvent);

        verify(jenkinsClient, times(0)).notify(any(), any());
    }


    @Test
    public void anyEvent_doesNotNotifyJenkins_whenRepoConfigHasMissingJenkinsKey() throws IOException {
        when(repositoryConfig.getJenkinsKey()).thenReturn(null);

        eventBroker.onBranchDeleted(branchDeletedEvent);

        verify(jenkinsClient, times(0)).notify(any(), any());
    }

    @Test
    public void anyEvent_doesNotNotifyJenkins_whenUnableToFindMatchingJenkinsKey() throws IOException {
        when(pluginConfig.findInstance(anyString())).thenReturn(null);

        eventBroker.onBranchDeleted(branchDeletedEvent);

        verify(jenkinsClient, times(0)).notify(any(), any());
    }


}