package com.figaf.plugin.tasks;

import com.figaf.integration.cpi.entity.runtime_artifacts.IntegrationContent;
import com.figaf.integration.cpi.entity.runtime_artifacts.IntegrationContentErrorInformation;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.tasks.Input;

/**
 * @author Arsenii Istlentev
 */
@Setter
public class DeployIntegrationFlow extends AbstractIntegrationFlowTask {

    private static final int MAX_NUMBER_OF_ATTEMPTS = 36;
    private static final long SLEEP_TIME = 5000L;

    @Input
    private Boolean waitForStartup = false;

    public void doTaskAction() throws Exception {
        System.out.println("deployIntegrationFlow");
        defineParameters(true);
        String taskId = cpiIntegrationFlowClient.deployIFlow(requestContext, packageExternalId, integrationFlowExternalId, integrationFlowTechnicalName);
        if (waitForStartup == null || !waitForStartup) {
            return;
        }
        int numberOfAttempts = 1;
        boolean deployStatusSuccess = false;
        while (!deployStatusSuccess && numberOfAttempts <= MAX_NUMBER_OF_ATTEMPTS) {
            String deployStatus = cpiIntegrationFlowClient.checkDeployStatus(requestContext, taskId);
            System.out.println("deployStatus = " + deployStatus);
            if ("SUCCESS".equals(deployStatus)) {
                deployStatusSuccess = true;
            } else if ("DEPLOYING".equals(deployStatus)){
                numberOfAttempts++;
                Thread.sleep(SLEEP_TIME);
            } else {
                throw new RuntimeException(String.format("Deployment of IFlow %s has failed", integrationFlowTechnicalName));
            }
        }
        boolean started = false;
        while (!started && numberOfAttempts <= MAX_NUMBER_OF_ATTEMPTS) {
            IntegrationContent integrationContent = integrationContentClient.getIntegrationRuntimeArtifactByName(requestContext, integrationFlowTechnicalName);
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
                    throw new RuntimeException(String.format("Deployment of IFlow %s has failed. Error messages: %s", integrationFlowTechnicalName, integrationContentErrorInformation.getErrorMessage()));
                } else {
                    throw new RuntimeException(String.format("Deployment of IFlow %s has failed", integrationFlowTechnicalName));
                }
            } else if (!"STARTED".equals(integrationRuntimeArtifactStatus)) {
                numberOfAttempts++;
                Thread.sleep(SLEEP_TIME);
            } else {
                started = true;
            }
        }
        if (!started) {
            throw new RuntimeException(String.format("Deployment of IFlow %s hasn't been started successfully within %d ms", integrationFlowTechnicalName, MAX_NUMBER_OF_ATTEMPTS * SLEEP_TIME));
        }
    }
}
