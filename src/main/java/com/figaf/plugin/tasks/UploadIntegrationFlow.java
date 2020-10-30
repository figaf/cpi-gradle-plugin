package com.figaf.plugin.tasks;

import com.figaf.integration.cpi.entity.designtime_artifacts.CreateOrUpdateIFlowRequest;
import com.figaf.integration.cpi.entity.designtime_artifacts.CreateOrUpdatePackageRequest;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.gradle.api.tasks.Input;
import org.jaxen.JaxenException;
import org.jaxen.dom4j.Dom4jXPath;
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
import java.util.regex.Pattern;

/**
 * @author Arsenii Istlentev
 * @author Sergey Klochkov
 */
@Slf4j
@Setter
public class UploadIntegrationFlow extends AbstractIntegrationFlowTask {

    private static final Pattern NS_ELEMENT_WITHOUT_PREFIX_PATTERN = Pattern.compile("(?<!./)/([\\w-_.]+)(?!\\w*:)");
    private static final Pattern NS_ELEMENT_WITH_PREFIX_PATTERN = Pattern.compile("/(\\w+):([\\w-_.]+)");

    @Input
    private boolean uploadDraftVersion;

    public void doTaskAction() throws IOException {
        defineParameters(false);
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

            if (packageExternalId == null) {

                CreateOrUpdatePackageRequest createIntegrationPackageRequest = new CreateOrUpdatePackageRequest();
                createIntegrationPackageRequest.setTechnicalName(packageTechnicalName);

                Path packageInfoPath = Paths.get(sourceFolder.getParentFile().getAbsolutePath(), "/integration-package-info.xml");
                File packageInfoFile = packageInfoPath.toFile();

                if (packageInfoFile.exists() && packageInfoFile.isFile()) {

                    Document document = parseDocument(packageInfoFile);

                    List<Element> elements = selectNodes(document, "/entry/content/properties/DisplayName");
                    if (CollectionUtils.isNotEmpty(elements)) {
                        createIntegrationPackageRequest.setDisplayName(
                            elements.get(0).getStringValue()
                        );
                    }

                    elements = selectNodes(document, "/entry/content/properties/ShortText");
                    if (CollectionUtils.isNotEmpty(elements)) {
                        createIntegrationPackageRequest.setShortDescription(
                            elements.get(0).getStringValue()
                        );
                    }

                    elements = selectNodes(document, "/entry/content/properties/Vendor");
                    if (CollectionUtils.isNotEmpty(elements)) {
                        createIntegrationPackageRequest.setVendor(
                            elements.get(0).getStringValue()
                        );
                    }

                    elements = selectNodes(document, "/entry/content/properties/Version");
                    if (CollectionUtils.isNotEmpty(elements)) {
                        createIntegrationPackageRequest.setVersion(
                            elements.get(0).getStringValue()
                        );
                    }

                } else {
                    createIntegrationPackageRequest.setDisplayName(packageTechnicalName);
                    createIntegrationPackageRequest.setVersion("1.0.0");
                }

                packageExternalId = integrationPackageClient.createIntegrationPackage(requestContext, createIntegrationPackageRequest);
            }

            CreateOrUpdateIFlowRequest uploadIFlowRequest = new CreateOrUpdateIFlowRequest();
            uploadIFlowRequest.setName(integrationFlowDisplayedName);
            uploadIFlowRequest.setDescription(properties.getProperty("description"));

            CreateOrUpdateIFlowRequest.AdditionalAttributes additionalAttributes = new CreateOrUpdateIFlowRequest.AdditionalAttributes();
            String sourceValue = properties.getProperty("source");
            if (sourceValue != null) {
                additionalAttributes.getSource().add(sourceValue);
            }
            String targetValue = properties.getProperty("target");
            if (targetValue != null) {
                additionalAttributes.getTarget().add(targetValue);
            }
            uploadIFlowRequest.setAdditionalAttrs(additionalAttributes);

            if (integrationFlowExternalId == null) {
                uploadIFlowRequest.setId(integrationFlowTechnicalName);
                cpiIntegrationFlowClient.createIntegrationFlow(
                    requestContext,
                    packageExternalId,
                    uploadIFlowRequest,
                    bundledModel
                );
            } else {
                uploadIFlowRequest.setId(integrationFlowExternalId);
                cpiIntegrationFlowClient.updateArtifact(
                    requestContext,
                    packageExternalId,
                    integrationFlowExternalId,
                    uploadIFlowRequest,
                    bundledModel,
                    uploadDraftVersion,
                    manifest.getMainAttributes().getValue("Bundle-Version")
                );
            }
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

    private Document parseDocument(File xmlFile) {
        FileInputStream bais = null;
        Document document;
        try {
            bais = new FileInputStream(xmlFile);
            SAXReader reader = new SAXReader();
            document = reader.read(bais);
        } catch (Exception ex) {
            String errorMsg = String.format("Error while parsing xml document: %s", ex.getMessage());
            log.error(errorMsg, ex);
            throw new RuntimeException(errorMsg, ex);
        } finally {
            IOUtils.closeQuietly(bais);
        }

        return document;
    }

    public <T> List<T> selectNodes(Document document, String xPathString) {
        if (!isXPathString(xPathString)) {
            return new ArrayList<>();
        } else {
            try {
                if (StringUtils.isNotBlank(xPathString)) {
                    xPathString = xPathString.replaceAll(NS_ELEMENT_WITH_PREFIX_PATTERN.pattern(), "/*[name()='$1:$2']");
                    xPathString = xPathString.replaceAll(NS_ELEMENT_WITHOUT_PREFIX_PATTERN.pattern(), "/*[local-name()='$1']");
                }

                Dom4jXPath xPath = new Dom4jXPath(xPathString);
                return xPath.selectNodes(document);
            } catch (Exception ex) {
                String errorMsg = String.format("Error while applying xPath to xml document: %s", ex.getMessage());
                log.error(errorMsg, ex);
                throw new RuntimeException(errorMsg, ex);
            }
        }
    }

    public boolean isXPathString(String xPathString) {
        try {
            Dom4jXPath xPath = new Dom4jXPath(xPathString);
            return xPathString.contains("/");
        } catch (JaxenException ex) {
            return false;
        }
    }

}