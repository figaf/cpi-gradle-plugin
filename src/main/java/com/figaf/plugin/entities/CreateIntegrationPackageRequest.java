package com.figaf.plugin.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergey Klochkov
 */
@Getter
@Setter
@ToString
public class CreateIntegrationPackageRequest {

    private String technicalName;
    private String displayName;
    private String shortDescription;
    private String vendor;
    private String version;

}
