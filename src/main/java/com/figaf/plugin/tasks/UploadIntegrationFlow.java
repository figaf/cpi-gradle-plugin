package com.figaf.plugin.tasks;

import com.figaf.plugin.entities.CreateIFlowRequest;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.gradle.api.tasks.Input;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.jar.Manifest;

/**
 * @author Arsenii Istlentev
 */
@Slf4j
@Setter
public class UploadIntegrationFlow extends AbstractIntegrationFlowTask {

    @Input
    private Boolean uploadDraftVersion;

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

            Manifest manifest = parseManifestFile(directoryWithExcludedFiles);
            String integrationFlowDisplayedName = manifest.getMainAttributes().getValue("Bundle-Name");
            if (integrationFlowDisplayedName == null) {
                log.error("integrationFlowDisplayedName is null, integrationFlowTechnicalName will be used instead");
                integrationFlowDisplayedName = integrationFlowTechnicalName;
            }

            Properties properties = getMetainfoProperties(directoryWithExcludedFiles);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ZipUtil.pack(directoryWithExcludedFiles, bos);
            bos.close();
            byte[] bundledModel = bos.toByteArray();

            CreateIFlowRequest uploadIFlowRequest = new CreateIFlowRequest();
            uploadIFlowRequest.setId(integrationFlowExternalId);
            uploadIFlowRequest.setDisplayedName(integrationFlowDisplayedName);
            uploadIFlowRequest.setDescription(properties.getProperty("description"));

            CreateIFlowRequest.AdditionalAttributes additionalAttributes = new CreateIFlowRequest.AdditionalAttributes();
            String sourceValue = properties.getProperty("source");
            if (sourceValue != null) {
                additionalAttributes.getSource().add(sourceValue);
            }
            String targetValue = properties.getProperty("target");
            if (targetValue != null) {
                additionalAttributes.getTarget().add(targetValue);
            }
            uploadIFlowRequest.setAdditionalAttrs(additionalAttributes);

            cpiClient.uploadIntegrationFlow(
                cpiConnectionProperties,
                packageExternalId,
                integrationFlowExternalId,
                uploadIFlowRequest,
                bundledModel,
                uploadDraftVersion,
                manifest.getMainAttributes().getValue("Bundle-Version")
            );
        } finally {
            FileUtils.deleteDirectory(directoryWithExcludedFiles);
        }
    }

    private Manifest parseManifestFile(File directoryWithExcludedFiles) {
        Path manifestFilePath = Paths.get(directoryWithExcludedFiles.getPath(), "META-INF/MANIFEST.MF");
        try (FileInputStream fis = new FileInputStream(manifestFilePath.toFile())) {
            return new Manifest(fis);
        } catch (Exception ex) {
            log.error("Cannot retrieve 'Bundle-Name' from MANIFEST.MF: ", ex);
            throw new IllegalStateException("Cannot retrieve 'Bundle-Name' from MANIFEST.MF", ex);
        }
    }

    private Properties getMetainfoProperties(File directoryWithExcludedFiles) throws IOException {
        InputStream inputStream = null;
        try {
            Path metainfoFilePath = Paths.get(directoryWithExcludedFiles.getPath(), "metainfo.prop");
            inputStream = new FileInputStream(metainfoFilePath.toFile());
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Cannot read values from metainfo.prop: ", ex);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

}