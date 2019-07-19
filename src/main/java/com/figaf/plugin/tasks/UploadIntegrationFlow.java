package com.figaf.plugin.tasks;

import com.figaf.plugin.entities.CreateIFlowRequest;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Arsenii Istlentev
 */
@Slf4j
@Setter
public class UploadIntegrationFlow extends AbstractIntegrationFlowTask {

    public void doTaskAction() throws IOException {
        defineParameters();
        Path pathToDirectoryWithExcludedFiles = Files.createTempDirectory("cpi-plugin-upload-iflow-" + UUID.randomUUID().toString());
        File directoryWithExcludedFiles = pathToDirectoryWithExcludedFiles.toFile();
        try {
            List<Path> pathsToExclude = new ArrayList<>();
            for (String fileNameToExclude : ignoreFilesList) {
                pathsToExclude.add(Paths.get(sourceFilePath, fileNameToExclude));
            }

            FileUtils.copyDirectory(sourceFolder, directoryWithExcludedFiles, pathname -> {
                boolean accept = true;
                for (Path pathToExclude : pathsToExclude) {
                    if (pathname.toString().contains(pathToExclude.toString())) {
                        accept = false;
                        break;
                    }
                }
                return accept;
            });

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ZipUtil.pack(directoryWithExcludedFiles, bos);
            bos.close();
            byte[] bundledModel = bos.toByteArray();

            cpiClient.deleteIntegrationFlow(agent, packageExternalId, integrationFlowExternalId);

            CreateIFlowRequest uploadIFlowRequest = new CreateIFlowRequest();
            uploadIFlowRequest.setId(integrationFlowTechnicalName);
            uploadIFlowRequest.setName(integrationFlowTechnicalName);

            cpiClient.uploadIntegrationFlow(agent, packageExternalId, uploadIFlowRequest, bundledModel);
        } finally {
            FileUtils.deleteDirectory(directoryWithExcludedFiles);
        }
    }
}