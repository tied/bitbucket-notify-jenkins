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

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import net.java.ao.DBParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides an interface for fetching and persisting repository configuration data
 */
@Component
public class RepositoryConfigService {

    @ComponentImport
    private final ActiveObjects entityManager;

    @Autowired
    public RepositoryConfigService(ActiveObjects entityManager) {

        this.entityManager = entityManager;
    }

    public RepositoryConfigEntity getConfig(int repositoryId) {
        return this.entityManager.get(RepositoryConfigEntity.class, repositoryId);
    }

    public RepositoryConfigEntity createConfig(int repositoryId, RepositoryConfigDTO repoConfig) {
        return entityManager.create(
                RepositoryConfigEntity.class,
                new DBParam("ID", repositoryId),
                new DBParam("JENKINS_KEY", repoConfig.getJenkinsInstance()),
                new DBParam("ACTIVE", repoConfig.isActive()),
                new DBParam("TARGET_PLUGIN", repoConfig.getJenkinsTargetPlugin()));
    }

    public RepositoryConfigEntity updateConfig(int repositoryId, RepositoryConfigDTO repoConfig) {
        RepositoryConfigEntity config = getConfig(repositoryId);

        if (config != null) {
            config.setIsActive(repoConfig.isActive());
            config.setJenkinsKey(repoConfig.getJenkinsInstance());
            config.setJenkinsTargetPlugin(repoConfig.getJenkinsTargetPlugin());
            config.save();
        }

        return config;
    }

    public RepositoryConfigEntity createOrUpdateConfig(int repoId, RepositoryConfigDTO repoConfig) {
        RepositoryConfigEntity config = getConfig(repoId);

        if (config != null) {
            return updateConfig(repoId, repoConfig);
        } else {
            return createConfig(repoId, repoConfig);
        }
    }
}
