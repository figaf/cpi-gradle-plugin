package com.figaf.plugin;

import com.figaf.integration.common.entity.AuthenticationType;
import com.figaf.integration.common.entity.CloudPlatformType;
import com.figaf.integration.common.factory.HttpClientsFactory;
import com.figaf.plugin.enumeration.ArtifactType;
import com.figaf.plugin.tasks.*;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.SetProperty;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Arsenii Istlentev
 */
public class CpiPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {

        CpiPluginExtension extension = project.getExtensions().create("cpiPlugin", CpiPluginExtension.class, project);

        project.getTasks().register("uploadArtifact", UploadArtifact.class, uploadArtifact -> {
            applyExtension(uploadArtifact, extension, "Builds bundled model of artifact and uploads it to CPI.");
            uploadArtifact.setUploadDraftVersion(extension.getUploadDraftVersion().getOrElse(false));
        });

        project.getTasks().register("deployArtifact", DeployArtifact.class, deployArtifact -> {
            applyExtension(deployArtifact, extension, "Deploys artifact on CPI. Usually it makes sense to run this task after 'uploadArtifact'.");
            deployArtifact.setWaitForStartup(extension.getWaitForStartup().getOrNull());
        });

        project.getTasks().register("downloadArtifact", DownloadArtifact.class, downloadArtifact ->
            applyExtension(downloadArtifact, extension, "Downloads artifact bundled model from CPI and unpacks it to module folder."));
    }

    private void applyExtension(AbstractArtifactTask abstractartifactTask, CpiPluginExtension extension, String taskDescription) {
        try {
            abstractartifactTask.setGroup("cpi-plugin");
            abstractartifactTask.setDescription(taskDescription);
            abstractartifactTask.setUrl(extension.getUrl().getOrNull());
            abstractartifactTask.setUsername(extension.getUsername().getOrNull());
            abstractartifactTask.setPassword(extension.getPassword().getOrNull());
            String cpiPlatformTypeString = extension.getPlatformType().getOrNull();
            if (StringUtils.isNotEmpty(cpiPlatformTypeString)) {
                abstractartifactTask.setPlatformType(CloudPlatformType.valueOf(cpiPlatformTypeString));
            } else {
                abstractartifactTask.setPlatformType(CloudPlatformType.NEO);
            }
            abstractartifactTask.setSourceFilePath(extension.getSourceFilePath().getOrNull());

            abstractartifactTask.setLoginPageUrl(extension.getLoginPageUrl().getOrNull());
            abstractartifactTask.setSsoUrl(extension.getSsoUrl().getOrNull());
            abstractartifactTask.setOauthTokenUrl(extension.getOauthTokenUrl().getOrNull());
            String authenticationTypeString = extension.getAuthenticationType().getOrNull();
            if (StringUtils.isNotEmpty(authenticationTypeString)) {
                abstractartifactTask.setAuthenticationType(AuthenticationType.valueOf(authenticationTypeString));
            } else {
                abstractartifactTask.setAuthenticationType(AuthenticationType.BASIC);
            }
            abstractartifactTask.setPublicApiClientId(extension.getPublicApiClientId().getOrNull());
            abstractartifactTask.setPublicApiClientSecret(extension.getPublicApiClientSecret().getOrNull());

            abstractartifactTask.setPackageTechnicalName(extension.getPackageTechnicalName().getOrNull());
            abstractartifactTask.setPackageExternalId(extension.getPackageExternalId().getOrNull());
            abstractartifactTask.setArtifactTechnicalName(extension.getArtifactTechnicalName().getOrNull());
            abstractartifactTask.setArtifactExternalId(extension.getArtifactExternalId().getOrNull());
            SetProperty<String> ignoreFilesListProperty = extension.getIgnoreFilesList();
            Set<String> ignoreFilesList = new HashSet<>();
            if (ignoreFilesListProperty != null && ignoreFilesListProperty.isPresent()) {
                ignoreFilesList.addAll(ignoreFilesListProperty.get());
            }
            abstractartifactTask.setIgnoreFilesList(ignoreFilesList);
            abstractartifactTask.setArtifactType(ArtifactType.valueOf(extension.getArtifactType().getOrNull()));
            abstractartifactTask.setHttpClientsFactory(extension.getHttpClientsFactory().getOrElse(new HttpClientsFactory()));
            abstractartifactTask.setDoSeparateByTypes(extension.getDoSeparateByTypes().getOrElse(false));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

}