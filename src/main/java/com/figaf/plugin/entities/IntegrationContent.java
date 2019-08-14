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
public class IntegrationContent {

    private String id;
    private String version;
    private String name;
    private String type;
    private String deployedBy;
    private Date deployedOn;
    private String status;
}
