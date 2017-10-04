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

import com.atlassian.bitbucket.server.ApplicationPropertiesService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;

/**
 *  App services that encapsulates nasty bits of loading and saving config data.
 *
 *  This class is interesting in that upon instantiation, it will attempt to find
 *  settings from bitbucket.properties. If settings found, they will be seeded
 *  into the plugin setting store.
 */
@Component
public class PluginConfigService {

    private static final Logger log = LoggerFactory.getLogger(PluginConfigService.class);

    private static final String PLUGIN_PREFIX = "plugin.";
    private static final String KEY_PREFIX = "com.mastercard.scm.bitbucket.notifyjenkins.";
    private static final String REPO_URL_PATTERN_KEY = KEY_PREFIX + "repoUrlPattern";
    private static final String INSTANCE_COUNT_KEY = KEY_PREFIX + "instanceCount";
    private static final String INSTANCE_CODE_KEY_FORMATTER = KEY_PREFIX + "instances[%s].code";
    private static final String INSTANCE_NAME_KEY_FORMATTER = KEY_PREFIX + "instances[%s].name";
    private static final String INSTANCE_URL_KEY_FORMATTER = KEY_PREFIX + "instances[%s].url";
    private static final String EMPTY = "";

    @ComponentImport
    private final ApplicationPropertiesService propertiesService;

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    @ComponentImport
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public PluginConfigService(
            PluginSettingsFactory pluginSettingsFactory,
            TransactionTemplate transactionTemplate,
            ApplicationPropertiesService propertiesService) {

        this.pluginSettingsFactory = pluginSettingsFactory;
        this.transactionTemplate = transactionTemplate;
        this.propertiesService = propertiesService;

        persistSeedDataAsSettings();
    }


    /**
     * Load config data from data store. If never saved, will return default values.
     */
    public PluginConfig getConfig() {
        return (PluginConfig) transactionTemplate.execute(() -> {
            SettingsReader reader = new SettingsReader(pluginSettingsFactory.createGlobalSettings());

            PluginConfig config = new PluginConfig();
            config.setRepoUrlPattern(reader.read(REPO_URL_PATTERN_KEY, EMPTY));
            config.setInstances(new ArrayList<>());

            int count = reader.read(INSTANCE_COUNT_KEY, 0);

            for (int i = 0; i < count; i++) {
                PluginConfig.JenkinsInstance instance = new PluginConfig.JenkinsInstance();
                instance.setCode(reader.read(String.format(INSTANCE_CODE_KEY_FORMATTER, i), EMPTY));
                instance.setName(reader.read(String.format(INSTANCE_NAME_KEY_FORMATTER, i), EMPTY));
                instance.setUrl(reader.read(String.format(INSTANCE_URL_KEY_FORMATTER, i), EMPTY));

                config.getInstances().add(instance);
            }

            return config;
        });
    }

    /**
     *  Save config data back to data store
     */
    public void saveConfig(PluginConfig config) {
        transactionTemplate.execute(() -> {
            PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
            pluginSettings.put(REPO_URL_PATTERN_KEY, config.getRepoUrlPattern());
            pluginSettings.put(INSTANCE_COUNT_KEY, String.valueOf(config.getInstances().size()));

            for (int i = 0; i < config.getInstances().size(); i++) {
                PluginConfig.JenkinsInstance instance = config.getInstances().get(i);
                pluginSettings.put(String.format(INSTANCE_CODE_KEY_FORMATTER, i), instance.getCode());
                pluginSettings.put(String.format(INSTANCE_NAME_KEY_FORMATTER, i), instance.getName());
                pluginSettings.put(String.format(INSTANCE_URL_KEY_FORMATTER, i), instance.getUrl());
            }

            return null;
        });
    }


    protected void persistSeedDataAsSettings() {
        PluginConfig seedData = getSeedData();

        log.info("Found Seed Data: {}", seedData.toString());

        boolean seedPropertiesSpecified = StringUtils.hasText(seedData.getRepoUrlPattern()) || !seedData.getInstances().isEmpty();

        if (seedPropertiesSpecified) {
            this.saveConfig(seedData);
        }
    }

    protected PluginConfig getSeedData() {
        PluginConfig config = new PluginConfig();
        config.setRepoUrlPattern(this.propertiesService.getPluginProperty(PLUGIN_PREFIX + REPO_URL_PATTERN_KEY, EMPTY));
        config.setInstances(new ArrayList<>());

        int i = 0;

        // load instances while we keep finding them
        while (true) {
            PluginConfig.JenkinsInstance instance = new PluginConfig.JenkinsInstance();
            instance.setCode(this.propertiesService.getPluginProperty(PLUGIN_PREFIX + String.format(INSTANCE_CODE_KEY_FORMATTER, i, EMPTY)));
            instance.setName(this.propertiesService.getPluginProperty(PLUGIN_PREFIX + String.format(INSTANCE_NAME_KEY_FORMATTER, i, EMPTY)));
            instance.setUrl(this.propertiesService.getPluginProperty(PLUGIN_PREFIX + String.format(INSTANCE_URL_KEY_FORMATTER, i, EMPTY)));

            if (StringUtils.hasText(instance.getCode())) {
                config.getInstances().add(instance);
                i++;
            } else {
                break;
            }
        }

        return config;
    }


    /**
     *  Helps reading settings from the blunt PluginConfig API
     */
    protected static class SettingsReader {

        private PluginSettings settings;

        public SettingsReader(PluginSettings settings) {
            this.settings = settings;
        }

        protected String read(String key, String defaultValue) {
            if (settings.get(key) != null) {
                return (String) settings.get(key);
            } else {
                return defaultValue;
            }
        }

        protected int read(String key, int defaultValue) {
            if (settings.get(key) != null) {
                return Integer.parseInt((String) settings.get(key));
            } else {
                return defaultValue;
            }
        }
    }
}
