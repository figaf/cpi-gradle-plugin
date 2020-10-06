package com.figaf.plugin.client;

import com.figaf.plugin.client.wrapper.CpiCommonClientWrapper;
import com.figaf.plugin.entities.CpiConnectionProperties;
import com.figaf.plugin.entities.CpiPlatformType;
import com.figaf.plugin.entities.IntegrationContent;
import com.figaf.plugin.response_parser.IntegrationContentPrivateApiParser;
import okhttp3.HttpUrl;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
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
public class IntegrationRuntimeClient extends CpiCommonClientWrapper {

    public List<String> getIntegrationRuntimeErrorInformation(CpiConnectionProperties cpiConnectionProperties, IntegrationContent integrationContent) throws Exception {
        if (CpiPlatformType.CLOUD_FOUNDRY.equals(cpiConnectionProperties.getCpiPlatformType())) {
            String path = String.format("/itspaces/Operations/com.sap.it.op.tmn.commands.dashboard.webui.IntegrationComponentDetailCommand?artifactId=%s", integrationContent.getExternalId());
            return executeGet(
                cpiConnectionProperties,
                path,
                IntegrationContentPrivateApiParser::getIntegrationRuntimeErrorInformation
            );
        } else {
            return getIntegrationRuntimeErrorInformation(cpiConnectionProperties, integrationContent.getName());
        }
    }

    public IntegrationContent getIntegrationRuntimeArtifactByName(CpiConnectionProperties cpiConnectionProperties, String name) {
        if (CpiPlatformType.CLOUD_FOUNDRY.equals(cpiConnectionProperties.getCpiPlatformType())) {
            String path = "/itspaces/Operations/com.sap.it.op.tmn.commands.dashboard.webui.IntegrationComponentsListCommand";
            return executeGet(
                cpiConnectionProperties,
                path,
                body -> IntegrationContentPrivateApiParser.getIntegrationRuntimeArtifactByName(body, name)
            );
        } else {
            return getIntegrationRuntimeArtifactByNameUsingPublicApi(cpiConnectionProperties, name);
        }
    }

    private List<String> getIntegrationRuntimeErrorInformation(CpiConnectionProperties cpiConnectionProperties, String name) throws Exception {
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
                    handleChildInstances(childInstances, errorMessages);
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

    private IntegrationContent getIntegrationRuntimeArtifactByNameUsingPublicApi(CpiConnectionProperties cpiConnectionProperties, String name) {
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

    private void handleChildInstances(JSONArray childInstances, List<String> errorMessages) {
        if (childInstances == null) {
            return;
        }
        for (int i = 0; i < childInstances.length(); i++) {
            JSONObject child = childInstances.getJSONObject(i);
            JSONArray parameters = child.optJSONArray("parameter");
            if (parameters == null) {
                handleChildInstances(child.optJSONArray("childInstances"), errorMessages);
            } else {
                for (int j = 0; j < parameters.length(); j++) {
                    errorMessages.add(parameters.getString(j));
                }
            }
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

}
