package com.mastercard.scm.bitbucket.notifyjenkins;

import net.java.ao.Accessor;
import net.java.ao.Entity;
import net.java.ao.Mutator;
import net.java.ao.Preload;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;


/**
 * Database table keyed by Repository Id which allows this plugin to persist notification data for a repo
 */
@Table("bbnj_repository")
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
    void setJenkinsKey(String pluginTarget);

    @Accessor("TARGET_PLUGIN")
    String getJenkinsTargetPlugin();

    @Mutator("TARGET_PLUGIN")
    void setJenkinsTargetPlugin(String targetPlugin);
}

