package com.figaf.plugin.tasks;

import com.figaf.integration.common.entity.*;
import com.figaf.integration.common.factory.HttpClientsFactory;
import com.figaf.integration.cpi.client.CpiIntegrationFlowClient;
import com.figaf.integration.cpi.client.IntegrationContentClient;
import com.figaf.integration.cpi.client.IntegrationPackageClient;
import com.figaf.integration.cpi.entity.designtime_artifacts.CpiArtifact;
import com.figaf.integration.cpi.entity.designtime_artifacts.IntegrationPackage;
import com.figaf.plugin.enumeration.ArtifactType;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.*;

/**
 * @author Arsenii Istlentev
 */
@Setter
public abstract class AbstractArtifactTask extends DefaultTask {

    @Input
    protected String url;

    @Input
    protected String username;

    @Input
    protected String password;

    @Input
    protected CloudPlatformType platformType;

    @Input
    protected String loginPageUrl;

    @Input
    protected String ssoUrl;

    @Input
    protected String oauthTokenUrl;

    @Input
    protected AuthenticationType authenticationType;

    @Input
    protected String clientId;

    @Input
    protected String clientSecret;

    @Input
    protected String sourceFilePath;

    @Input
    protected String packageTechnicalName;

    @Input
    protected String artifactTechnicalName;

    @Input
    protected Set<String> ignoreFilesList;

    @Input
    protected ArtifactType artifactType;

    @Input
    protected HttpClientsFactory httpClientsFactory;

    protected String artifactExternalId;

    protected String packageExternalId;

    protected ConnectionProperties cpiConnectionProperties;

    protected RequestContext requestContext;

    protected String deployedBundleVersion;

    protected File sourceFolder;

    protected IntegrationPackageClient integrationPackageClient;

    protected CpiIntegrationFlowClient cpiIntegrationFlowClient;

    protected IntegrationContentClient integrationContentClient;

    public AbstractArtifactTask() {
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
        System.out.println("httpClientsFactory = " + httpClientsFactory);

        this.integrationPackageClient = new IntegrationPackageClient(httpClientsFactory);
        this.cpiIntegrationFlowClient = new CpiIntegrationFlowClient(integrationPackageClient, httpClientsFactory);
        this.integrationContentClient = new IntegrationContentClient(httpClientsFactory);

        requestContext = new RequestContext();
        requestContext.setCloudPlatformType(platformType);
        requestContext.setConnectionProperties(cpiConnectionProperties);
        requestContext.setPlatform(Platform.CPI);
        requestContext.setRestTemplateWrapperKey("");
        requestContext.setLoginPageUrl(loginPageUrl);
        requestContext.setSsoUrl(ssoUrl);
        requestContext.setOauthUrl(oauthTokenUrl);
        requestContext.setAuthenticationType(authenticationType);
        requestContext.setClientId(clientId);
        requestContext.setClientSecret(clientSecret);

        sourceFolder = new File(sourceFilePath);

        if (packageTechnicalName == null && artifactTechnicalName == null) {
            packageTechnicalName = sourceFolder.getParentFile().getName();
            artifactTechnicalName = sourceFolder.getName();
        }

        IntegrationPackage integrationPackage = getIntegrationPackageIfExists(requestContext, packageTechnicalName);

        if (integrationPackage != null) {
            packageExternalId = integrationPackage.getExternalId();
        } else if (checkObjectsExistence) {
            throw new RuntimeException(String.format("Cannot find package with name %s", packageTechnicalName));
        }

        // if packageExternalId == null then package doesn't exist and hence iFlow/Value Mapping doesn't exist
        if (packageExternalId != null) {
            CpiArtifact cpiIntegrationObjectData = getArtifactData(
                requestContext,
                packageTechnicalName,
                artifactTechnicalName,
                this.artifactType.toString()
            );

            if (cpiIntegrationObjectData != null) {
                artifactExternalId = cpiIntegrationObjectData.getExternalId();
                deployedBundleVersion = cpiIntegrationObjectData.getVersion();
            } else if (checkObjectsExistence) {
                throw new RuntimeException(String.format("Cannot find artifact with name %s in package %s", artifactTechnicalName, packageTechnicalName));
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
        System.out.println("artifactTechnicalName = " + artifactTechnicalName);
        System.out.println("artifactExternalId = " + artifactExternalId);
        System.out.println("deployedBundleVersion = " + deployedBundleVersion);
        System.out.println("ignoreFilesList = " + ignoreFilesList);

        System.out.println("loginPageUrl = " + loginPageUrl);
        System.out.println("ssoUrl = " + ssoUrl);
        System.out.println("oauthTokenUrl = " + oauthTokenUrl);
        System.out.println("authenticationType = " + authenticationType);
        System.out.println("clientId = " + clientId);
    }


    private CpiArtifact getArtifactData(
        RequestContext requestContext,
        String packageTechnicalName,
        String artifactTechnicalName,
        String artifactType
    ) {

        List<CpiArtifact> artifactsInThePackage = cpiIntegrationFlowClient.getArtifactsByPackage(
            requestContext,
            packageTechnicalName,
            null,
            null,
            Collections.singleton(artifactType)
        );

        CpiArtifact artifactCpiIntegrationObjectData = null;

        for (CpiArtifact artifact : artifactsInThePackage) {
            if (artifact.getTechnicalName().equals(artifactTechnicalName)) {
                artifactCpiIntegrationObjectData = artifact;
                break;
            }
        }

        return artifactCpiIntegrationObjectData;
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
