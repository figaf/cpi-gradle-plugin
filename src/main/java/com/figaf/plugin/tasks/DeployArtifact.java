package com.figaf.plugin.tasks;

import com.figaf.integration.cpi.entity.runtime_artifacts.IntegrationContent;
import com.figaf.integration.cpi.entity.runtime_artifacts.IntegrationContentErrorInformation;
import com.figaf.integration.cpi.entity.runtime_artifacts.RuntimeArtifactIdentifier;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.tasks.Input;

/**
 * @author Arsenii Istlentev
 */
@Setter
public class DeployArtifact extends AbstractArtifactTask {

    private static final int MAX_NUMBER_OF_ATTEMPTS = 36;
    private static final long SLEEP_TIME = 5000L;

    @Getter
    @Input
    private Boolean waitForStartup = false;

    public void doTaskAction() throws Exception {
        System.out.println("deployArtifact");
        defineParameters(true);
        String taskId = deployArtifact();

        if (waitForStartup == null || !waitForStartup) {
            return;
        }
        int numberOfAttempts = 1;
        boolean deployStatusSuccess = false;
        while (!deployStatusSuccess && numberOfAttempts <= MAX_NUMBER_OF_ATTEMPTS) {
            String deployStatus = getDeployStatus(taskId);
            System.out.println("deployStatus = " + deployStatus);
            if ("SUCCESS".equals(deployStatus)) {
                deployStatusSuccess = true;
            } else if ("DEPLOYING".equals(deployStatus)){
                numberOfAttempts++;
                Thread.sleep(SLEEP_TIME);
            } else {
                throw new RuntimeException(String.format("Deployment of %s has failed", artifactTechnicalName));
            }
        }

        RuntimeArtifactIdentifier runtimeArtifactIdentifier = RuntimeArtifactIdentifier.builder()
            .technicalName(artifactTechnicalName)
            .build();

        boolean started = false;
        while (!started && numberOfAttempts <= MAX_NUMBER_OF_ATTEMPTS) {
            IntegrationContent integrationContent;
            try {
                integrationContent = integrationContentClient.getIntegrationRuntimeArtifact(requestContext, runtimeArtifactIdentifier);
            } catch (Exception ex) {
                System.err.println("Check Public API Client Credentials. If you are sure that they are correct, try to execute 'gradlew --stop' to clean gradle cache and rerun the task");
                throw ex;
            }
            String integrationRuntimeArtifactStatus = integrationContent.getStatus();
            System.out.println("integrationRuntimeArtifactStatus = " + integrationRuntimeArtifactStatus);
            if ("ERROR".equals(integrationRuntimeArtifactStatus)) {
                IntegrationContentErrorInformation integrationContentErrorInformation = null;
                try {
                    integrationContentErrorInformation = integrationContentClient.getIntegrationRuntimeArtifactErrorInformation(requestContext, integrationContent);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (integrationContentErrorInformation != null && StringUtils.isNotEmpty(integrationContentErrorInformation.getErrorMessage())) {
                    throw new RuntimeException(String.format("Deployment of %s has failed. Error messages: %s", artifactTechnicalName, integrationContentErrorInformation.getErrorMessage()));
                } else {
                    throw new RuntimeException(String.format("Deployment of %s has failed", artifactTechnicalName));
                }
            } else if (!"STARTED".equals(integrationRuntimeArtifactStatus)) {
                numberOfAttempts++;
                Thread.sleep(SLEEP_TIME);
            } else {
                started = true;
            }
        }
        if (!started) {
            throw new RuntimeException(String.format("Deployment of %s hasn't been started successfully within %d ms", artifactTechnicalName, MAX_NUMBER_OF_ATTEMPTS * SLEEP_TIME));
        }
    }

    private String deployArtifact() {
        String taskId = null;

        switch (artifactType) {
            case IFLOW:
                taskId = cpiIntegrationFlowClient.deployIFlow(
                    requestContext,
                    packageExternalId,
                    artifactExternalId,
                    artifactTechnicalName
                );
                break;
            case VALUE_MAPPING:
                taskId = cpiValueMappingClient.deployValueMapping(
                    requestContext,
                    packageExternalId,
                    artifactExternalId
                );
                break;
            case SCRIPT_COLLECTION:
                taskId = cpiScriptCollectionClient.deployScriptCollection(
                    requestContext,
                    packageExternalId,
                    artifactExternalId,
                    artifactTechnicalName
                );
                break;
            case MESSAGE_MAPPING:
                taskId = cpiMessageMappingClient.deployMessageMapping(
                    requestContext,
                    packageExternalId,
                    artifactExternalId
                );
                break;
            case FUNCTION_LIBRARIES:
                taskId = cpiFunctionLibrariesClient.deployFunctionLibraries(
                    requestContext,
                    packageExternalId,
                    artifactExternalId,
                    artifactTechnicalName
                );
                break;
        }

        return taskId;
    }

    private String getDeployStatus(String taskId) {
        return cpiRuntimeArtifactClient.checkDeploymentStatus(requestContext, taskId);
    }
}
