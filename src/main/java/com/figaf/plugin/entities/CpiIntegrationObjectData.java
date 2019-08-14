package com.figaf.plugin.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * @author Arsenii Istlentev
 */
@Getter
@Setter
@ToString
public class CpiIntegrationObjectData {

    private String integrationPackageId;
    private String externalId;
    private String technicalName;
    private String displayedName;
    private String version;
    private Date creationDate;
    private String createdBy;
    private Date modificationDate;
    private String modifiedBy;
    private Date deploymentDate;
    private String deployedBy;
    private String status;
    private boolean deployed = false;
    private String description;
}
