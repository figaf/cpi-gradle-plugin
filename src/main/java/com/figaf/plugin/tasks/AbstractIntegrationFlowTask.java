package com.figaf.plugin.tasks;

import com.figaf.plugin.client.IntegrationFlowClient;
import com.figaf.plugin.client.IntegrationPackageClient;
import com.figaf.plugin.client.IntegrationRuntimeClient;
import com.figaf.plugin.entities.CpiConnectionProperties;
import com.figaf.plugin.entities.CpiIntegrationObjectData;
import com.figaf.plugin.entities.CpiPlatformType;
import com.figaf.plugin.entities.IntegrationPackage;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
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
    protected CpiPlatformType platformType;

    @Input
    protected String sourceFilePath;

    @Input
    protected String packageTechnicalName;

    @Input
    protected String integrationFlowTechnicalName;

    @Input
    protected Set<String> ignoreFilesList;

    protected String integrationFlowExternalId;

    protected String packageExternalId;

    protected CpiConnectionProperties cpiConnectionProperties;

    protected String deployedBundleVersion;

    protected File sourceFolder;

    protected final IntegrationPackageClient integrationPackageClient = new IntegrationPackageClient();
    protected final IntegrationFlowClient integrationFlowClient = new IntegrationFlowClient();
    protected final IntegrationRuntimeClient integrationRuntimeClient = new IntegrationRuntimeClient();

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

    protected void defineParameters(boolean checkObjectsExistence) {
        cpiConnectionProperties = new CpiConnectionProperties(url, username, password, platformType);
        System.out.println("cpiConnectionProperties = " + cpiConnectionProperties);
        sourceFolder = new File(sourceFilePath);

        if (packageTechnicalName == null && integrationFlowTechnicalName == null) {
            packageTechnicalName = sourceFolder.getParentFile().getName();
            integrationFlowTechnicalName = sourceFolder.getName();
        }

        IntegrationPackage integrationPackage = integrationPackageClient.getIntegrationPackageIfExists(cpiConnectionProperties, packageTechnicalName);

        if (integrationPackage != null) {
            packageExternalId = integrationPackage.getExternalId();
        } else if (checkObjectsExistence) {
            throw new RuntimeException(String.format("Cannot find package with name %s", packageTechnicalName));
        }

        // if packageExternalId == null then package doesn't exist and hence iFlow doesn't exist
        if (packageExternalId != null) {
            CpiIntegrationObjectData cpiIntegrationObjectData = integrationFlowClient.getIFlowData(cpiConnectionProperties, packageTechnicalName, integrationFlowTechnicalName);

            if (cpiIntegrationObjectData != null) {
                integrationFlowExternalId = cpiIntegrationObjectData.getExternalId();
                deployedBundleVersion = cpiIntegrationObjectData.getVersion();
            } else if (checkObjectsExistence) {
                throw new RuntimeException(String.format("Cannot find iflow with name %s in package %s", integrationFlowTechnicalName, packageTechnicalName));
            }
        }

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
