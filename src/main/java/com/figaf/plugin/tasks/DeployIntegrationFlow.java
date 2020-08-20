package com.figaf.plugin.tasks;

import com.figaf.plugin.entities.IntegrationContent;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.tasks.Input;

import java.util.List;

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
        String taskId = cpiClient.deployIFlow(cpiConnectionProperties, packageExternalId, integrationFlowExternalId, integrationFlowTechnicalName);
        if (waitForStartup == null || !waitForStartup) {
            return;
        }
        int numberOfAttempts = 1;
        boolean deployStatusSuccess = false;
        while (!deployStatusSuccess && numberOfAttempts <= MAX_NUMBER_OF_ATTEMPTS) {
            String deployStatus = cpiClient.checkDeployStatus(cpiConnectionProperties, taskId);
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
            IntegrationContent integrationContent = cpiClient.getIntegrationRuntimeArtifactByName(cpiConnectionProperties, integrationFlowTechnicalName);
            String integrationRuntimeArtifactStatus = integrationContent.getStatus();
            System.out.println("integrationRuntimeArtifactStatus = " + integrationRuntimeArtifactStatus);
            if ("ERROR".equals(integrationRuntimeArtifactStatus)) {
                List<String> errorMessages = null;
                try {
                    errorMessages = cpiClient.getIntegrationRuntimeErrorInformation(cpiConnectionProperties, integrationFlowTechnicalName);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (errorMessages != null) {
                    throw new RuntimeException(String.format("Deployment of IFlow %s has failed. Error messages: %s", integrationFlowTechnicalName, StringUtils.join(errorMessages,", ")));
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
