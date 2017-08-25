package com.mastercard.scm.bitbucket.notifyjenkins;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Represents plugin configuration data that is managed on a dedicated admin screen. These data
 * can also be updated by an admin via the REST API that the plugin exposes.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class PluginConfig {

    @XmlElement
    private String repoUrlPattern;

    /**
     * A list of Jenkins instances
     *
     * @return
     */
    @XmlElementWrapper(name = "instances")
    @XmlElement
    private List<JenkinsInstance> instances;

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class JenkinsInstance {
        @XmlElement
        private String code;
        @XmlElement
        private String name;
        @XmlElement
        private String url;
    }

    public JenkinsInstance findInstance(String code) {
        for (JenkinsInstance instance : this.getInstances()) {
            if (instance.getCode().equals(code)) {
                return instance;
            }
        }

        return null;
    }

    public URL buildRepoUrl(String projectKey, String repoSlug) throws MalformedURLException {
        return new URL(buildRepoUrlString(projectKey, repoSlug));
    }

    public String buildRepoUrlString(String projectKey, String repoSlug) {
        return getRepoUrlPattern()
                .replace("{{PROJECT_KEY}}", projectKey)
                .replace("{{REPO_SLUG}}", repoSlug);
    }
}

