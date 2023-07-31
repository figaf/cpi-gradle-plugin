package com.figaf.plugin;

import com.figaf.integration.common.entity.AuthenticationType;
import com.figaf.integration.common.entity.CloudPlatformType;
import com.figaf.integration.common.factory.HttpClientsFactory;
import com.figaf.integration.cpi.entity.designtime_artifacts.CpiArtifactType;
import com.figaf.plugin.tasks.AbstractArtifactTask;
import com.figaf.plugin.tasks.DeployArtifact;
import com.figaf.plugin.tasks.DownloadArtifact;
import com.figaf.plugin.tasks.UploadArtifact;
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

    private void applyExtension(AbstractArtifactTask abstractArtifactTask, CpiPluginExtension extension, String taskDescription) {
        try {
            abstractArtifactTask.setGroup("cpi-plugin");
            abstractArtifactTask.setDescription(taskDescription);
            abstractArtifactTask.setUrl(extension.getUrl().getOrNull());
            abstractArtifactTask.setUsername(extension.getUsername().getOrNull());
            abstractArtifactTask.setPassword(extension.getPassword().getOrNull());
            String cpiPlatformTypeString = extension.getPlatformType().getOrNull();
            if (StringUtils.isNotEmpty(cpiPlatformTypeString)) {
                abstractArtifactTask.setPlatformType(CloudPlatformType.valueOf(cpiPlatformTypeString));
            } else {
                abstractArtifactTask.setPlatformType(CloudPlatformType.NEO);
            }
            abstractArtifactTask.setSourceFilePath(extension.getSourceFilePath().getOrNull());

            abstractArtifactTask.setLoginPageUrl(extension.getLoginPageUrl().getOrNull());
            abstractArtifactTask.setSsoUrl(extension.getSsoUrl().getOrNull());
            abstractArtifactTask.setUseCustomIdp(extension.getUseCustomIdp().getOrElse(false));
            abstractArtifactTask.setSamlUrl(extension.getSamlUrl().getOrNull());
            abstractArtifactTask.setFigafAgentId(extension.getFigafAgentId().getOrNull());
            abstractArtifactTask.setIdpName(extension.getIdpName().getOrNull());
            abstractArtifactTask.setIdpApiClientId(extension.getIdpApiClientId().getOrNull());
            abstractArtifactTask.setIdpApiClientSecret(extension.getIdpApiClientSecret().getOrNull());
            abstractArtifactTask.setOauthTokenUrl(extension.getOauthTokenUrl().getOrNull());
            String authenticationTypeString = extension.getAuthenticationType().getOrNull();
            if (StringUtils.isNotEmpty(authenticationTypeString)) {
                abstractArtifactTask.setAuthenticationType(AuthenticationType.valueOf(authenticationTypeString));
            } else {
                abstractArtifactTask.setAuthenticationType(AuthenticationType.BASIC);
            }
            abstractArtifactTask.setPublicApiUrl(extension.getPublicApiUrl().getOrNull());
            abstractArtifactTask.setPublicApiClientId(extension.getPublicApiClientId().getOrNull());
            abstractArtifactTask.setPublicApiClientSecret(extension.getPublicApiClientSecret().getOrNull());

            abstractArtifactTask.setPackageTechnicalName(extension.getPackageTechnicalName().getOrNull());
            abstractArtifactTask.setPackageExternalId(extension.getPackageExternalId().getOrNull());
            abstractArtifactTask.setArtifactTechnicalName(extension.getArtifactTechnicalName().getOrNull());
            abstractArtifactTask.setArtifactExternalId(extension.getArtifactExternalId().getOrNull());
            SetProperty<String> ignoreFilesListProperty = extension.getIgnoreFilesList();
            Set<String> ignoreFilesList = new HashSet<>();
            if (ignoreFilesListProperty != null && ignoreFilesListProperty.isPresent()) {
                ignoreFilesList.addAll(ignoreFilesListProperty.get());
            }
            abstractArtifactTask.setIgnoreFilesList(ignoreFilesList);
            abstractArtifactTask.setArtifactType(CpiArtifactType.valueOf(extension.getArtifactType().getOrNull()));
            abstractArtifactTask.setHttpClientsFactory(extension.getHttpClientsFactory().getOrElse(new HttpClientsFactory()));
            abstractArtifactTask.setUseSeparateFolderForEachArtifactType(extension.getUseSeparateFolderForEachArtifactType().getOrElse(false));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

}