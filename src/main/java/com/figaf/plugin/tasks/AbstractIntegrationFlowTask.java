package com.figaf.plugin.tasks;

import com.figaf.plugin.CpiClient;
import com.figaf.plugin.entities.CpiConnectionProperties;
import com.figaf.plugin.entities.CpiIntegrationObjectData;
import com.figaf.plugin.entities.IntegrationPackage;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Arsenii Istlentev
 */
@Setter
public abstract class AbstractIntegrationFlowTask extends DefaultTask {

    @Input
    protected String url;

    @Input
    protected String username;

    @Input
    protected String password;

    @Input
    protected String sourceFilePath;

    @Input
    protected String packageTechnicalName;

    @Input
    protected String packageExternalId;

    @Input
    protected String integrationFlowTechnicalName;

    @Input
    protected String integrationFlowExternalId;

    @Input
    protected Set<String> ignoreFilesList;

    protected CpiConnectionProperties cpiConnectionProperties;

    protected String deployedBundleVersion;

    protected File sourceFolder;

    protected CpiClient cpiClient = new CpiClient();

    @TaskAction
    public void taskAction() {
        try {
            doTaskAction();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    protected abstract void doTaskAction() throws Exception;

    protected void defineParameters() {
        cpiConnectionProperties = new CpiConnectionProperties(url, username, password);
        System.out.println("cpiConnectionProperties = " + cpiConnectionProperties);
        sourceFolder = new File(sourceFilePath);

        if (packageTechnicalName == null && integrationFlowTechnicalName == null) {
            packageTechnicalName = sourceFolder.getParentFile().getName();
            integrationFlowTechnicalName = sourceFolder.getName();
        }

        if (packageExternalId == null) {
            IntegrationPackage integrationPackage = cpiClient.getIntegrationPackageIfExists(cpiConnectionProperties, packageTechnicalName);
            packageExternalId = integrationPackage.getExternalId();
        }

        CpiIntegrationObjectData cpiIntegrationObjectData = cpiClient.getIFlowData(cpiConnectionProperties, packageTechnicalName, integrationFlowTechnicalName);

        if (integrationFlowExternalId == null) {
            integrationFlowExternalId = cpiIntegrationObjectData.getExternalId();
        }

        deployedBundleVersion = cpiIntegrationObjectData.getVersion();

        if (CollectionUtils.isEmpty(ignoreFilesList)) {
            ignoreFilesList = new HashSet<>();
        }
        ignoreFilesList.add("src/test");
        ignoreFilesList.add("build.gradle");
        ignoreFilesList.add("gradle.properties");
        ignoreFilesList.add("settings.gradle");

        System.out.println("packageTechnicalName = " + packageTechnicalName);
        System.out.println("packageExternalId = " + packageExternalId);
        System.out.println("integrationFlowTechnicalName = " + integrationFlowTechnicalName);
        System.out.println("integrationFlowExternalId = " + integrationFlowExternalId);
        System.out.println("deployedBundleVersion = " + deployedBundleVersion);
        System.out.println("ignoreFilesList = " + ignoreFilesList);
    }
}
