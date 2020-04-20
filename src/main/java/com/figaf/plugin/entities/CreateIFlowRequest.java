package com.figaf.plugin.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Arsenii Istlentev
 */
@Getter
@Setter
@ToString
public class CreateIFlowRequest {
    private String id;
    private String displayedName;
    private String description;
    private String type = "IFlow";
    private String additionalAttrs = "{\"source\":[],\"target\":[]}";
    private String fileName;
}
