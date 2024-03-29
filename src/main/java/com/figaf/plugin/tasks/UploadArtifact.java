package com.figaf.plugin.tasks;

import com.figaf.integration.cpi.entity.designtime_artifacts.*;
import lombok.Getter;
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
import java.util.*;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import static com.figaf.integration.cpi.entity.designtime_artifacts.CpiArtifactType.*;
import static java.lang.String.format;

/**
 * @author Arsenii Istlentev
 * @author Sergey Klochkov
 */
@Slf4j
@Setter
public class UploadArtifact extends AbstractArtifactTask {

    private static final Pattern NS_ELEMENT_WITHOUT_PREFIX_PATTERN = Pattern.compile("(?<!./)/([\\w-_.]+)(?!\\w*:)");
    private static final Pattern NS_ELEMENT_WITH_PREFIX_PATTERN = Pattern.compile("/(\\w+):([\\w-_.]+)");
    private static final Map<CpiArtifactType, String> ARTIFACT_TYPE_TO_PREFIX_MAP = new HashMap<>();
    static {
        ARTIFACT_TYPE_TO_PREFIX_MAP.put(IFLOW, "iflow");
        ARTIFACT_TYPE_TO_PREFIX_MAP.put(VALUE_MAPPING, "vm");
        ARTIFACT_TYPE_TO_PREFIX_MAP.put(SCRIPT_COLLECTION, "sc");
        ARTIFACT_TYPE_TO_PREFIX_MAP.put(MESSAGE_MAPPING, "mm");
        ARTIFACT_TYPE_TO_PREFIX_MAP.put(FUNCTION_LIBRARIES, "fl");
    }

    @Getter
    @Input
    private boolean uploadDraftVersion;

    public void doTaskAction() throws IOException {
        System.out.println("uploadArtifact");
        defineParameters(false);

        Path pathToDirectoryWithExcludedFiles = Files.createTempDirectory(
            format(
                "cpi-plugin-upload-%s-%s",
                ARTIFACT_TYPE_TO_PREFIX_MAP.get(artifactType),
                UUID.randomUUID()
            )
        );
        CreateOrUpdateCpiArtifactRequest uploadArtifactRequest = createRequest();

        File directoryWithExcludedFiles = pathToDirectoryWithExcludedFiles.toFile();
        try {
            copySourceFolderToTempDirectoryWithExcludedFiles(directoryWithExcludedFiles);
            fillUploadRequest(uploadArtifactRequest, directoryWithExcludedFiles);
            if (artifactExternalId == null) {
                createArtifact(uploadArtifactRequest);
            } else {
                updateArtifact(uploadArtifactRequest);
            }
        } finally {
            FileUtils.deleteDirectory(directoryWithExcludedFiles);
        }
    }

    private void createArtifact(CreateOrUpdateCpiArtifactRequest uploadArtifactRequest) {
        switch (artifactType) {
            case IFLOW:
                cpiIntegrationFlowClient.createIFlow(requestContext, (CreateIFlowRequest) uploadArtifactRequest);
                break;
            case VALUE_MAPPING:
                cpiValueMappingClient.createValueMapping(requestContext, (CreateValueMappingRequest) uploadArtifactRequest);
                break;
            case SCRIPT_COLLECTION:
                 cpiScriptCollectionClient.createScriptCollection(
                     requestContext,
                     (CreateScriptCollectionRequest) uploadArtifactRequest
                 );
                break;
            case MESSAGE_MAPPING:
                cpiMessageMappingClient.createMessageMapping(
                    requestContext,
                    (CreateMessageMappingRequest) uploadArtifactRequest
                );
                break;
            case FUNCTION_LIBRARIES:
                cpiFunctionLibrariesClient.createFunctionLibraries(
                    requestContext,
                    (CreateFunctionLibrariesRequest) uploadArtifactRequest
                );
                break;
        }
    }

    private void updateArtifact(CreateOrUpdateCpiArtifactRequest uploadArtifactRequest) {
        cpiRuntimeArtifactClient.updateArtifact(requestContext, uploadArtifactRequest);
    }

    private CreateOrUpdateCpiArtifactRequest createRequest() {
        if (artifactExternalId == null) {
            return createCreateRequest();
        } else {
            return createUpdateRequest();
        }
    }

    private CreateOrUpdateCpiArtifactRequest createCreateRequest() {
        CreateOrUpdateCpiArtifactRequest createRequest = null;

        switch (artifactType) {
            case IFLOW:
                createRequest = CreateIFlowRequest.builder().build();
                break;
            case VALUE_MAPPING:
                createRequest = CreateValueMappingRequest.builder().build();
                break;
            case SCRIPT_COLLECTION:
                createRequest = CreateScriptCollectionRequest.builder().build();
                break;
            case MESSAGE_MAPPING:
                createRequest = CreateMessageMappingRequest.builder().build();
                break;
            case FUNCTION_LIBRARIES:
                createRequest = CreateFunctionLibrariesRequest.builder().build();
                break;
        }

        return createRequest;
    }

    private CreateOrUpdateCpiArtifactRequest createUpdateRequest() {
        CreateOrUpdateCpiArtifactRequest updateRequest = null;

        switch (artifactType) {
            case IFLOW:
                updateRequest = UpdateIFlowRequest.builder().build();
                break;
            case VALUE_MAPPING:
                updateRequest = UpdateValueMappingRequest.builder().build();
                break;
            case SCRIPT_COLLECTION:
                updateRequest = UpdateScriptCollectionRequest.builder().build();
                break;
            case MESSAGE_MAPPING:
                updateRequest = UpdateMessageMappingRequest.builder().build();
                break;
            case FUNCTION_LIBRARIES:
                updateRequest = UpdateFunctionLibrariesRequest.builder().build();
                break;
        }

        return updateRequest;
    }

    private void fillUploadRequest(CreateOrUpdateCpiArtifactRequest uploadArtifactRequest, File directoryWithExcludedFiles) throws IOException {
        if (artifactExternalId != null) {
            uploadArtifactRequest.setId(artifactExternalId);
            uploadArtifactRequest.setUploadDraftVersion(uploadDraftVersion);
        } else {
            uploadArtifactRequest.setId(artifactTechnicalName);
        }
        fillPackageExternalIdAndCreatePackageIfNeeded(uploadArtifactRequest);
        fillBundledModel(uploadArtifactRequest, directoryWithExcludedFiles);
        fillUploadRequestViaManifest(uploadArtifactRequest, directoryWithExcludedFiles);
        fillUploadRequestViaProperties(uploadArtifactRequest, directoryWithExcludedFiles);
    }

    private void fillUploadRequestViaManifest(CreateOrUpdateCpiArtifactRequest uploadArtifactRequest, File directoryWithExcludedFiles) {
        Manifest manifest = parseManifestFile(directoryWithExcludedFiles);
        String artifactDisplayedName = manifest.getMainAttributes().getValue("Bundle-Name");
        if (artifactDisplayedName == null) {
            log.error("artifactDisplayedName is null, artifactTechnicalName will be used instead");
            artifactDisplayedName = artifactTechnicalName;
        }

        uploadArtifactRequest.setName(artifactDisplayedName);
        if (artifactExternalId != null) {
            uploadArtifactRequest.setNewArtifactVersion(manifest.getMainAttributes().getValue("Bundle-Version"));
        }
    }

    private void fillUploadRequestViaProperties(CreateOrUpdateCpiArtifactRequest uploadArtifactRequest, File directoryWithExcludedFiles) throws IOException {
        Properties properties = getMetainfoProperties(directoryWithExcludedFiles);

        uploadArtifactRequest.setDescription(properties.getProperty("description"));

        CreateOrUpdateCpiArtifactRequest.AdditionalAttributes additionalAttributes = new CreateOrUpdateCpiArtifactRequest.AdditionalAttributes();
        String sourceValue = properties.getProperty("source");
        if (sourceValue != null) {
            additionalAttributes.getSource().add(sourceValue);
        }
        String targetValue = properties.getProperty("target");
        if (targetValue != null) {
            additionalAttributes.getTarget().add(targetValue);
        }
        uploadArtifactRequest.setAdditionalAttrs(additionalAttributes);
    }

    private void fillBundledModel(CreateOrUpdateCpiArtifactRequest uploadArtifactRequest, File directoryWithExcludedFiles) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipUtil.pack(directoryWithExcludedFiles, bos);
        bos.close();
        uploadArtifactRequest.setBundledModel(bos.toByteArray());
    }

    private void fillPackageExternalIdAndCreatePackageIfNeeded(CreateOrUpdateCpiArtifactRequest uploadArtifactRequest) {
        if (packageExternalId == null) {
            createPackage();
        }
        uploadArtifactRequest.setPackageExternalId(packageExternalId);
    }

    private void copySourceFolderToTempDirectoryWithExcludedFiles(File directoryWithExcludedFiles) throws IOException {
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
    }

    private void createPackage() {
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
            String errorMsg = format("Error while parsing xml document: %s", ex.getMessage());
            log.error(errorMsg, ex);
            throw new RuntimeException(errorMsg, ex);
        } finally {
            IOUtils.closeQuietly(bais);
        }

        return document;
    }

    private  <T> List<T> selectNodes(Document document, String xPathString) {
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
                String errorMsg = format("Error while applying xPath to xml document: %s", ex.getMessage());
                log.error(errorMsg, ex);
                throw new RuntimeException(errorMsg, ex);
            }
        }
    }

    private boolean isXPathString(String xPathString) {
        try {
            Dom4jXPath xPath = new Dom4jXPath(xPathString);
            return xPathString.contains("/");
        } catch (JaxenException ex) {
            return false;
        }
    }

}