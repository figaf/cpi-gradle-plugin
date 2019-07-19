package com.figaf.plugin;

import lombok.Getter;
import lombok.ToString;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

/**
 * @author Arsenii Istlentev
 */
@Getter
@ToString
public class CpiPluginExtension {

    private final Property<String> protocol;

    private final Property<String> host;

    private final Property<Integer> port;

    private final Property<String> username;

    private final Property<String> password;

    private final Property<String> sourceFilePath;

    private final Property<String> packageTechnicalName;

    private final Property<String> packageExternalId;

    private final Property<String> integrationFlowTechnicalName;

    private final Property<String> integrationFlowExternalId;

    private final Property<Boolean> waitForStartup;

    public CpiPluginExtension(Project project) {
        this.protocol = project.getObjects().property(String.class);
        this.host = project.getObjects().property(String.class);
        this.port = project.getObjects().property(Integer.class);
        this.username = project.getObjects().property(String.class);
        this.password = project.getObjects().property(String.class);
        this.sourceFilePath = project.getObjects().property(String.class);
        this.packageTechnicalName = project.getObjects().property(String.class);
        this.packageExternalId = project.getObjects().property(String.class);
        this.integrationFlowTechnicalName = project.getObjects().property(String.class);
        this.integrationFlowExternalId = project.getObjects().property(String.class);
        this.waitForStartup = project.getObjects().property(Boolean.class);
    }
}
