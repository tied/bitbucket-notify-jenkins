package com.mastercard.scm.bitbucket.notifyjenkins;

import net.java.ao.Accessor;
import net.java.ao.Entity;
import net.java.ao.Mutator;
import net.java.ao.Preload;
import net.java.ao.schema.Default;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;


/**
 * Database table keyed by Repository Id which allows this plugin to persist notification data for a repo
 */
@Table("BNJConfig")
@Preload
public interface RepositoryConfigEntity extends Entity {

    @NotNull
    @Accessor("ACTIVE")
    boolean isActive();

    @Mutator("ACTIVE")
    void setIsActive(boolean isActive);

    @Accessor("JENKINS_KEY")
    String getJenkinsKey();

    @Mutator("JENKINS_KEY")
    void setJenkinsKey(String jenkinsKey);

    @NotNull
    @Accessor("TRIGGER_REPO_PUSH")
    Boolean isTriggerOnRepoPush();

    @Mutator("TRIGGER_REPO_PUSH")
    void setIsTriggerOnRepoPush(Boolean isTriggerOnRepoPush);

    @NotNull
    @Accessor("TRIGGER_BRANCH_CREATED")
    boolean isTriggerOnBranchCreated();

    @Mutator("TRIGGER_BRANCH_CREATED")
    void setIsTriggerOnBranchCreated(boolean isTriggerOnBranchCreated);

    @NotNull
    @Accessor("TRIGGER_BRANCH_DELETED")
    boolean isTriggerOnBranchDeleted();

    @Mutator("TRIGGER_BRANCH_DELETED")
    void setIsTriggerOnBranchDeleted(boolean isTriggerOnBranchDeleted);

    @NotNull
    @Accessor("TRIGGER_FILE_EDIT")
    boolean isTriggerOnFileEdit();

    @Mutator("TRIGGER_FILE_EDIT")
    void setIsTriggerOnFileEdit(boolean isTriggerOnFileEdit);

    @NotNull
    @Accessor("TRIGGER_PR_MERGED")
    boolean isTriggerOnPullRequestMerge();

    @Mutator("TRIGGER_PR_MERGED")
    void setIsTriggerOnPullRequestMerge(boolean isTriggerOnPullRequestMerge);


}

