package com.figaf.plugin.client;

import com.figaf.plugin.client.wrapper.CpiCommonClientWrapper;
import com.figaf.plugin.entities.CpiConnectionProperties;
import com.figaf.plugin.entities.CreateIntegrationPackageRequest;
import com.figaf.plugin.entities.IntegrationPackage;
import com.figaf.plugin.entities.RestTemplateWrapper;
import com.figaf.plugin.response_parser.IntegrationPackageParser;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * @author Arsenii Istlentev
 */
public class IntegrationPackageClient extends CpiCommonClientWrapper {

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
            return null;
        }
    }

    public String createIntegrationPackage(
        CpiConnectionProperties cpiConnectionProperties,
        CreateIntegrationPackageRequest request
    ) {
        RestTemplateWrapper restTemplateWrapper = getRestTemplateWrapper(cpiConnectionProperties);
        String token = retrieveToken(cpiConnectionProperties, restTemplateWrapper.getRestTemplate());

        return createIntegrationPackage(cpiConnectionProperties, request, restTemplateWrapper.getRestTemplate(), token);
    }

    private List<IntegrationPackage> getIntegrationPackages(CpiConnectionProperties cpiConnectionProperties, String filter) {
        String path = "/itspaces/odata/1.0/workspace.svc/ContentPackages?$format=json" + (filter == null ? "" : "&$filter=" + filter.replace(" ", "%20"));
        return executeGet(cpiConnectionProperties, path, IntegrationPackageParser::buildIntegrationPackages);
    }

    private String createIntegrationPackage(CpiConnectionProperties cpiConnectionProperties, CreateIntegrationPackageRequest request, RestTemplate restTemplate, String userApiCsrfToken) {

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.newInstance()
            .scheme(cpiConnectionProperties.getProtocol())
            .host(cpiConnectionProperties.getHost())
            .path("/itspaces/odata/1.0/workspace.svc/ContentEntities.ContentPackages");

        if (cpiConnectionProperties.getPort() != null) {
            uriBuilder.port(cpiConnectionProperties.getPort());
        }
        URI uri = uriBuilder.build().toUri();

        JSONObject requestBody = new JSONObject()
            .put("TechnicalName", request.getTechnicalName())
            .put("DisplayName", request.getDisplayName())
            .put("ShortText", request.getShortDescription())
            .put("Vendor", request.getVendor())
            .put("Version", request.getVersion())
            .put("Category", "Integration")
            .put("SupportedPlatforms", "SAP HANA Cloud Integration");


        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-CSRF-Token", userApiCsrfToken);
        httpHeaders.add("Accept", "application/json");
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        org.springframework.http.HttpEntity<String> requestEntity = new org.springframework.http.HttpEntity<>(requestBody.toString(), httpHeaders);

        ResponseEntity<String> responseEntity = restTemplate.exchange(
            uri,
            HttpMethod.POST,
            requestEntity,
            String.class
        );

        if (HttpStatus.CREATED.equals(responseEntity.getStatusCode())) {
            JSONObject createdPackage = new JSONObject(responseEntity.getBody()).getJSONObject("d");
            return createdPackage.getString("reg_id");
        } else {
            throw new RuntimeException(String.format(
                "Couldn't create package %s: Code: %d, Message: %s",
                request.getTechnicalName(),
                responseEntity.getStatusCode().value(),
                responseEntity.getBody())
            );
        }

    }

}
