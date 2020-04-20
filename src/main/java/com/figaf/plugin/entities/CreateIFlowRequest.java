package com.figaf.plugin.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

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
    private AdditionalAttributes additionalAttrs = new AdditionalAttributes();
    private String fileName;

    @Getter
    @ToString
    public static class AdditionalAttributes {

        private List<String> source = new ArrayList<>();
        private List<String> target = new ArrayList<>();
    }
}
