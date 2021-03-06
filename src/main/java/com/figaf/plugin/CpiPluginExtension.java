package com.figaf.plugin;

import com.figaf.integration.common.factory.HttpClientsFactory;
import lombok.Getter;
import lombok.ToString;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

/**
 * @author Arsenii Istlentev
 */
@Getter
@ToString
public class CpiPluginExtension {

    private final Property<String> url;

    private final Property<String> username;

    private final Property<String> password;

    private final Property<String> platformType;

    private final Property<String> sourceFilePath;

    private final Property<String> packageTechnicalName;

    private final Property<String> packageExternalId;

    private final Property<String> artifactTechnicalName;

    private final Property<String> artifactExternalId;

    private final Property<Boolean> waitForStartup;

    private final SetProperty<String> ignoreFilesList;

    private final Property<Boolean> uploadDraftVersion;

    private final Property<String> artifactType;

    private final Property<HttpClientsFactory> httpClientsFactory;

    public CpiPluginExtension(Project project) {
        this.url = project.getObjects().property(String.class);
        this.username = project.getObjects().property(String.class);
        this.password = project.getObjects().property(String.class);
        this.platformType = project.getObjects().property(String.class);
        this.sourceFilePath = project.getObjects().property(String.class);
        this.packageTechnicalName = project.getObjects().property(String.class);
        this.packageExternalId = project.getObjects().property(String.class);
        this.artifactTechnicalName = project.getObjects().property(String.class);
        this.artifactExternalId = project.getObjects().property(String.class);
        this.waitForStartup = project.getObjects().property(Boolean.class);
        this.ignoreFilesList = project.getObjects().setProperty(String.class);
        this.uploadDraftVersion = project.getObjects().property(Boolean.class);
        this.artifactType = project.getObjects().property(String.class);
        this.httpClientsFactory = project.getObjects().property(HttpClientsFactory.class);
    }
}
