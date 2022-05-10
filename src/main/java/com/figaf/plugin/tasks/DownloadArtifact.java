package com.figaf.plugin.tasks;

import com.figaf.plugin.enumeration.ArtifactType;
import com.figaf.plugin.utils.XMLUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author Arsenii Istlentev
 */
@Slf4j
@Setter
public class DownloadArtifact extends AbstractArtifactTask {

    public void doTaskAction() throws IOException {
        System.out.println("downloadArtifact");
        defineParameters(true);

        Path pathToArtifactZipArchive = Files.createTempFile(String.format("%s_%s", artifactType, artifactTechnicalName), ".zip");
        File artifactZipArchiveFile = pathToArtifactZipArchive.toFile();
        try {
            List<Path> pathsToInclude = new ArrayList<>();
            for (String fileNameToExclude : ignoreFilesList) {
                pathsToInclude.add(Paths.get(sourceFilePath, fileNameToExclude));
            }

            List<Path> sourceFolderPaths = Files.walk(sourceFolder.toPath()).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for (Path path : sourceFolderPaths) {
                boolean needToDelete = true;
                for (Path pathToInclude : pathsToInclude) {
                    if (path.toString().contains(pathToInclude.toString())) {
                        needToDelete = false;
                        break;
                    }
                }
                if (needToDelete) {
                    if (!Files.isDirectory(path) || path.toFile().list() != null && path.toFile().list().length == 0 && !path.equals(sourceFolder.toPath())) {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }

            byte[] bundledModel = downloadArtifact();
            if (ArtifactType.VALUE_MAPPING.equals(artifactType)) {
                FileUtils.writeByteArrayToFile(artifactZipArchiveFile, formatValueMapping(bundledModel));
            } else {
                FileUtils.writeByteArrayToFile(artifactZipArchiveFile, bundledModel);
            }
            ZipUtil.unpack(artifactZipArchiveFile, sourceFolder);

        } finally {
            Files.deleteIfExists(pathToArtifactZipArchive);
        }
    }

    private byte[] formatValueMapping(byte[] zipArchive) {
        try (
            ByteArrayInputStream bais = new ByteArrayInputStream(zipArchive);
            ZipInputStream zipIn = new ZipInputStream(bais);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(baos)
        ) {
            ZipEntry sourceEntry = zipIn.getNextEntry();

            while (sourceEntry != null) {
                String sourceFileName = sourceEntry.getName();
                if (sourceFileName.endsWith(".xml")) {
                    byte[] formattedEntry = XMLUtils.prettyPrintXML(IOUtils.toByteArray(zipIn)).getBytes(StandardCharsets.UTF_8);
                    zipOut.putNextEntry(new ZipEntry(sourceFileName));
                    zipOut.write(formattedEntry);
                } else {
                    byte[] entry = IOUtils.toByteArray(zipIn);
                    zipOut.putNextEntry(new ZipEntry(sourceFileName));
                    zipOut.write(entry);
                }

                zipIn.closeEntry();
                zipOut.closeEntry();
                sourceEntry = zipIn.getNextEntry();
            }
            zipOut.finish();

            return baos.toByteArray();
        } catch (Exception ex) {
            log.error("Error occurred while formatting value mapping: " + ex.getMessage(), ex);
            throw new RuntimeException("Error occurred while formatting value mapping: " + ex.getMessage(), ex);
        }
    }

    private byte[] downloadArtifact() {
        byte[] bundledModel = null;

        switch (artifactType) {
            case CPI_IFLOW:
                bundledModel = cpiIntegrationFlowClient.downloadIFlow(
                    requestContext,
                    packageExternalId,
                    artifactExternalId
                );
                break;
            case VALUE_MAPPING:
                bundledModel = cpiValueMappingClient.downloadValueMapping(
                    requestContext,
                    packageExternalId,
                    artifactExternalId
                );
                break;
            case SCRIPT_COLLECTION:
                bundledModel = cpiScriptCollectionClient.downloadScriptCollection(
                    requestContext,
                    packageExternalId,
                    artifactExternalId
                );
                break;
            case CPI_MESSAGE_MAPPING:
                bundledModel = cpiMessageMappingClient.downloadMessageMapping(
                    requestContext,
                    packageExternalId,
                    artifactExternalId
                );
                break;
        }

        return bundledModel;
    }
}
