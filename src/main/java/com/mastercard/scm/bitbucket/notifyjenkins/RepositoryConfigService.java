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

    public RepositoryConfigEntity getConfig(int repoId) {
        return this.entityManager.get(RepositoryConfigEntity.class, repoId);
    }

    public RepositoryConfigEntity createConfig(int repoId, RepositoryConfigDTO repoConfig) {
        return entityManager.create(
                RepositoryConfigEntity.class,
                new DBParam("ID", repoId),
                new DBParam("JENKINS_KEY", repoConfig.getJenkinsInstance()),
                new DBParam("ACTIVE", repoConfig.isActive()),
                new DBParam("TRIGGER_REPO_PUSH", repoConfig.isTriggerRepoPush()),
                new DBParam("TRIGGER_BRANCH_CREATED", repoConfig.isTriggerBranchCreated()),
                new DBParam("TRIGGER_BRANCH_DELETED", repoConfig.isTriggerBranchDeleted()),
                new DBParam("TRIGGER_FILE_EDIT", repoConfig.isTriggerFileEdit()),
                new DBParam("TRIGGER_PR_MERGED", repoConfig.isTriggerPRMerged()));
    }

    public RepositoryConfigEntity updateConfig(int repoId, RepositoryConfigDTO repoConfig) {
        RepositoryConfigEntity config = getConfig(repoId);

        if (config != null) {
            config.setIsActive(repoConfig.isActive());
            config.setJenkinsKey(repoConfig.getJenkinsInstance());
            config.setIsTriggerOnBranchCreated(repoConfig.isTriggerBranchCreated());
            config.setIsTriggerOnBranchDeleted(repoConfig.isTriggerBranchDeleted());
            config.setIsTriggerOnFileEdit(repoConfig.isTriggerFileEdit());
            config.setIsTriggerOnRepoPush(repoConfig.isTriggerRepoPush());
            config.setIsTriggerOnPullRequestMerge(repoConfig.isTriggerPRMerged());
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
