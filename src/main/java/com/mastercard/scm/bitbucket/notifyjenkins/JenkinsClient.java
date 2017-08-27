package com.mastercard.scm.bitbucket.notifyjenkins;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Send notification requests to Jenkins Git plugin and Jenkins Bitbucket Branch Source plugin.
 */
@Component
public class JenkinsClient {

    private static final Logger log = LoggerFactory.getLogger(JenkinsClient.class);

    private static final String BITBUCKET_SCMSOURCE_HOOK_NOTIFY = "/bitbucket-scmsource-hook/notify";
    private static final String BITBUCKET_SERVER = "server";
    private static final String BITBUCKET_TYPE = "X-Bitbucket-Type";
    private static final String CHARSET = "UTF-8";
    private static final String EVENT_KEY = "X-Event-Key";

    private final PluginConfigService pluginConfigService;
    private final ObjectMapper objectMapper;

    @Autowired
    public JenkinsClient(PluginConfigService pluginConfigService) {
        this.pluginConfigService = pluginConfigService;
        this.objectMapper = new ObjectMapper();
    }

    public void notifyGit(String jenkinsKey, String repositoryUrl) {
        PluginConfig.JenkinsInstance jenkinsInstance = getJenkinsInstance(jenkinsKey);

        if (jenkinsInstance == null) {
            return;
        }

        try {
            String encodedRepositoryUrl = URLEncoder.encode(String.valueOf(repositoryUrl), CHARSET);
            String url = String.format("%s/git/notifyCommit?url=%s", jenkinsInstance.getUrl(), encodedRepositoryUrl);

            log.info("Send request to Jenkins Git Plugin using URL: {}", url);

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpGet get = new HttpGet(url);

                try (CloseableHttpResponse response = client.execute(get)) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    log.info("Response Code: {}", response.getStatusLine());
                    log.info("Response Body: {}", responseBody);
                }
            }
        } catch (UnsupportedEncodingException e) {
            String message = String.format("Error encoding repository URL: %s", repositoryUrl);
            log.error(message, e);
        } catch (IOException e) {
            log.error("Error sending request to Jenkins Git Plugin", e);
        }
    }

    public <T> void notifyBranchSource(String jenkinsKey, String eventCode, T resource) {
        try {
            PluginConfig.JenkinsInstance jenkinsInstance = getJenkinsInstance(jenkinsKey);

            if (jenkinsInstance == null) {
                return;
            }

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resource);
            String url = jenkinsInstance.getUrl() + BITBUCKET_SCMSOURCE_HOOK_NOTIFY;

            log.info("Send request to Jenkins Bitbucket Branch Source Plugin using URL: {} and data: {}", url, json);

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(url);
                post.setHeaders(new Header[]{
                        new BasicHeader(EVENT_KEY, eventCode),
                        new BasicHeader(BITBUCKET_TYPE, BITBUCKET_SERVER)
                });
                post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = client.execute(post)) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    log.info("Response Code: {}", response.getStatusLine());
                    log.info("Response Body: {}", responseBody);
                }
            }
        } catch (IOException e) {
            log.error("Error sending notification to Bitbucket Branch Source Plugin", e);
        }
    }

    private PluginConfig.JenkinsInstance getJenkinsInstance(String jenkinsKey) {
        log.debug("Searching for Jenkins Instance with key", jenkinsKey);

        PluginConfig config = this.pluginConfigService.getConfig();
        PluginConfig.JenkinsInstance jenkinsInstance = config.findInstance(jenkinsKey);

        if (jenkinsInstance == null) {
            log.error("Unable to find Jenkins instance for key: {}", jenkinsKey);
        }

        return jenkinsInstance;
    }
}
