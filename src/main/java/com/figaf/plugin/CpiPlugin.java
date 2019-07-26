package com.figaf.plugin;

import com.figaf.plugin.tasks.AbstractIntegrationFlowTask;
import com.figaf.plugin.tasks.DeployIntegrationFlow;
import com.figaf.plugin.tasks.DownloadIntegrationFlow;
import com.figaf.plugin.tasks.UploadIntegrationFlow;
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

        project.getTasks().register("uploadIntegrationFlow", UploadIntegrationFlow.class, uploadIntegrationFlow -> applyExtension(uploadIntegrationFlow, extension));

        project.getTasks().register("deployIntegrationFlow", DeployIntegrationFlow.class, deployIntegrationFlow -> {
            applyExtension(deployIntegrationFlow, extension);
            deployIntegrationFlow.setWaitForStartup(extension.getWaitForStartup().getOrNull());
        });

        project.getTasks().register("downloadIntegrationFlow", DownloadIntegrationFlow.class, downloadIntegrationFlow -> applyExtension(downloadIntegrationFlow, extension));

    }

    private void applyExtension(AbstractIntegrationFlowTask abstractIntegrationFlowTask, CpiPluginExtension extension) {
        try {
            abstractIntegrationFlowTask.setUrl(extension.getUrl().getOrNull());
            abstractIntegrationFlowTask.setUsername(extension.getUsername().getOrNull());
            abstractIntegrationFlowTask.setPassword(extension.getPassword().getOrNull());
            abstractIntegrationFlowTask.setSourceFilePath(extension.getSourceFilePath().getOrNull());
            abstractIntegrationFlowTask.setPackageTechnicalName(extension.getPackageTechnicalName().getOrNull());
            abstractIntegrationFlowTask.setPackageExternalId(extension.getPackageExternalId().getOrNull());
            abstractIntegrationFlowTask.setIntegrationFlowTechnicalName(extension.getIntegrationFlowTechnicalName().getOrNull());
            abstractIntegrationFlowTask.setIntegrationFlowExternalId(extension.getIntegrationFlowExternalId().getOrNull());
            SetProperty<String> ignoreFilesListProperty = extension.getIgnoreFilesList();
            Set<String> ignoreFilesList = new HashSet<>();
            if (ignoreFilesListProperty != null && ignoreFilesListProperty.isPresent()) {
                ignoreFilesList.addAll(ignoreFilesListProperty.get());
            }
            abstractIntegrationFlowTask.setIgnoreFilesList(ignoreFilesList);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

}