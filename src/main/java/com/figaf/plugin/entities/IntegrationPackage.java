package com.figaf.plugin.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.UUID;

/**
 * @author Arsenii Istlentev
 */
@Getter
@Setter
@ToString
public class IntegrationPackage {

    private String id = UUID.randomUUID().toString();
    private String externalId;
    private String technicalName;
    private String displayedName;
    private String version;
    private Date creationDate;
    private String createdBy;
    private Date modificationDate;
    private String modifiedBy;
    private boolean deleted;
}
