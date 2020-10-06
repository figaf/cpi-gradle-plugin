package com.figaf.plugin.client;

import com.figaf.plugin.client.wrapper.CpiCommonClientWrapper;
import com.figaf.plugin.entities.CpiConnectionProperties;
import com.figaf.plugin.entities.CpiIntegrationObjectData;
import com.figaf.plugin.entities.CreateIFlowRequest;
import com.figaf.plugin.entities.RestTemplateWrapper;
import com.figaf.plugin.response_parser.CpiIntegrationFlowParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Arsenii Istlentev
 */
public class IntegrationFlowClient extends CpiCommonClientWrapper {

    public CpiIntegrationObjectData getIFlowData(
        CpiConnectionProperties cpiConnectionProperties,
        String packageTechnicalName,
        String iFlowTechnicalName
    ) {

        List<CpiIntegrationObjectData> integrationFlowsInThePackage = getIntegrationFlowsByPackage(cpiConnectionProperties, packageTechnicalName);

        CpiIntegrationObjectData iFlowCpiIntegrationObjectData = null;

        for (CpiIntegrationObjectData iFlow : integrationFlowsInThePackage) {
            if (iFlow.getTechnicalName().equals(iFlowTechnicalName)) {
                iFlowCpiIntegrationObjectData = iFlow;
                break;
            }
        }

        return iFlowCpiIntegrationObjectData;
    }

    public void createIntegrationFlow(
        CpiConnectionProperties cpiConnectionProperties,
        String externalPackageId,
        CreateIFlowRequest request,
        byte[] bundledModel
    ) {
        RestTemplateWrapper restTemplateWrapper = getRestTemplateWrapper(cpiConnectionProperties);
        String token = retrieveToken(cpiConnectionProperties, restTemplateWrapper.getRestTemplate());
        String url = buildUrl(cpiConnectionProperties, String.format("/itspaces/api/1.0/workspace/%s/iflows/", externalPackageId));
        createIntegrationFlow(cpiConnectionProperties, externalPackageId, request, bundledModel, "iflowBrowse-data", url, restTemplateWrapper, token);
    }

    public void updateIntegrationFlow(
        CpiConnectionProperties cpiConnectionProperties,
        String externalPackageId,
        String externalArtifactId,
        CreateIFlowRequest request,
        byte[] bundledModel,
        Boolean uploadDraftVersion,
        String newIflowVersion
    ) {
        RestTemplateWrapper restTemplateWrapper = getRestTemplateWrapper(cpiConnectionProperties);
        String token = retrieveToken(cpiConnectionProperties, restTemplateWrapper.getRestTemplate());
        String url = buildUrl(cpiConnectionProperties, String.format("/itspaces/api/1.0/workspace/%s/artifacts", externalPackageId));
        updateIntegrationFlow(cpiConnectionProperties, externalPackageId, externalArtifactId, request, bundledModel, url, uploadDraftVersion, newIflowVersion, restTemplateWrapper, token);
    }

    public String deployIFlow(CpiConnectionProperties cpiConnectionProperties, String packageExternalId, String iFlowExternalId, String iFlowTechnicalName) {
        RestTemplateWrapper restTemplateWrapper = getRestTemplateWrapper(cpiConnectionProperties);
        String token = retrieveToken(cpiConnectionProperties, restTemplateWrapper.getRestTemplate());
        String url = buildUrl(cpiConnectionProperties, String.format("/itspaces/api/1.0/workspace/%s/artifacts/%s/entities/%s/iflows/%s?webdav=DEPLOY", packageExternalId, iFlowExternalId, iFlowExternalId, iFlowTechnicalName));
        return deployIFlow(cpiConnectionProperties, packageExternalId, url, restTemplateWrapper.getRestTemplate(), token);
    }

    public String checkDeployStatus(CpiConnectionProperties cpiConnectionProperties, String taskId) {
        String path = String.format("/itspaces/api/1.0/deploystatus/%s", taskId);
        return executeGet(
            cpiConnectionProperties,
            path,
            CpiIntegrationFlowParser::retrieveDeployStatus
        );
    }

    public byte[] downloadIntegrationFlow(CpiConnectionProperties cpiConnectionProperties, String externalPackageId, String externalArtifactId) {
        String path = String.format("/itspaces/api/1.0/workspace/%s/artifacts/%s/entities/%s", externalPackageId, externalArtifactId, externalArtifactId);
        return executeGet(
            cpiConnectionProperties,
            path,
            resolvedBody -> resolvedBody,
            byte[].class
        );
    }

    private List<CpiIntegrationObjectData> getIntegrationFlowsByPackage(
        CpiConnectionProperties cpiConnectionProperties,
        String packageTechnicalName
    ) {
        String path = String.format("/itspaces/odata/1.0/workspace.svc/ContentPackages('%s')/Artifacts?$format=json", packageTechnicalName);
        return executeGet(cpiConnectionProperties, path, CpiIntegrationFlowParser::buildCpiIntegrationObjectDataList);
    }

    private void createIntegrationFlow(
        CpiConnectionProperties cpiConnectionProperties,
        String externalPackageId,
        CreateIFlowRequest request,
        byte[] model,
        String textBodyAttrName,
        String uploadArtifactUri,
        RestTemplateWrapper restTemplateWrapper,
        String userApiCsrfToken
    ) {

        HttpResponse uploadArtifactResponse = null;
        boolean locked = false;
        try {
            lockPackage(cpiConnectionProperties, externalPackageId, userApiCsrfToken, restTemplateWrapper.getRestTemplate(), true);
            locked = true;

            HttpPost uploadArtifactRequest = new HttpPost(uploadArtifactUri);

            JSONObject requestBody = new JSONObject();
            requestBody.put("id", request.getId());
            requestBody.put("name", request.getDisplayedName());
            requestBody.put("description", request.getDescription());
            requestBody.put("type", request.getType());
            requestBody.put("additionalAttrs", new JSONObject(request.getAdditionalAttrs()));
            requestBody.put("fileName", "model.zip");

            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            entityBuilder.addBinaryBody("payload", model, ContentType.DEFAULT_BINARY, "model.zip");
            entityBuilder.addTextBody("_charset_", "UTF-8");
            entityBuilder.addTextBody(textBodyAttrName, requestBody.toString(), ContentType.APPLICATION_JSON);

            org.apache.http.HttpEntity entity = entityBuilder.build();
            uploadArtifactRequest.setHeader("X-CSRF-Token", userApiCsrfToken);
            uploadArtifactRequest.setEntity(entity);

            HttpClient client = restTemplateWrapper.getHttpClient();

            uploadArtifactResponse = client.execute(uploadArtifactRequest);

            switch (uploadArtifactResponse.getStatusLine().getStatusCode()) {
                case 201: {
                    return;
                }
                default: {
                    throw new RuntimeException("Couldn't execute artifact uploading:\n" + IOUtils.toString(uploadArtifactResponse.getEntity().getContent(), StandardCharsets.UTF_8));
                }
            }

        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while uploading artifact: " + ex.getMessage(), ex);
        } finally {
            HttpClientUtils.closeQuietly(uploadArtifactResponse);
            if (locked) {
                unlockPackage(cpiConnectionProperties, externalPackageId, userApiCsrfToken, restTemplateWrapper.getRestTemplate());
            }
        }

    }

    private void updateIntegrationFlow(
        CpiConnectionProperties cpiConnectionProperties,
        String externalPackageId,
        String externalIFlowId,
        CreateIFlowRequest request,
        byte[] bundledModel,
        String uploadArtifactUri,
        Boolean uploadDraftVersion,
        String newIflowVersion,
        RestTemplateWrapper restTemplateWrapper,
        String userApiCsrfToken
    ) {
        HttpResponse uploadArtifactResponse = null;
        boolean locked = false;
        try {
            lockOrUnlockIFlow(cpiConnectionProperties, externalPackageId, externalIFlowId, restTemplateWrapper.getRestTemplate(), userApiCsrfToken, "LOCK", true);
            try {
                lockOrUnlockIFlow(cpiConnectionProperties, externalPackageId, externalIFlowId, restTemplateWrapper.getRestTemplate(), userApiCsrfToken, "LOCK", false);
            } catch (HttpClientErrorException ex) {
                if (HttpStatus.LOCKED.equals(ex.getStatusCode())) {
                    System.out.println(String.format("artifact %s is already locked", externalIFlowId));
                } else {
                    throw new RuntimeException("Couldn't lock or unlock artifact\n" + ex.getResponseBodyAsString());
                }
            }
            locked = true;

            HttpPost uploadArtifactRequest = new HttpPost(uploadArtifactUri);

            JSONObject requestBody = new JSONObject();
            requestBody.put("id", request.getId());
            requestBody.put("entityID", request.getId());
            requestBody.put("name", request.getDisplayedName());
            if (request.getDescription() != null) {
                requestBody.put("description", request.getDescription());
            }
            requestBody.put("type", request.getType());
            requestBody.put("additionalAttrs", new JSONObject(request.getAdditionalAttrs()));
            requestBody.put("fileName", "model.zip");

            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            entityBuilder.addBinaryBody("simpleUploader", bundledModel, ContentType.DEFAULT_BINARY, "model.zip");
            entityBuilder.addTextBody("_charset_", "UTF-8");
            entityBuilder.addTextBody("simpleUploader-data", requestBody.toString(), ContentType.APPLICATION_JSON);

            System.out.println("requestBody = " + requestBody);

            org.apache.http.HttpEntity entity = entityBuilder.build();
            uploadArtifactRequest.setHeader("X-CSRF-Token", userApiCsrfToken);
            uploadArtifactRequest.setEntity(entity);

            HttpClient client = restTemplateWrapper.getHttpClient();

            uploadArtifactResponse = client.execute(uploadArtifactRequest);

            switch (uploadArtifactResponse.getStatusLine().getStatusCode()) {
                case 201:
                    JSONObject jsonObject = new JSONObject(IOUtils.toString(uploadArtifactResponse.getEntity().getContent(), "UTF-8"));
                    if (uploadDraftVersion == null || !uploadDraftVersion) {
                        System.out.println(String.format("Assigning version %s to iflow", jsonObject.getString("bundleVersion")));
                        setVersionToArtifact(
                            cpiConnectionProperties,
                            externalPackageId,
                            externalIFlowId,
                            restTemplateWrapper.getRestTemplate(),
                            userApiCsrfToken,
                            newIflowVersion
                        );
                    } else {
                        System.out.println("Iflow uploaded as draft version");
                    }
                    return;

                default:
                    throw new RuntimeException("Couldn't execute artifact uploading:\n" + IOUtils.toString(uploadArtifactResponse.getEntity().getContent(), "UTF-8"));

            }

        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while uploading value mapping: " + ex.getMessage(), ex);
        } finally {
            if (uploadArtifactResponse != null) {
                HttpClientUtils.closeQuietly(uploadArtifactResponse);
            }
            if (locked) {
                lockOrUnlockIFlow(cpiConnectionProperties, externalPackageId, externalIFlowId, restTemplateWrapper.getRestTemplate(), userApiCsrfToken, "UNLOCK", false);
            }
        }
    }

    private String deployIFlow(CpiConnectionProperties cpiConnectionProperties, String packageExternalId, String deployArtifactUri, RestTemplate restTemplate, String userApiCsrfToken) {
        boolean locked = false;
        try {
            lockPackage(cpiConnectionProperties, packageExternalId, userApiCsrfToken, restTemplate, true);
            locked = true;

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("X-CSRF-Token", userApiCsrfToken);

            org.springframework.http.HttpEntity<Void> httpEntity = new org.springframework.http.HttpEntity<>(httpHeaders);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                deployArtifactUri,
                HttpMethod.PUT,
                httpEntity,
                String.class
            );

            if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                String result = responseEntity.getBody();
                return new JSONObject(result).getString("taskId");
            } else {
                throw new RuntimeException(
                    String.format("Couldn't execute Artifact deployment:\n Code: %d, Message: %s",
                        responseEntity.getStatusCode().value(),
                        responseEntity.getBody()
                    )
                );
            }
        } finally {
            if (locked) {
                unlockPackage(cpiConnectionProperties, packageExternalId, userApiCsrfToken, restTemplate);
            }
        }
    }

    private void lockPackage(CpiConnectionProperties cpiConnectionProperties, String externalPackageId, String csrfToken, RestTemplate restTemplate, boolean forceLock) {
        lockOrUnlockPackage(cpiConnectionProperties, externalPackageId, csrfToken, restTemplate, "LOCK", forceLock);
    }

    private void unlockPackage(CpiConnectionProperties cpiConnectionProperties, String externalPackageId, String csrfToken, RestTemplate restTemplate) {
        lockOrUnlockPackage(cpiConnectionProperties, externalPackageId, csrfToken, restTemplate, "UNLOCK", false);
    }

    private void lockOrUnlockPackage(CpiConnectionProperties cpiConnectionProperties, String externalPackageId, String csrfToken, RestTemplate restTemplate, String webdav, boolean forceLock) {

        Assert.notNull(cpiConnectionProperties, "cpiConnectionProperties must be not null!");
        Assert.notNull(externalPackageId, "externalPackageId must be not null!");
        Assert.notNull(csrfToken, "csrfToken must be not null!");
        Assert.notNull(restTemplate, "restTemplate must be not null!");

        try {

            UriComponentsBuilder uriBuilder = UriComponentsBuilder.newInstance()
                .scheme(cpiConnectionProperties.getProtocol())
                .host(cpiConnectionProperties.getHost())
                .path("/itspaces/api/1.0/workspace/{0}");

            if (cpiConnectionProperties.getPort() != null) {
                uriBuilder.port(cpiConnectionProperties.getPort());
            }

            uriBuilder.queryParam("webdav", webdav);
            if (forceLock) {
                uriBuilder.queryParam("forcelock", true);
            }

            URI uri = uriBuilder
                .buildAndExpand(externalPackageId)
                .encode()
                .toUri();

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("X-CSRF-Token", csrfToken);

            org.springframework.http.HttpEntity<Void> requestEntity = new org.springframework.http.HttpEntity<>(null, httpHeaders);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                uri,
                HttpMethod.PUT,
                requestEntity,
                String.class
            );

            if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                throw new RuntimeException(String.format(
                    "Couldn't lock package %s: Code: %d, Message: %s",
                    externalPackageId,
                    responseEntity.getStatusCode().value(),
                    responseEntity.getBody())
                );
            }

        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while locking package: " + ex.getMessage(), ex);
        }
    }

    private void lockOrUnlockIFlow(CpiConnectionProperties cpiConnectionProperties, String externalPackageId, String artifactExternalId, RestTemplate restTemplate, String userApiCsrfToken, String webdav, boolean lockinfo) {

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.newInstance()
            .scheme(cpiConnectionProperties.getProtocol())
            .host(cpiConnectionProperties.getHost())
            .path("itspaces/api/1.0/workspace/{0}/artifacts/{1}");
        if (lockinfo) {
            uriBuilder.queryParam("lockinfo", "true");
        }
        uriBuilder.queryParam("webdav", webdav);

        if (cpiConnectionProperties.getPort() != null) {
            uriBuilder.port(cpiConnectionProperties.getPort());
        }

        URI lockOrUnlockArtifactUri = uriBuilder
            .buildAndExpand(externalPackageId, artifactExternalId)
            .encode()
            .toUri();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-CSRF-Token", userApiCsrfToken);

        org.springframework.http.HttpEntity<Void> requestEntity = new org.springframework.http.HttpEntity<>(httpHeaders);
        ResponseEntity<String> responseEntity = restTemplate.exchange(
            lockOrUnlockArtifactUri,
            HttpMethod.PUT,
            requestEntity,
            String.class
        );

        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            throw new RuntimeException("Couldn't lock or unlock artifact\n" + responseEntity.getBody());
        }

    }

    private void setVersionToArtifact(CpiConnectionProperties cpiConnectionProperties, String externalPackageId, String iflowExternalId, RestTemplate restTemplate, String userApiCsrfToken, String version) {
        try {

            UriComponentsBuilder uriBuilder = UriComponentsBuilder.newInstance()
                .scheme(cpiConnectionProperties.getProtocol())
                .host(cpiConnectionProperties.getHost())
                .path("itspaces/api/1.0/workspace/{0}/artifacts/{1}")
                .queryParam("notifications", "true")
                .queryParam("webdav", "CHECKIN");

            if (cpiConnectionProperties.getPort() != null) {
                uriBuilder.port(cpiConnectionProperties.getPort());
            }

            URI lockOrUnlockArtifactUri = uriBuilder
                .buildAndExpand(externalPackageId, iflowExternalId)
                .encode()
                .toUri();

            JSONObject requestBody = new JSONObject();
            requestBody.put("comment", "");
            requestBody.put("semanticVersion", version);

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("X-CSRF-Token", userApiCsrfToken);

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody.toString(), httpHeaders);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                lockOrUnlockArtifactUri,
                HttpMethod.PUT,
                entity,
                String.class
            );

            if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                throw new RuntimeException("Couldn't set version to Artifact:\n" + responseEntity.getBody());

            }

        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while setting version Artifact: " + ex.getMessage(), ex);
        }
    }

}
