package com.figaf.plugin.tasks;

import com.figaf.integration.common.entity.CloudPlatformType;
import com.figaf.integration.common.entity.ConnectionProperties;
import com.figaf.integration.common.entity.Platform;
import com.figaf.integration.common.entity.RequestContext;
import com.figaf.integration.common.factory.HttpClientsFactory;
import com.figaf.integration.cpi.client.CpiIntegrationFlowClient;
import com.figaf.integration.cpi.client.IntegrationContentClient;
import com.figaf.integration.cpi.client.IntegrationPackageClient;
import com.figaf.integration.cpi.entity.designtime_artifacts.CpiArtifact;
import com.figaf.integration.cpi.entity.designtime_artifacts.IntegrationPackage;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Arsenii Istlentev
 */
@Setter
public abstract class AbstractIntegrationFlowTask extends DefaultTask {

    private final static String SSO_URL = "https://accounts.sap.com/saml2/idp/sso";

    @Input
    protected String url;

    @Input
    protected String username;

    @Input
    protected String password;

    @Input
    protected CloudPlatformType platformType;

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

    protected ConnectionProperties cpiConnectionProperties;

    protected RequestContext requestContext;

    protected String deployedBundleVersion;

    protected File sourceFolder;

    protected IntegrationPackageClient integrationPackageClient;

    protected CpiIntegrationFlowClient cpiIntegrationFlowClient;

    protected IntegrationContentClient integrationContentClient;

    public AbstractIntegrationFlowTask() {
        HttpClientsFactory httpClientsFactory = new HttpClientsFactory();
        this.integrationPackageClient = new IntegrationPackageClient(SSO_URL);
        this.cpiIntegrationFlowClient = new CpiIntegrationFlowClient(SSO_URL, integrationPackageClient);
        this.integrationContentClient = new IntegrationContentClient(SSO_URL, httpClientsFactory);
    }

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
        cpiConnectionProperties = new ConnectionProperties(url, username, password);
        System.out.println("cpiConnectionProperties = " + cpiConnectionProperties);

        requestContext = new RequestContext();
        requestContext.setCloudPlatformType(platformType);
        requestContext.setConnectionProperties(cpiConnectionProperties);
        requestContext.setPlatform(Platform.CPI);
        requestContext.setRestTemplateWrapperKey("");

        sourceFolder = new File(sourceFilePath);

        if (packageTechnicalName == null && integrationFlowTechnicalName == null) {
            packageTechnicalName = sourceFolder.getParentFile().getName();
            integrationFlowTechnicalName = sourceFolder.getName();
        }

        IntegrationPackage integrationPackage = getIntegrationPackageIfExists(requestContext, packageTechnicalName);

        if (integrationPackage != null) {
            packageExternalId = integrationPackage.getExternalId();
        } else if (checkObjectsExistence) {
            throw new RuntimeException(String.format("Cannot find package with name %s", packageTechnicalName));
        }

        // if packageExternalId == null then package doesn't exist and hence iFlow doesn't exist
        if (packageExternalId != null) {
            CpiArtifact cpiIntegrationObjectData = getIFlowData(requestContext, packageTechnicalName, integrationFlowTechnicalName);

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


    private CpiArtifact getIFlowData(
        RequestContext requestContext,
        String packageTechnicalName,
        String iFlowTechnicalName
    ) {

        List<CpiArtifact> integrationFlowsInThePackage = cpiIntegrationFlowClient.getArtifactsByPackage(
            requestContext,
            packageTechnicalName,
            null,
            null,
            Collections.singleton("CPI_IFLOW")
        );

        CpiArtifact iFlowCpiIntegrationObjectData = null;

        for (CpiArtifact iFlow : integrationFlowsInThePackage) {
            if (iFlow.getTechnicalName().equals(iFlowTechnicalName)) {
                iFlowCpiIntegrationObjectData = iFlow;
                break;
            }
        }

        return iFlowCpiIntegrationObjectData;
    }

    private IntegrationPackage getIntegrationPackageIfExists(
        RequestContext requestContext,
        String packageTechnicalName
    ) {
        List<IntegrationPackage> integrationPackagesSearchResult = integrationPackageClient.getIntegrationPackages(
            requestContext,
            String.format("TechnicalName eq '%s'", packageTechnicalName)
        );

        if (integrationPackagesSearchResult.size() == 1) {
            return integrationPackagesSearchResult.get(0);
        } else if (integrationPackagesSearchResult.size() > 1) {
            throw new RuntimeException(
                String.format(
                    "Unexpected state: %d integration packages were found by name %s.",
                    integrationPackagesSearchResult.size(),
                    packageTechnicalName
                )
            );
        } else {
            return null;
        }
    }

}
