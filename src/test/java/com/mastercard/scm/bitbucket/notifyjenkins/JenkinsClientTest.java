package com.mastercard.scm.bitbucket.notifyjenkins;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class JenkinsClientTest {

    public static final int PORT = 8934;

    JenkinsClient jenkinsClient;

    @Before
    public void before() {
        jenkinsClient = new JenkinsClient();
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(PORT);

    @Test
    public void notify_handlesResponse_whenRequestIsSuccessfully() throws IOException {
        stubFor(get(urlEqualTo("/jenkins/git/notifyCommit?url=http%3A%2F%2Flocalhost%2Fstash%2Frepo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain;charset=iso-8859-1")
                        .withBody("Scheduled polling")));

        URL jenkinsUrl = new URL("http://localhost:8934/jenkins");
        URL repoUrl = new URL("http://localhost/stash/repo");

        JenkinsResponse response = jenkinsClient.notify(jenkinsUrl, repoUrl);

        assertThat(response.getCode(), is(200));
        assertThat(response.getBody(), is("Scheduled polling"));
    }
}
