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
 * Data that will be sent to Bitbucket Branch source plugin on Ref change events
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefChangeResource {

    private Repository repository;
    private Push push;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Repository {
        private String scmId;
        private String slug;
        @JsonProperty(value = "public")
        private boolean pub;
        private Project project;
        private Map<String, List<Link>> links;
    }

    @Data
    @AllArgsConstructor
    public static class Push {
        private List<PushChange> changes;
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
    @NoArgsConstructor
    public static class PushChange {
        @JsonProperty(value = "created")
        private boolean created;
        @JsonProperty(value = "closed")
        private boolean closed;
        @JsonProperty(value = "old")
        private Ref oldRef;
        @JsonProperty(value = "new")
        private Ref newRef;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Ref {
        private String name;
        private Target target;
    }

    @Data
    @AllArgsConstructor
    public static class Target {
        private String hash;
    }
}
