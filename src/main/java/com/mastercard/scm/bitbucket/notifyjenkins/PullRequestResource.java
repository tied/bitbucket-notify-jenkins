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
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Data that will be sent to the Bitbucket Branch Source plugin on Pull Request events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PullRequestResource {

    private Actor actor;
    @JsonProperty("pullrequest")
    private PullRequest pullRequest;
    private Repository repository;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Actor {
        private String username;
        private String displayName;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PullRequest {
        private String id;
        private String title;
        private String authorLogin;
        private Ref fromRef;
        private Ref toRef;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Repository {
        private String scmId;
        private String slug;
        private String fullName;
        private String ownerName;
        @JsonProperty(value = "public")
        private boolean pub;
        private Project project;
        private Owner owner;
        private Map<String, List<Link>> links;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Ref {
        private Repository repository;
        private Branch branch;
        private Commit commit;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Branch {
        private String rawNode;
        private String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Commit {
        private String hash;
        private String message;
        private String date;
        private int authorTimestamp;
    }

    @Data
    @AllArgsConstructor
    public static class Project {
        private String key;
        private String name;
    }

    @Data
    @AllArgsConstructor
    public static class Link {
        private String href;
    }

    @Data
    @AllArgsConstructor
    public static class Owner {
        private String username;
        private String displayName;
    }
}
