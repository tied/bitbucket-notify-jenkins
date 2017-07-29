package com.mastercard.scm.bitbucket.notifyjenkins;


import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;


/**
 * Send requests to Jenkins
 */
@Component
public class JenkinsClient {

    private static final Logger log = LoggerFactory.getLogger(JenkinsClient.class);

    public static final String CHARSET = "UTF-8";

    public JenkinsResponse notify(URL jenkinsUrl, URL repoUrl) throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String requestUrl = buildRequestUrl(jenkinsUrl, repoUrl);
            HttpGet httpGet = new HttpGet(requestUrl);

            log.info("Notify Jenkins: {}", requestUrl);

            return httpclient.execute(httpGet, createResponseHandler());
        }
    }

    private ResponseHandler<JenkinsResponse> createResponseHandler() {
        return response -> {
            int status = response.getStatusLine().getStatusCode();
            String body = "";

            if (status == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    body = EntityUtils.toString(entity);
                }
            }

            log.info("Jenkins Response -> Status: {}, Body: {}", status, body);

            return new JenkinsResponse(status, body);
        };
    }

    protected String buildRequestUrl(URL jenkinsUrl, URL repoUrl) throws UnsupportedEncodingException, MalformedURLException {
        return String.format("%s/git/notifyCommit?url=%s",
                jenkinsUrl,
                URLEncoder.encode(String.valueOf(repoUrl), CHARSET));
    }
}
