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

