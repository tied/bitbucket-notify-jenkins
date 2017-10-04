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

import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;

public class PluginConfigTest {

    PluginConfig config;

    @Before
    public void before() throws MalformedURLException {
        config = new PluginConfig();

        config.setInstances(Arrays.asList(
                new PluginConfig.JenkinsInstance("ONE", "One", "http://jenkins/one"),
                new PluginConfig.JenkinsInstance("TWO", "Two", "http://jenkins/two")));
    }

    @Test
    public void findInstance_returnsNull_whenNotFound() {
        PluginConfig.JenkinsInstance actualInstance = config.findInstance("BAD");

        assertNull(actualInstance);
    }

    @Test
    public void findInstance_returnsNull_whenSearchCodeNull() {
        PluginConfig.JenkinsInstance actualInstance = config.findInstance(null);

        assertNull(actualInstance);
    }

    @Test
    public void findInstance_returnsInstance_whenMatchFound() {
        PluginConfig.JenkinsInstance actualInstance = config.findInstance("TWO");

        assertThat(actualInstance.getCode(), is("TWO"));
    }

    @Test
    public void buildRepoUrl_returnsInterpolatedValue_whenAsked() throws MalformedURLException {
        config.setRepoUrlPattern("http://jenkins/{{PROJECT_KEY}}/{{REPO_SLUG}}");

        URL actualUrl = config.buildRepoUrl("ABC", "123");

        assertThat(actualUrl, is(new URL("http://jenkins/ABC/123")));
    }
}