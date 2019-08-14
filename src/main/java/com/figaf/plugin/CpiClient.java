package com.figaf.plugin;

import com.figaf.plugin.entities.*;
import okhttp3.HttpUrl;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Arsenii Istlentev
 */
public class CpiClient {

    public IntegrationPackage getIntegrationPackageIfExists(
        CpiConnectionProperties cpiConnectionProperties,
        String packageTechnicalName
    ) {
        List<IntegrationPackage> integrationPackagesSearchResult = getIntegrationPackages(
            cpiConnectionProperties,
            String.format("TechnicalName eq '%s'", packageTechnicalName)
        );

        if (integrationPackagesSearchResult.size() == 1) {
            return integrationPackagesSearchResult.get(0);
        } else if (integrationPackagesSearchResult.size() > 1) {
            throw new RuntimeException(
                String.format(
                    "Unexpected state: %d integration packages were found by name %s.",
                    integrationPackagesSearchResult.size(),
                    packageTechnicalName
                )
            );
        } else {
            throw new RuntimeException(String.format("Cannot find package with name %s", packageTechnicalName));
        }
    }

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

        if (iFlowCpiIntegrationObjectData == null) {
            throw new RuntimeException(String.format("Cannot find iflow with name %s in package %s", iFlowTechnicalName, packageTechnicalName));
        }
        return iFlowCpiIntegrationObjectData;
    }

    public void uploadIntegrationFlow(CpiConnectionProperties cpiConnectionProperties, String externalPackageId, String externalIFlowId, CreateIFlowRequest request, byte[] bundledModel, Boolean uploadDraftVersion) {
        try {
            HttpUrl.Builder uriBuilder = new HttpUrl.Builder()
                .scheme(cpiConnectionProperties.getProtocol())
                .host(cpiConnectionProperties.getHost())
                .encodedPath(String.format("/itspaces/api/1.0/workspace/%s/artifacts", externalPackageId));
            if (cpiConnectionProperties.getPort() != null) {
                uriBuilder.port(cpiConnectionProperties.getPort());
            }
            String uri = uriBuilder.build().toString();

            HttpClient client = HttpClients.custom().build();
            Header basicAuthHeader = createBasicAuthHeader(cpiConnectionProperties);

            String userApiCsrfToken = getCsrfToken(cpiConnectionProperties, client);

            HttpResponse uploadIFlowResponse = null;
            boolean locked = false;
            try {
                lockOrUnlockIflow(cpiConnectionProperties, externalPackageId, externalIFlowId, client, "LOCK", true);
                lockOrUnlockIflow(cpiConnectionProperties, externalPackageId, externalIFlowId, client, "LOCK", false);
                locked = true;

                HttpPost uploadIFlowRequest = new HttpPost(uri);

                JSONObject requestBody = new JSONObject();
                requestBody.put("id", request.getId());
                requestBody.put("entityID", request.getId());
                requestBody.put("name", request.getName());
                requestBody.put("description", request.getDescription());
                requestBody.put("type", request.getType());
                requestBody.put("additionalAttrs", new JSONObject(request.getAdditionalAttrs()));
                requestBody.put("fileName", "model.zip");

                MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
                entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                entityBuilder.addBinaryBody("simpleUploader", bundledModel, ContentType.DEFAULT_BINARY, "model.zip");
                entityBuilder.addTextBody("_charset_", "UTF-8");
                entityBuilder.addTextBody("simpleUploader-data", requestBody.toString(), ContentType.APPLICATION_JSON);

                HttpEntity entity = entityBuilder.build();
                uploadIFlowRequest.setHeader("X-CSRF-Token", userApiCsrfToken);
                uploadIFlowRequest.setHeader(basicAuthHeader);
                uploadIFlowRequest.setEntity(entity);

                uploadIFlowResponse = client.execute(uploadIFlowRequest);

                switch (uploadIFlowResponse.getStatusLine().getStatusCode()) {
                    case 201:
                        JSONObject jsonObject = new JSONObject(IOUtils.toString(uploadIFlowResponse.getEntity().getContent(), "UTF-8"));
                        if (uploadDraftVersion == null || !uploadDraftVersion) {
                            setVersionToIFlow(cpiConnectionProperties, externalPackageId, externalIFlowId, client, jsonObject.getString("bundleVersion"));
                        }
                        return;

                    default:
                        throw new RuntimeException("Couldn't execute iFlow uploading:\n" + IOUtils.toString(uploadIFlowResponse.getEntity().getContent(), "UTF-8"));

                }

            } finally {
                if (uploadIFlowResponse != null) {
                    HttpClientUtils.closeQuietly(uploadIFlowResponse);
                }
                if (locked) {
                    lockOrUnlockIflow(cpiConnectionProperties, externalPackageId, externalIFlowId, client, "UNLOCK", false);
                }
            }


        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while uploading iFlow: " + ex.getMessage(), ex);
        }
    }

    public String deployIFlow(CpiConnectionProperties cpiConnectionProperties, String packageExternalId, String iFlowExternalId, String iFlowTechnicalName) {
        try {
            HttpUrl.Builder uriBuilder = new HttpUrl.Builder()
                .scheme(cpiConnectionProperties.getProtocol())
                .host(cpiConnectionProperties.getHost())
                .encodedPath(String.format("/itspaces/api/1.0/workspace/%s/artifacts/%s/entities/%s/iflows/%s", packageExternalId, iFlowExternalId, iFlowExternalId, iFlowTechnicalName));
            if (cpiConnectionProperties.getPort() != null) {
                uriBuilder.port(cpiConnectionProperties.getPort());
            }
            String uri = uriBuilder.build().toString();

            HttpClient client = HttpClients.custom().build();
            Header basicAuthHeader = createBasicAuthHeader(cpiConnectionProperties);

            String userApiCsrfToken = getCsrfToken(cpiConnectionProperties, client);

            HttpResponse deployIFlowResponse = null;
            boolean locked = false;
            try {
                lockPackage(cpiConnectionProperties, packageExternalId, userApiCsrfToken, client, true);
                locked = true;

                HttpDeploy deployIFlowRequest = new HttpDeploy(uri);

                deployIFlowRequest.setHeader("X-CSRF-Token", userApiCsrfToken);
                deployIFlowRequest.setHeader(basicAuthHeader);

                deployIFlowResponse = client.execute(deployIFlowRequest);

                switch (deployIFlowResponse.getStatusLine().getStatusCode()) {
                    case 200: {
                        return new JSONObject(IOUtils.toString(deployIFlowResponse.getEntity().getContent(), "UTF-8")).getString("taskId");
                    }
                    default: {
                        throw new RuntimeException(
                            String.format("Couldn't execute iFlow deployment:\n Code: %d, Message: %s",
                                deployIFlowResponse.getStatusLine().getStatusCode(),
                                IOUtils.toString(deployIFlowResponse.getEntity().getContent(), "UTF-8")
                            )
                        );
                    }
                }

            } finally {
                HttpClientUtils.closeQuietly(deployIFlowResponse);
                if (locked) {
                    unlockPackage(cpiConnectionProperties, packageExternalId, userApiCsrfToken, client);
                }
            }


        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while deploying iFlow: " + ex.getMessage(), ex);
        }
    }

    public String checkDeployStatus(CpiConnectionProperties cpiConnectionProperties, String taskId) {
        try {
            HttpUrl.Builder uriBuilder = new HttpUrl.Builder()
                .scheme(cpiConnectionProperties.getProtocol())
                .host(cpiConnectionProperties.getHost())
                .encodedPath(String.format("/itspaces/api/1.0/deploystatus/%s", taskId))
                .addQueryParameter("$format", "json");
            if (cpiConnectionProperties.getPort() != null) {
                uriBuilder.port(cpiConnectionProperties.getPort());
            }
            String uri = uriBuilder.build().toString();

            HttpClient client = HttpClients.custom().build();

            Header basicAuthHeader = createBasicAuthHeader(cpiConnectionProperties);

            HttpGet request = new HttpGet(uri);
            request.setHeader("Content-type", "application/json");
            request.setHeader(basicAuthHeader);
            HttpResponse response = null;
            try {
                response = client.execute(request);
                switch (response.getStatusLine().getStatusCode()) {
                    case 200: {
                        JSONObject jsonObject = new JSONObject(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
                        return (String) jsonObject.get("status");
                    }
                    default: {
                        throw new RuntimeException("Couldn't check deploy status:\n" + IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
                    }
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while checking deploy status: " + ex.getMessage(), ex);
        }
    }

    public List<String> getIntegrationRuntimeErrorInformation(CpiConnectionProperties cpiConnectionProperties, String name) throws Exception {
        List<String> errorMessages = new ArrayList<>();

        HttpUrl.Builder uriBuilder = new HttpUrl.Builder()
            .scheme(cpiConnectionProperties.getProtocol())
            .host(cpiConnectionProperties.getHost())
            .encodedPath(String.format("/api/v1/IntegrationRuntimeArtifacts('%s')/ErrorInformation/$value", name));
        if (cpiConnectionProperties.getPort() != null) {
            uriBuilder.port(cpiConnectionProperties.getPort());
        }
        String uri = uriBuilder.build().toString();

        HttpClient client = HttpClients.custom().build();

        Header basicAuthHeader = createBasicAuthHeader(cpiConnectionProperties);

        HttpGet request = new HttpGet(uri);
        request.setHeader("Content-type", "application/json");
        request.setHeader(basicAuthHeader);
        HttpResponse response = null;
        try {
            response = client.execute(request);
            switch (response.getStatusLine().getStatusCode()) {
                case 200: {
                    JSONObject jsonObject = new JSONObject(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
                    JSONArray childInstances = jsonObject.getJSONArray("childInstances");
                    for (int i = 0; i < childInstances.length(); i++) {
                        JSONObject child = childInstances.getJSONObject(i);
                        JSONArray parameters = child.getJSONArray("parameter");
                        for (int j = 0; j < parameters.length(); j++) {
                            errorMessages.add(parameters.getString(j));
                        }
                    }
                    return errorMessages;
                }
                default: {
                    throw new RuntimeException("Couldn't get error messages:\n" + IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
                }
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public IntegrationContent getIntegrationRuntimeArtifactByName(CpiConnectionProperties cpiConnectionProperties, String name) {
        try {
            HttpUrl.Builder uriBuilder = new HttpUrl.Builder()
                .scheme(cpiConnectionProperties.getProtocol())
                .host(cpiConnectionProperties.getHost())
                .encodedPath(String.format("/api/v1/IntegrationRuntimeArtifacts('%s')", name))
                .addQueryParameter("$format", "json");
            if (cpiConnectionProperties.getPort() != null) {
                uriBuilder.port(cpiConnectionProperties.getPort());
            }
            String uri = uriBuilder.build().toString();

            HttpClient client = HttpClients.custom().build();

            Header basicAuthHeader = createBasicAuthHeader(cpiConnectionProperties);

            HttpGet request = new HttpGet(uri);
            request.setHeader("Content-type", "application/json");
            request.setHeader(basicAuthHeader);
            HttpResponse response = null;
            try {
                response = client.execute(request);
                switch (response.getStatusLine().getStatusCode()) {
                    case 200: {
                        JSONObject responseModel = new JSONObject(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
                        JSONObject integrationContentEntry = responseModel.getJSONObject("d");
                        IntegrationContent integrationContent = fillIntegrationContent(integrationContentEntry);
                        return integrationContent;
                    }
                    default: {
                        throw new RuntimeException("Couldn't execute integration runtime artifact GET request:\n" + IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
                    }
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while fetching integration runtime artifact: " + ex.getMessage(), ex);
        }
    }

    public byte[] downloadIntegrationFlow(
        CpiConnectionProperties cpiConnectionProperties,
        String externalPackageId,
        String externalIFlowId) {
        try {
            HttpUrl.Builder uriBuilder = new HttpUrl.Builder()
                .scheme(cpiConnectionProperties.getProtocol())
                .host(cpiConnectionProperties.getHost())
                .encodedPath(String.format("/itspaces/api/1.0/workspace/%s/artifacts/%s/entities/%s", externalPackageId, externalIFlowId, externalIFlowId));
            if (cpiConnectionProperties.getPort() != null) {
                uriBuilder.port(cpiConnectionProperties.getPort());
            }
            String uri = uriBuilder.build().toString();

            HttpClient client = HttpClients.custom().build();

            HttpResponse downloadIFlowResponse = null;
            try {

                HttpGet downloadIFlowRequest = new HttpGet(uri);
                downloadIFlowRequest.setHeader(createBasicAuthHeader(cpiConnectionProperties));
                downloadIFlowResponse = client.execute(downloadIFlowRequest);

                switch (downloadIFlowResponse.getStatusLine().getStatusCode()) {
                    case 200: {
                        return IOUtils.toByteArray(downloadIFlowResponse.getEntity().getContent());
                    }
                    default: {
                        throw new RuntimeException("Couldn't execute iFlow downloading:\n" +
                            IOUtils.toString(downloadIFlowResponse.getEntity().getContent(), "UTF-8")
                        );
                    }
                }

            } finally {
                HttpClientUtils.closeQuietly(downloadIFlowResponse);
            }

        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while while downloading an iFlow: " + ex.getMessage(), ex);
        }
    }

    private List<IntegrationPackage> getIntegrationPackages(CpiConnectionProperties cpiConnectionProperties, String filter) {
        HttpResponse httpResponse;
        try {

            HttpUrl.Builder builder = new HttpUrl.Builder()
                .scheme(cpiConnectionProperties.getProtocol())
                .host(cpiConnectionProperties.getHost())
                .encodedPath("/itspaces/odata/1.0/workspace.svc/ContentPackages")
                .addQueryParameter("$format", "json")
                .addQueryParameter("$filter", filter);
            if (cpiConnectionProperties.getPort() != null) {
                builder.port(cpiConnectionProperties.getPort());
            }
            String url = builder.build().toString();

            HttpGet getRequest = new HttpGet(url);
            Header basicAuthHeader = createBasicAuthHeader(cpiConnectionProperties);
            getRequest.setHeader(basicAuthHeader);
            HttpClient httpClient = HttpClients.custom().build();
            httpResponse = httpClient.execute(getRequest);
            httpResponse.getEntity().getContent();
            JSONObject response = new JSONObject(IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8"));
            JSONArray packagesJsonArray = response.getJSONObject("d").getJSONArray("results");

            List<IntegrationPackage> packages = new ArrayList<>();
            for (int ind = 0; ind < packagesJsonArray.length(); ind++) {
                JSONObject packageElement = packagesJsonArray.getJSONObject(ind);

                IntegrationPackage integrationPackage = new IntegrationPackage();
                integrationPackage.setExternalId(packageElement.getString("reg_id"));
                integrationPackage.setTechnicalName(packageElement.getString("TechnicalName"));
                integrationPackage.setDisplayedName(packageElement.getString("DisplayName"));
                integrationPackage.setVersion(optString(packageElement, "Version"));
                integrationPackage.setCreationDate(
                    new Timestamp(Long.parseLong(packageElement.getString("CreatedAt").replaceAll("[^0-9]", "")))
                );
                integrationPackage.setCreatedBy(optString(packageElement, "CreatedBy"));
                String modifiedAt = optString(packageElement, "ModifiedAt");
                integrationPackage.setModificationDate(modifiedAt != null
                    ? new Timestamp(Long.parseLong(modifiedAt.replaceAll("[^0-9]", "")))
                    : null
                );
                integrationPackage.setModifiedBy(packageElement.getString("ModifiedBy"));

                packages.add(integrationPackage);
            }

            return packages;
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while getting packages: " + ex.getMessage(), ex);
        }
    }

    private List<CpiIntegrationObjectData> getIntegrationFlowsByPackage(
        CpiConnectionProperties cpiConnectionProperties,
        String packageTechnicalName
    ) {
        HttpResponse httpResponse = null;
        try {

            HttpUrl.Builder uriBuilder = new HttpUrl.Builder()
                .scheme(cpiConnectionProperties.getProtocol())
                .host(cpiConnectionProperties.getHost())
                .encodedPath(String.format("/itspaces/odata/1.0/workspace.svc/ContentPackages('%s')/Artifacts", packageTechnicalName))
                .addQueryParameter("$format", "json");
            if (cpiConnectionProperties.getPort() != null) {
                uriBuilder.port(cpiConnectionProperties.getPort());
            }
            String uri = uriBuilder.build().toString();

            HttpGet getRequest = new HttpGet(uri);
            getRequest.setHeader(createBasicAuthHeader(cpiConnectionProperties));
            HttpClient httpClient = HttpClients.custom().build();
            httpResponse = httpClient.execute(getRequest);

            JSONObject response = new JSONObject(IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8"));
            JSONArray iFlowsJsonArray = response.getJSONObject("d").getJSONArray("results");

            List<CpiIntegrationObjectData> integrationFlows = new ArrayList<>();

            for (int ind = 0; ind < iFlowsJsonArray.length(); ind++) {
                JSONObject iFlowElement = iFlowsJsonArray.getJSONObject(ind);

                if (!StringUtils.equals(optString(iFlowElement, "Type"), "IFlow")) {
                    continue;
                }

                CpiIntegrationObjectData integrationFlow = new CpiIntegrationObjectData();
                integrationFlow.setExternalId(iFlowElement.getString("reg_id"));
                integrationFlow.setTechnicalName(iFlowElement.getString("Name"));
                integrationFlow.setDisplayedName(iFlowElement.getString("DisplayName"));
                integrationFlow.setVersion(optString(iFlowElement, "Version"));
                integrationFlow.setCreationDate(
                    new Timestamp(Long.parseLong(iFlowElement.getString("CreatedAt").replaceAll("[^0-9]", "")))
                );
                integrationFlow.setCreatedBy(optString(iFlowElement, "CreatedBy"));
                String modifiedAt = optString(iFlowElement, "ModifiedAt");
                integrationFlow.setModificationDate(modifiedAt != null
                    ? new Timestamp(Long.parseLong(modifiedAt.replaceAll("[^0-9]", "")))
                    : null
                );
                integrationFlow.setModifiedBy(iFlowElement.getString("ModifiedBy"));
                integrationFlow.setDescription(iFlowElement.getString("Description"));

                integrationFlows.add(integrationFlow);
            }

            return integrationFlows;
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while getting iFlows: " + ex.getMessage(), ex);
        }
    }

    private void lockOrUnlockIflow(CpiConnectionProperties cpiConnectionProperties, String externalPackageId, String iflowExternalId, HttpClient client, String webdav, boolean lockinfo) {
        try {
            HttpUrl.Builder uriBuilder = new HttpUrl.Builder()
                .scheme(cpiConnectionProperties.getProtocol())
                .host(cpiConnectionProperties.getHost())
                .encodedPath(String.format("/itspaces/api/1.0/workspace/%s/artifacts/%s", externalPackageId, iflowExternalId))
                .addQueryParameter("webdav", webdav);
            if (lockinfo) {
                uriBuilder.addQueryParameter("lockinfo", "true");
            }
            if (cpiConnectionProperties.getPort() != null) {
                uriBuilder.port(cpiConnectionProperties.getPort());
            }
            String uri = uriBuilder.build().toString();

            Header basicAuthHeader = createBasicAuthHeader(cpiConnectionProperties);

            String userApiCsrfToken = getCsrfToken(cpiConnectionProperties, client);
            HttpPut lockIFlowRequest = new HttpPut(uri);
            HttpResponse httpResponse = null;
            try {
                lockIFlowRequest.setHeader("X-CSRF-Token", userApiCsrfToken);
                lockIFlowRequest.setHeader(basicAuthHeader);
                httpResponse = client.execute(lockIFlowRequest);

                switch (httpResponse.getStatusLine().getStatusCode()) {
                    case 200:
                        return;

                    default:
                        throw new RuntimeException("Couldn't lock or unlock iFlow\n" + IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8"));
                }

            } finally {
                HttpClientUtils.closeQuietly(httpResponse);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while locking or unlocking iFlow: " + ex.getMessage(), ex);
        }
    }

    private void setVersionToIFlow(CpiConnectionProperties cpiConnectionProperties, String externalPackageId, String iflowExternalId, HttpClient client, String version) {
        try {
            HttpUrl.Builder uriBuilder = new HttpUrl.Builder()
                .scheme(cpiConnectionProperties.getProtocol())
                .host(cpiConnectionProperties.getHost())
                .encodedPath(String.format("/itspaces/api/1.0/workspace/%s/artifacts/%s", externalPackageId, iflowExternalId))
                .addQueryParameter("notifications", "true")
                .addQueryParameter("webdav", "CHECKIN");
            if (cpiConnectionProperties.getPort() != null) {
                uriBuilder.port(cpiConnectionProperties.getPort());
            }
            String uri = uriBuilder.build().toString();

            Header basicAuthHeader = createBasicAuthHeader(cpiConnectionProperties);

            String userApiCsrfToken = getCsrfToken(cpiConnectionProperties, client);
            HttpPut httpPutRequest = new HttpPut(uri);
            HttpResponse httpResponse = null;
            try {
                httpPutRequest.setHeader("X-CSRF-Token", userApiCsrfToken);
                httpPutRequest.setHeader(basicAuthHeader);

                JSONObject requestBody = new JSONObject();
                requestBody.put("comment", "");
                requestBody.put("semanticVersion", version);

                HttpEntity entity = new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON);
                httpPutRequest.setEntity(entity);
                httpResponse = client.execute(httpPutRequest);

                switch (httpResponse.getStatusLine().getStatusCode()) {
                    case 200:
                        return;

                    default:
                        throw new RuntimeException("Couldn't set version to IFlow:\n" + IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8"));

                }

            } finally {
                HttpClientUtils.closeQuietly(httpResponse);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while setting version iFlow: " + ex.getMessage(), ex);
        }
    }

    private void lockPackage(CpiConnectionProperties cpiConnectionProperties, String externalPackageId, String csrfToken, HttpClient httpClient, boolean forceLock) {
        Validate.notNull(cpiConnectionProperties, "agent must be not null!");
        Validate.notNull(externalPackageId, "externalPackageId must be not null!");
        Validate.notNull(csrfToken, "csrfToken must be not null!");
        Validate.notNull(httpClient, "httpClient must be not null!");

        HttpResponse lockResponse = null;
        try {

            HttpUrl.Builder uriBuilder = new HttpUrl.Builder()
                .scheme(cpiConnectionProperties.getProtocol())
                .host(cpiConnectionProperties.getHost())
                .encodedPath(String.format("/itspaces/api/1.0/workspace/%s", externalPackageId));
            if (cpiConnectionProperties.getPort() != null) {
                uriBuilder.port(cpiConnectionProperties.getPort());
            }
            if (forceLock) {
                uriBuilder.addQueryParameter("forcelock", "true");
            }
            String uri = uriBuilder.build().toString();

            HttpLock lockRequest = new HttpLock(uri);
            lockRequest.setHeader(new BasicHeader("X-CSRF-Token", csrfToken));
            lockRequest.setHeader(createBasicAuthHeader(cpiConnectionProperties));

            lockResponse = httpClient.execute(lockRequest);

            switch (lockResponse.getStatusLine().getStatusCode()) {
                case 200:
                    break;
                default:
                    throw new RuntimeException(String.format(
                        "Couldn't lock package %s: Code: %d, Message: %s",
                        externalPackageId,
                        lockResponse.getStatusLine().getStatusCode(),
                        IOUtils.toString(lockResponse.getEntity().getContent(), "UTF-8"))
                    );
            }

        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while locking package: " + ex.getMessage(), ex);
        } finally {
            HttpClientUtils.closeQuietly(lockResponse);
        }
    }

    private void unlockPackage(CpiConnectionProperties cpiConnectionProperties, String externalPackageId, String csrfToken, HttpClient httpClient) {
        Validate.notNull(cpiConnectionProperties, "agent must be not null!");
        Validate.notNull(externalPackageId, "externalPackageId must be not null!");
        Validate.notNull(csrfToken, "csrfToken must be not null!");
        Validate.notNull(httpClient, "httpClient must be not null!");

        HttpResponse unlockResponse = null;
        try {

            HttpUrl.Builder uriBuilder = new HttpUrl.Builder()
                .scheme(cpiConnectionProperties.getProtocol())
                .host(cpiConnectionProperties.getHost())
                .encodedPath(String.format("/itspaces/api/1.0/workspace/%s", externalPackageId));
            if (cpiConnectionProperties.getPort() != null) {
                uriBuilder.port(cpiConnectionProperties.getPort());
            }
            String uri = uriBuilder.build().toString();

            HttpUnlock unlockRequest = new HttpUnlock(uri);
            unlockRequest.setHeader(new BasicHeader("X-CSRF-Token", csrfToken));
            unlockRequest.setHeader(createBasicAuthHeader(cpiConnectionProperties));

            unlockResponse = httpClient.execute(unlockRequest);

            switch (unlockResponse.getStatusLine().getStatusCode()) {
                case 200:
                    break;
                default:
                    throw new RuntimeException(String.format(
                        "Couldn't unlock package %s: Code: %d, Message: %s",
                        externalPackageId,
                        unlockResponse.getStatusLine().getStatusCode(),
                        IOUtils.toString(unlockResponse.getEntity().getContent(), "UTF-8"))
                    );
            }

        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while unlocking package: " + ex.getMessage(), ex);
        } finally {
            HttpClientUtils.closeQuietly(unlockResponse);
        }
    }

    private IntegrationContent fillIntegrationContent(JSONObject integrationContentEntry) throws JSONException {
        IntegrationContent integrationContent = new IntegrationContent();
        integrationContent.setId(integrationContentEntry.getString("Id"));
        integrationContent.setVersion(integrationContentEntry.getString("Version"));
        integrationContent.setName(integrationContentEntry.getString("Name"));
        integrationContent.setType(integrationContentEntry.getString("Type"));
        integrationContent.setDeployedBy(integrationContentEntry.getString("DeployedBy"));
        integrationContent.setDeployedOn(
            new Timestamp(Long.parseLong(integrationContentEntry.getString("DeployedOn").replaceAll("[^0-9]", "")))
        );
        integrationContent.setStatus(integrationContentEntry.getString("Status"));
        return integrationContent;
    }


    private String getCsrfToken(CpiConnectionProperties cpiConnectionProperties, HttpClient httpClient) {
        Validate.notNull(cpiConnectionProperties, "httpClient must be not null!");
        Validate.notNull(httpClient, "httpClient must be not null!");

        HttpResponse headResponse = null;
        try {

            HttpUrl.Builder uriBuilder = new HttpUrl.Builder()
                .scheme(cpiConnectionProperties.getProtocol())
                .host(cpiConnectionProperties.getHost())
                .encodedPath("/itspaces/api/1.0/user");
            if (cpiConnectionProperties.getPort() != null) {
                uriBuilder.port(cpiConnectionProperties.getPort());
            }
            String uri = uriBuilder.build().toString();

            HttpGet getRequest = new HttpGet(uri);
            getRequest.setHeader("X-CSRF-Token", "Fetch");
            getRequest.setHeader(createBasicAuthHeader(cpiConnectionProperties));

            headResponse = httpClient.execute(getRequest);

            if (headResponse == null) {
                throw new RuntimeException("Couldn't fetch token: response is null.");
            }

            if (headResponse.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException(String.format(
                    "Couldn't fetch token. Code: %d, Message: %s",
                    headResponse.getStatusLine().getStatusCode(),
                    IOUtils.toString(headResponse.getEntity().getContent(), "UTF-8"))
                );
            }

            return headResponse.getFirstHeader("X-CSRF-Token").getValue();

        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while fetching csrf token: " + ex.getMessage(), ex);
        } finally {
            HttpClientUtils.closeQuietly(headResponse);
        }
    }

    private Header createBasicAuthHeader(CpiConnectionProperties cpiConnectionProperties) {
        return new BasicHeader(
            "Authorization",
            String.format(
                "Basic %s",
                Base64.encodeBase64String(
                    (cpiConnectionProperties.getUsername() + ":" + cpiConnectionProperties.getPassword()).getBytes(StandardCharsets.UTF_8)
                )
            )
        );
    }


    private String optString(JSONObject json, String key) {
        if (json.isNull(key)) {
            return null;
        } else {
            return json.optString(key, null);
        }
    }
}
