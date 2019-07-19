package com.figaf.plugin.tasks;

import com.figaf.plugin.CpiClient;
import com.figaf.plugin.entities.Agent;
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
    protected String protocol = "https";

    @Input
    protected String host;

    @Input
    protected Integer port = 443;

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

    protected Agent agent;

    protected File sourceFolder;

    protected CpiClient cpiClient = new CpiClient();

    public void setProtocol(String protocol) {
        if (StringUtils.isNotEmpty(protocol)) {
            this.protocol = protocol;
        }
    }

    public void setPort(Integer port) {
        if (port != null && port != 0) {
            this.port = port;
        }
    }

    @TaskAction
    public void taskAction() {
        try {
            doTaskAction();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected abstract void doTaskAction() throws Exception;

    protected void defineParameters() {
        agent = new Agent(protocol, host, port, username, password);
        sourceFolder = new File(sourceFilePath);

        if (packageTechnicalName == null && integrationFlowTechnicalName == null) {
            packageTechnicalName = sourceFolder.getParentFile().getName();
            integrationFlowTechnicalName = sourceFolder.getName();
        }

        if (packageExternalId == null) {
            IntegrationPackage integrationPackage = cpiClient.getIntegrationPackageIfExists(agent, packageTechnicalName);
            packageExternalId = integrationPackage.getExternalId();
        }
        if (integrationFlowExternalId == null) {
            CpiIntegrationObjectData cpiIntegrationObjectData = cpiClient.getIFlowData(agent, packageTechnicalName, integrationFlowTechnicalName);
            integrationFlowExternalId = cpiIntegrationObjectData.getExternalId();
        }

        if (CollectionUtils.isEmpty(ignoreFilesList)) {
            ignoreFilesList = new HashSet<>();
        }
        ignoreFilesList.add("src/test");
        ignoreFilesList.add("build.gradle");

        System.out.println("agent = " + agent);
        System.out.println("packageTechnicalName = " + packageTechnicalName);
        System.out.println("packageExternalId = " + packageExternalId);
        System.out.println("integrationFlowTechnicalName = " + integrationFlowTechnicalName);
        System.out.println("integrationFlowExternalId = " + integrationFlowExternalId);
        System.out.println("ignoreFilesList = " + ignoreFilesList);
    }
}
