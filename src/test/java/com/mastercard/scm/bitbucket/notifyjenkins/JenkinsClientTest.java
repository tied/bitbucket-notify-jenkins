package com.mastercard.scm.bitbucket.notifyjenkins;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class JenkinsClientTest {

    public static final int PORT = 8934;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(PORT);

    @Mock
    PluginConfigService pluginConfigService;

    @InjectMocks
    JenkinsClient jenkinsClient;

    @Before
    public void before() {
        PluginConfig config = new PluginConfig();
        config.setInstances(Arrays.asList(new PluginConfig.JenkinsInstance("PROD", "Production", "http://localhost:8934")));

        when(pluginConfigService.getConfig()).thenReturn(config);
    }

    @Test
    public void notifyGit_sendsRequest() throws IOException {
        jenkinsClient.notifyGit("PROD", "http://bitbucket/repo.git");

        verify(1, getRequestedFor(urlEqualTo("/git/notifyCommit?url=http%3A%2F%2Fbitbucket%2Frepo.git")));
    }

    @Test
    public void notifyGit_doesNotSendRequest_whenJenkinsInstanceNotFound() throws IOException {
        jenkinsClient.notifyGit("__NOT_FOUND__", "http://bitbucket/repo.git");

        verify(0, getRequestedFor(urlEqualTo("/git/notifyCommit?url=http%3A%2F%2Fbitbucket%2Frepo.git")));
    }

    @Test
    public void notifyBranchSource_sendsRefChangeEvent() throws IOException, URISyntaxException {
        RefChangeResource resource = createExpectedRefChangeResource();

        jenkinsClient.notifyBranchSource("PROD", "repo:push", resource);

        verify(1, postRequestedFor(urlEqualTo("/bitbucket-scmsource-hook/notify"))
                .withHeader("X-Event-Key", equalTo("repo:push"))
                .withHeader("X-Bitbucket-Type", equalTo("server"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withRequestBody(equalTo(getResourceText("actual-ref-change-resource.json")))
        );
    }

    @Test
    public void notifyBranchSource_doesNotSendRefChangeEvent_whenJenkinsInstanceNotFound() throws IOException, URISyntaxException {
        RefChangeResource resource = createExpectedRefChangeResource();

        jenkinsClient.notifyBranchSource("__NOT_FOUND__", "repo:push", resource);

        verify(0, postRequestedFor(urlEqualTo("/bitbucket-scmsource-hook/notify")));
    }

    @Test
    public void notifyBranchSource_sendsPullRequestEvent() throws IOException, URISyntaxException {
        PullRequestResource resource = createExpectedPullRequestResource();

        jenkinsClient.notifyBranchSource("PROD", "pullrequest:created", resource);

        verify(1, postRequestedFor(urlEqualTo("/bitbucket-scmsource-hook/notify"))
                .withHeader("X-Event-Key", equalTo("pullrequest:created"))
                .withHeader("X-Bitbucket-Type", equalTo("server"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withRequestBody(equalTo(getResourceText("actual-pull-request-resource.json")))
        );
    }

    @Test
    public void notifyBranchSource_doesNotSendPullRequestEvent_whenJenkinsInstanceNotFound() throws IOException, URISyntaxException {
        PullRequestResource resource = createExpectedPullRequestResource();

        jenkinsClient.notifyBranchSource("__NOT_FOUND__", "pullrequest:created", resource);

        verify(0, postRequestedFor(urlEqualTo("/bitbucket-scmsource-hook/notify")));
    }

    private RefChangeResource createExpectedRefChangeResource() {
        return new RefChangeResource(
                new RefChangeResource.Repository(
                        "git",
                        "jenkins-project3",
                        true,
                        new RefChangeResource.Project("TEST", "test"),
                        createExpectedRefChangeLink()),
                new RefChangeResource.Push(Arrays.asList(new RefChangeResource.PushChange(
                        false,
                        false,
                        new RefChangeResource.Ref(
                                "master",
                                new RefChangeResource.Target("7acea690f93c6bebad2da449f26117ae77e2aae6")),
                        new RefChangeResource.Ref(
                                "master",
                                new RefChangeResource.Target("1bcba9ffac5d375a0027da0bc3705cf3a6a1b447")
                        )
                ))));
    }

    private PullRequestResource createExpectedPullRequestResource() {
        return new PullRequestResource(
                new PullRequestResource.Actor("admin", "Bitbucket Admin"),
                new PullRequestResource.PullRequest("82", "b.txt edited online with Bitbucket", "Bitbucket Admin",
                        new PullRequestResource.Ref(
                                new PullRequestResource.Repository("git", "final_fork", "~ADMIN/final_fork", "~ADMIN", false,
                                        new PullRequestResource.Project("~ADMIN", "~ADMIN"),
                                        new PullRequestResource.Owner("~ADMIN", "Bitbucket Admin"),
                                        createExpectedPullRequestLink("http://ech-10-24-130-210:7990/stash/scm/~ADMIN/final_fork.git")
                                ),
                                new PullRequestResource.Branch("21b46b4fbb85de2f4fe2f96c7ffe7a2a6791ba52", "final_fork_branch"),
                                new PullRequestResource.Commit("21b46b4fbb85de2f4fe2f96c7ffe7a2a6791ba52", null, null, 0)
                        ),
                        new PullRequestResource.Ref(
                                new PullRequestResource.Repository("git", "jenkins-project3", "TEST/jenkins-project3", "TEST", true,
                                        new PullRequestResource.Project("TEST", "test"),
                                        new PullRequestResource.Owner("TEST", "TEST"),
                                        createExpectedPullRequestLink("http://ech-10-24-130-210:7990/stash/scm/TEST/jenkins-project3.git")
                                ),
                                new PullRequestResource.Branch("1bcba9ffac5d375a0027da0bc3705cf3a6a1b447", "master"),
                                new PullRequestResource.Commit("1bcba9ffac5d375a0027da0bc3705cf3a6a1b447", null, null, 0)
                        )),
                new PullRequestResource.Repository("git", "jenkins-project3", "TEST/jenkins-project3", "TEST",
                        true,
                        new PullRequestResource.Project("TEST", "test"),
                        new PullRequestResource.Owner("TEST", "TEST"),
                        createExpectedPullRequestLink("http://ech-10-24-130-210:7990/stash/scm/TEST/jenkins-project3.git")
                ));
    }

    private Map<String, List<PullRequestResource.Link>> createExpectedPullRequestLink(String url) {
        Map<String, List<PullRequestResource.Link>> links = new HashedMap();
        links.put("self", Arrays.asList(new PullRequestResource.Link(url)));
        return links;
    }

    private Map<String, List<RefChangeResource.Link>> createExpectedRefChangeLink() {
        Map<String, List<RefChangeResource.Link>> links = new HashedMap();
        links.put("self", Arrays.asList(new RefChangeResource.Link("http://ech-10-24-130-210:7990/stash/scm/TEST/jenkins-project3.git")));
        return links;
    }

    private String getResourceText(String resourceName) throws URISyntaxException, IOException {
        URL url = this.getClass().getResource("/com/mastercard/scm/bitbucket/notifyjenkins/" + resourceName);
        String fileContent = IOUtils.toString(url.toURI(), Charset.forName("utf-8"));
        // convert line endings to current OS line endings
        return fileContent.replace("\r\n", System.getProperty("line.separator"));
    }
}