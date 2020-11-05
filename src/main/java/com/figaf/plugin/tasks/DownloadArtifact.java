package com.figaf.plugin.tasks;

import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Arsenii Istlentev
 */
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

            byte[] bundledModel = cpiIntegrationFlowClient.downloadArtifact(requestContext, packageExternalId, artifactExternalId);
            FileUtils.writeByteArrayToFile(artifactZipArchiveFile, bundledModel);
            ZipUtil.unpack(artifactZipArchiveFile, sourceFolder);
        } finally {
            Files.deleteIfExists(pathToArtifactZipArchive);
        }
    }
}
