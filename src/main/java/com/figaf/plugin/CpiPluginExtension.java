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

    private final Property<String> loginPageUrl;

    private final Property<String> ssoUrl;

    private final Property<Boolean> useCustomIdp;

    private final Property<String> samlUrl;

    private final Property<String> idpName;

    private final Property<String> idpApiClientId;

    private final Property<String> idpApiClientSecret;

    private final Property<String> oauthTokenUrl;

    private final Property<String> authenticationType;

    private final Property<String> publicApiUrl;

    private final Property<String> publicApiClientId;

    private final Property<String> publicApiClientSecret;

    private final Property<String> packageTechnicalName;

    private final Property<String> packageExternalId;

    private final Property<String> artifactTechnicalName;

    private final Property<String> artifactExternalId;

    private final Property<Boolean> waitForStartup;

    private final SetProperty<String> ignoreFilesList;

    private final Property<Boolean> uploadDraftVersion;

    private final Property<String> artifactType;

    private final Property<HttpClientsFactory> httpClientsFactory;

    private final Property<Boolean> useSeparateFolderForEachArtifactType;

    public CpiPluginExtension(Project project) {
        this.url = project.getObjects().property(String.class);
        this.username = project.getObjects().property(String.class);
        this.password = project.getObjects().property(String.class);
        this.platformType = project.getObjects().property(String.class);
        this.sourceFilePath = project.getObjects().property(String.class);
        this.loginPageUrl = project.getObjects().property(String.class);
        this.ssoUrl = project.getObjects().property(String.class);
        this.useCustomIdp = project.getObjects().property(Boolean.class);
        this.samlUrl = project.getObjects().property(String.class);
        this.idpName = project.getObjects().property(String.class);
        this.idpApiClientId = project.getObjects().property(String.class);
        this.idpApiClientSecret = project.getObjects().property(String.class);
        this.oauthTokenUrl = project.getObjects().property(String.class);
        this.authenticationType = project.getObjects().property(String.class);
        this.publicApiUrl = project.getObjects().property(String.class);
        this.publicApiClientId = project.getObjects().property(String.class);
        this.publicApiClientSecret = project.getObjects().property(String.class);
        this.packageTechnicalName = project.getObjects().property(String.class);
        this.packageExternalId = project.getObjects().property(String.class);
        this.artifactTechnicalName = project.getObjects().property(String.class);
        this.artifactExternalId = project.getObjects().property(String.class);
        this.waitForStartup = project.getObjects().property(Boolean.class);
        this.ignoreFilesList = project.getObjects().setProperty(String.class);
        this.uploadDraftVersion = project.getObjects().property(Boolean.class);
        this.artifactType = project.getObjects().property(String.class);
        this.httpClientsFactory = project.getObjects().property(HttpClientsFactory.class);
        this.useSeparateFolderForEachArtifactType = project.getObjects().property(Boolean.class);
    }
}
