package com.mastercard.scm.bitbucket.notifyjenkins;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Repository specific configuration data that is passed back and forth between client and server.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public final class RepositoryConfigDTO {

    @XmlElement
    private boolean active;

    @XmlElement
    private String jenkinsInstance;

    @XmlElement
    private boolean triggerRepoPush;

    @XmlElement
    private boolean triggerBranchCreated;

    @XmlElement
    private boolean triggerBranchDeleted;

    @XmlElement
    private boolean triggerFileEdit;

    @XmlElement
    private boolean triggerPRMerged;
}

