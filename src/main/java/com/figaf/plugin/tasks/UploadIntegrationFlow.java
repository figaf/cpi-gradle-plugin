package com.figaf.plugin.tasks;

import com.figaf.plugin.entities.CreateIFlowRequest;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.gradle.api.tasks.Input;
import org.zeroturnaround.zip.ZipUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Arsenii Istlentev
 */
@Slf4j
@Setter
public class UploadIntegrationFlow extends AbstractIntegrationFlowTask {

    private final Pattern integrationFlowDisplayedNamePattern = Pattern.compile("Bundle-Name:\\s*(.*)");
    private final Pattern integrationFlowDescriptionPattern = Pattern.compile("description=(.*)");

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

            String integrationFlowDisplayedName = retrieveDisplayedName(directoryWithExcludedFiles);
            if (integrationFlowDisplayedName == null) {
                log.error("integrationFlowDisplayedName is null, integrationFlowTechnicalName will be used instead");
                integrationFlowDisplayedName = integrationFlowTechnicalName;
            }
            String integrationFlowDescription = retrieveDescription(directoryWithExcludedFiles);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ZipUtil.pack(directoryWithExcludedFiles, bos);
            bos.close();
            byte[] bundledModel = bos.toByteArray();

            CreateIFlowRequest uploadIFlowRequest = new CreateIFlowRequest();
            uploadIFlowRequest.setId(integrationFlowExternalId);
            uploadIFlowRequest.setDisplayedName(integrationFlowDisplayedName);
            uploadIFlowRequest.setDescription(integrationFlowDescription);

            cpiClient.uploadIntegrationFlow(cpiConnectionProperties, packageExternalId, integrationFlowExternalId, uploadIFlowRequest, bundledModel, uploadDraftVersion);
        } finally {
            FileUtils.deleteDirectory(directoryWithExcludedFiles);
        }
    }

    private String retrieveDisplayedName(File directoryWithExcludedFiles) {
        String integrationFlowDisplayedName = null;
        try {
            Path manifestFilePath = Paths.get(directoryWithExcludedFiles.getPath(), "META-INF/MANIFEST.MF");
            File manifestFile = manifestFilePath.toFile();
            String manifestFileContent = FileUtils.readFileToString(manifestFile, StandardCharsets.UTF_8);
            Matcher displayedNameMatcher = integrationFlowDisplayedNamePattern.matcher(manifestFileContent);
            if (displayedNameMatcher.find()) {
                integrationFlowDisplayedName = displayedNameMatcher.group(1);
            }
        } catch (Exception ex) {
            log.error("Cannot retrieve 'Bundle-Name' from MANIFEST.MF: ", ex);
        }
        return integrationFlowDisplayedName;
    }

    private String retrieveDescription(File directoryWithExcludedFiles) {
        String integrationFlowDescription = null;
        try {
            Path metainfoFilePath = Paths.get(directoryWithExcludedFiles.getPath(), "metainfo.prop");
            File metainfoFile = metainfoFilePath.toFile();
            String metainfoFileContent = FileUtils.readFileToString(metainfoFile, StandardCharsets.UTF_8);
            Matcher metainfoMatcher = integrationFlowDescriptionPattern.matcher(metainfoFileContent);
            if (metainfoMatcher.find()) {
                integrationFlowDescription = metainfoMatcher.group(1);
            }
        } catch (Exception ex) {
            log.error("Cannot retrieve 'description' from metainfo.prop: ", ex);
        }
        return integrationFlowDescription;
    }

}