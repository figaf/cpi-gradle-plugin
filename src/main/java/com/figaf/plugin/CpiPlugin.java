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

    private void applyExtension(AbstractArtifactTask abstractIntegrationFlowTask, CpiPluginExtension extension, String taskDescription) {
        try {
            abstractIntegrationFlowTask.setGroup("cpi-plugin");
            abstractIntegrationFlowTask.setDescription(taskDescription);
            abstractIntegrationFlowTask.setUrl(extension.getUrl().getOrNull());
            abstractIntegrationFlowTask.setUsername(extension.getUsername().getOrNull());
            abstractIntegrationFlowTask.setPassword(extension.getPassword().getOrNull());
            String cpiPlatformTypeString = extension.getPlatformType().getOrNull();
            if (StringUtils.isNotEmpty(cpiPlatformTypeString)) {
                abstractIntegrationFlowTask.setPlatformType(CloudPlatformType.valueOf(cpiPlatformTypeString));
            } else {
                abstractIntegrationFlowTask.setPlatformType(CloudPlatformType.NEO);
            }
            abstractIntegrationFlowTask.setSourceFilePath(extension.getSourceFilePath().getOrNull());

            abstractIntegrationFlowTask.setLoginPageUrl(extension.getLoginPageUrl().getOrNull());
            abstractIntegrationFlowTask.setSsoUrl(extension.getSsoUrl().getOrNull());
            abstractIntegrationFlowTask.setOauthTokenUrl(extension.getOauthTokenUrl().getOrNull());
            String authenticationTypeString = extension.getAuthenticationType().getOrNull();
            if (StringUtils.isNotEmpty(authenticationTypeString)) {
                abstractIntegrationFlowTask.setAuthenticationType(AuthenticationType.valueOf(authenticationTypeString));
            } else {
                abstractIntegrationFlowTask.setAuthenticationType(AuthenticationType.BASIC);
            }
            abstractIntegrationFlowTask.setClientId(extension.getClientId().getOrNull());
            abstractIntegrationFlowTask.setClientSecret(extension.getClientSecret().getOrNull());

            abstractIntegrationFlowTask.setPackageTechnicalName(extension.getPackageTechnicalName().getOrNull());
            abstractIntegrationFlowTask.setPackageExternalId(extension.getPackageExternalId().getOrNull());
            abstractIntegrationFlowTask.setArtifactTechnicalName(extension.getArtifactTechnicalName().getOrNull());
            abstractIntegrationFlowTask.setArtifactExternalId(extension.getArtifactExternalId().getOrNull());
            SetProperty<String> ignoreFilesListProperty = extension.getIgnoreFilesList();
            Set<String> ignoreFilesList = new HashSet<>();
            if (ignoreFilesListProperty != null && ignoreFilesListProperty.isPresent()) {
                ignoreFilesList.addAll(ignoreFilesListProperty.get());
            }
            abstractIntegrationFlowTask.setIgnoreFilesList(ignoreFilesList);
            abstractIntegrationFlowTask.setArtifactType(ArtifactType.valueOf(extension.getArtifactType().getOrNull()));
            abstractIntegrationFlowTask.setHttpClientsFactory(extension.getHttpClientsFactory().getOrElse(new HttpClientsFactory()));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

}