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

