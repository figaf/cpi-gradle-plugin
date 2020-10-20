package com.figaf.plugin.client.wrapper;

import com.figaf.plugin.entities.CpiConnectionProperties;
import com.figaf.plugin.entities.CpiPlatformType;
import com.figaf.plugin.entities.RestTemplateWrapper;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Arsenii Istlentev
 */
@AllArgsConstructor
public class CpiCommonClientWrapper {

    private final static int MAX_NUMBER_OF_AUTH_ATTEMPTS = 4;
    private final static Pattern LOCATION_URL_PATTERN = Pattern.compile(".*location=\"(.*)\"<\\/script>.*");
    private final static Pattern SIGNATURE_PATTERN = Pattern.compile(".*signature=(.*);path.*");
    private final static Pattern LOGIN_URL_PATTERN = Pattern.compile(".*<meta name=\"redirect\"[\\s\\S]*content=\"(.*)\">");


    public interface ResponseHandlerCallback<R, T> {
        R apply(T resolvedBody) throws Exception;
    }

    public <R> R executeGet(CpiConnectionProperties cpiConnectionProperties, String path, ResponseHandlerCallback<R, String> responseHandlerCallback) {
        return executeGet(cpiConnectionProperties, path, responseHandlerCallback, String.class);
    }

    public <R, T> R executeGet(CpiConnectionProperties cpiConnectionProperties, String path, ResponseHandlerCallback<R, T> responseHandlerCallback, Class<T> bodyType) {
        T responseBody;
        if (CpiPlatformType.CLOUD_FOUNDRY.equals(cpiConnectionProperties.getCpiPlatformType())) {
            ResponseEntity<T> initialResponseEntity = executeGetRequestReturningTextBody(cpiConnectionProperties, path, bodyType);
            responseBody = makeAuthRequestsAndReturnNeededBody(cpiConnectionProperties, path, initialResponseEntity, bodyType);
        } else {
            responseBody = executeGetRequestWithBasicAuthReturningTextBody(cpiConnectionProperties, path, bodyType);
        }

        R response;
        try {
            response = responseHandlerCallback.apply(responseBody);
        } catch (Exception ex) {
            throw new RuntimeException("Can't handle response body:", ex);
        }

        return response;
    }

    public String retrieveToken(CpiConnectionProperties cpiConnectionProperties, RestTemplate restTemplate) {
        return retrieveToken(cpiConnectionProperties, restTemplate, "/itspaces/api/1.0/user");
    }

    public String retrieveToken(CpiConnectionProperties cpiConnectionProperties, RestTemplate restTemplate, String path) {
        try {
            String url = buildUrl(cpiConnectionProperties, path);
            ResponseEntity<String> responseEntity;
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("X-CSRF-Token", "Fetch");
            if (CpiPlatformType.CLOUD_FOUNDRY.equals(cpiConnectionProperties.getCpiPlatformType())) {
                RequestEntity requestEntity = new RequestEntity(httpHeaders, HttpMethod.GET, new URI(url));
                ResponseEntity<String> initialResponseEntity = restTemplate.exchange(requestEntity, String.class);
                if (!HttpStatus.OK.equals(initialResponseEntity.getStatusCode()) || initialResponseEntity.getBody() != null) {
                    responseEntity = makeAuthRequestsAndReturnResponseEntity(cpiConnectionProperties, path, initialResponseEntity, httpHeaders, String.class, 1);
                } else {
                    responseEntity = initialResponseEntity;
                }
            } else {
                RequestEntity requestEntity = new RequestEntity(httpHeaders, HttpMethod.GET, new URI(url));
                responseEntity = restTemplate.exchange(requestEntity, String.class);
            }

            if (responseEntity == null) {
                throw new RuntimeException(String.format("Couldn't fetch token for user %s: response is null.", cpiConnectionProperties.getUsername()));
            }

            if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                throw new RuntimeException(String.format(
                    "Couldn't fetch token for user: Code: %d, Message: %s",
                    responseEntity.getStatusCode().value(),
                    responseEntity.getBody())
                );
            }

            String token = responseEntity.getHeaders().getFirst("X-CSRF-Token");
            return token;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected String buildUrl(CpiConnectionProperties cpiConnectionProperties, String path) {
        return String.format("%s%s", cpiConnectionProperties.getUrlRemovingDefaultPortIfNecessary(), path);
    }

    protected RestTemplateWrapper getRestTemplateWrapper(CpiConnectionProperties cpiConnectionProperties) {
        RestTemplateWrapper restTemplateWrapper;
        if (CpiPlatformType.CLOUD_FOUNDRY.equals(cpiConnectionProperties.getCpiPlatformType())) {
            restTemplateWrapper = RestTemplateWrapperHolder.getOrCreateRestTemplateWrapperSingleton();
        } else {
            restTemplateWrapper = RestTemplateWrapperHolder.createRestTemplateWrapper(new BasicAuthenticationInterceptor(cpiConnectionProperties.getUsername(), cpiConnectionProperties.getPassword()));
        }
        return restTemplateWrapper;
    }

    private <T> ResponseEntity<T> executeGetRequestReturningTextBody(CpiConnectionProperties cpiConnectionProperties, String path, Class<T> bodyType) {
        final String url = buildUrl(cpiConnectionProperties, path);
        try {
            RequestEntity requestEntity = new RequestEntity(HttpMethod.GET, new URI(url));
            RestTemplateWrapper restTemplateWrapper = RestTemplateWrapperHolder.getOrCreateRestTemplateWrapperSingleton();
            ResponseEntity<T> responseEntity = restTemplateWrapper.getRestTemplate().exchange(requestEntity, bodyType);
            return responseEntity;
        } catch (Exception ex) {
            String errorMessage = String.format("Can't execute GET request %s successfully: ", url);
            throw new RuntimeException(errorMessage, ex);
        }
    }

    private <T> T executeGetRequestWithBasicAuthReturningTextBody(CpiConnectionProperties cpiConnectionProperties, String path, Class<T> bodyType) {
        final String url = buildUrl(cpiConnectionProperties, path);
        try {
            RestTemplate restTemplateWithBasicAuth = RestTemplateWrapperHolder.createRestTemplate(new BasicAuthenticationInterceptor(cpiConnectionProperties.getUsername(), cpiConnectionProperties.getPassword()));
            RequestEntity requestEntity = new RequestEntity(HttpMethod.GET, new URI(url));
            ResponseEntity<T> responseEntity = restTemplateWithBasicAuth.exchange(requestEntity, bodyType);
            return responseEntity.getBody();
        } catch (Exception ex) {
            String errorMessage = String.format("Can't execute GET request %s successfully: ", url);
            throw new RuntimeException(errorMessage, ex);
        }
    }

    private <T> T makeAuthRequestsAndReturnNeededBody(CpiConnectionProperties cpiConnectionProperties, String path, ResponseEntity<T> initialResponseEntity, Class<T> responseType) {
        ResponseEntity<T> responseEntity = makeAuthRequestsAndReturnResponseEntity(cpiConnectionProperties, path, initialResponseEntity, null, responseType, 1);
        return responseEntity.getBody();
    }

    private <T> ResponseEntity<T> makeAuthRequestsAndReturnResponseEntity(
        CpiConnectionProperties cpiConnectionProperties,
        String path,
        ResponseEntity<T> initialResponseEntity,
        HttpHeaders additionalHeaders,
        Class<T> responseType,
        int numberOfAttempts
    ) {
        try {
            String responseBodyString = getResponseBodyString(initialResponseEntity);

            String authorizationUrl = retrieveAuthorizationUrl(responseBodyString);
            if (authorizationUrl == null) {
                return initialResponseEntity;
            }

            String signature = retrieveSignature(responseBodyString);

            String loginPageUrl = getLoginPageUrlFromAuthorizationPage(authorizationUrl);

            String loginPageContent = getLoginPageContent(loginPageUrl);

            MultiValueMap<String, String> loginFormData = buildLoginFormData(cpiConnectionProperties, loginPageContent);
            String redirectUrlReceivedAfterSuccessfulAuthorization = authorize("https://accounts.sap.com/saml2/idp/sso", loginFormData);

            ResponseEntity<T> result = executeRedirectRequestAfterSuccessfulAuthorization(redirectUrlReceivedAfterSuccessfulAuthorization, path, signature, additionalHeaders, responseType);

            return result;
        } catch (HttpClientErrorException ex) {
            if (HttpStatus.BAD_REQUEST.equals(ex.getStatusCode()) && numberOfAttempts < MAX_NUMBER_OF_AUTH_ATTEMPTS) {
                return makeAuthRequestsAndReturnResponseEntity(cpiConnectionProperties, path, initialResponseEntity, additionalHeaders, responseType, numberOfAttempts + 1);
            } else {
                System.err.println(ex.getResponseBodyAsString());
                throw ex;
            }
        } catch (Exception ex) {
            String errorMessage = String.format("Can't authorize and execute initial request on %s", path);
            throw new RuntimeException(errorMessage, ex);
        }
    }

    private String getLoginPageUrlFromAuthorizationPage(String url) throws URISyntaxException {
        System.out.println(String.format("#getLoginPageUrlFromAuthorizationPage(String url): %s", url));
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        RequestEntity requestEntity = new RequestEntity(httpHeaders, HttpMethod.GET, new URI(url));

        RestTemplateWrapper restTemplateWrapper = RestTemplateWrapperHolder.getOrCreateRestTemplateWrapperSingleton();
        ResponseEntity<String> responseEntity;
        try {
            responseEntity = restTemplateWrapper.getRestTemplate().exchange(requestEntity, String.class);
        } catch (HttpClientErrorException ex) {
            if (HttpStatus.BAD_REQUEST.equals(ex.getStatusCode())) {
                restTemplateWrapper = RestTemplateWrapperHolder.createNewRestTemplateWrapper();
                responseEntity = restTemplateWrapper.getRestTemplate().exchange(requestEntity, String.class);
            } else {
                throw ex;
            }
        }
        String loginPageUrl = retrieveLoginPageUrl(responseEntity.getBody());
        if (loginPageUrl == null) {
            throw new RuntimeException(String.format("Can't retrieve login page url from %s", responseEntity.getBody()));
        }

        return loginPageUrl.replaceAll("amp;", "");
    }

    private String getLoginPageContent(String url) throws Exception {
        System.out.println(String.format("#getLoginPageContent(String url): %s", url));
        RequestEntity requestEntity = new RequestEntity(HttpMethod.GET, new URI(url));
        RestTemplateWrapper restTemplateWrapper = RestTemplateWrapperHolder.getOrCreateRestTemplateWrapperSingleton();
        ResponseEntity<String> exchange = restTemplateWrapper.getRestTemplate().exchange(requestEntity, String.class);
        return exchange.getBody();
    }

    private String authorize(String url, MultiValueMap<String, String> map) {
        System.out.println(String.format("#authorize(String url, MultiValueMap<String, String> map): %s", url));
        HttpHeaders httpHeaders = new HttpHeaders();
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, httpHeaders);
        RestTemplateWrapper restTemplateWrapper = RestTemplateWrapperHolder.getOrCreateRestTemplateWrapperSingleton();
        ResponseEntity<String> response = restTemplateWrapper.getRestTemplate().postForEntity(url, request, String.class);
        String responseBody = response.getBody();
        if (StringUtils.contains(responseBody, "Sorry, we could not authenticate you")) {
            throw new RuntimeException("Login/password are not correct");
        }
        return response.getHeaders().getFirst("Location");
    }

    private <T> ResponseEntity<T> executeRedirectRequestAfterSuccessfulAuthorization(String url, String initialPath, String signature, HttpHeaders additionalHeaders, Class<T> responseType) throws Exception {
        System.out.println(String.format("#executeRedirectRequestAfterSuccessfulAuthorization(String url, String initialPath, String signature, HttpHeaders additionalHeaders, Class<T> responseType): %s, %s, %s, %s, %s",
            url, initialPath, signature, additionalHeaders, responseType
        ));

        String cookie = String.format("fragmentAfterLogin=; locationAfterLogin=%s; signature=%s", URLEncoder.encode(initialPath, "UTF-8"), signature);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", cookie);
        if (additionalHeaders != null) {
            headers.addAll(additionalHeaders);
        }

        RequestEntity requestEntity = new RequestEntity(headers, HttpMethod.GET, new URI(url));
        RestTemplateWrapper restTemplateWrapper = RestTemplateWrapperHolder.getOrCreateRestTemplateWrapperSingleton();
        ResponseEntity<T> responseEntity = restTemplateWrapper.getRestTemplate().exchange(requestEntity, responseType);
        return responseEntity;
    }

    private <T> String getResponseBodyString(ResponseEntity<T> responseEntity) {
        T responseBody = responseEntity.getBody();
        String responseBodyString;
        if (responseBody instanceof String) {
            responseBodyString = (String) responseBody;
        } else if (responseBody instanceof byte[]) {
            responseBodyString = new String((byte[]) responseBody);
        } else {
            throw new RuntimeException(String.format("Can't get string body from %s", responseBody));
        }
        return responseBodyString;
    }

    private String retrieveAuthorizationUrl(String responseBodyString) {
        return getFirstMatchedGroup(responseBodyString, LOCATION_URL_PATTERN, null);
    }

    private String retrieveSignature(String responseBodyString) {
        return getFirstMatchedGroup(responseBodyString, SIGNATURE_PATTERN, "");
    }

    private String retrieveLoginPageUrl(String responseBodyString) {
        return getFirstMatchedGroup(responseBodyString, LOGIN_URL_PATTERN, null);
    }

    private String getFirstMatchedGroup(String responseBodyString, Pattern pattern, String defaultValue) {
        Matcher matcher = pattern.matcher(responseBodyString);
        String foundGroup = defaultValue;
        if (matcher.find()) {
            foundGroup = matcher.group(1);
        }
        return foundGroup;
    }

    private MultiValueMap<String, String> buildLoginFormData(CpiConnectionProperties cpiConnectionProperties, String html) {

        Map<String, String> loginFormData = new HashMap<>();
        loginFormData.put("j_username", cpiConnectionProperties.getUsername());
        loginFormData.put("j_password", cpiConnectionProperties.getPassword());

        Document doc = Jsoup.parse(html);

        Element logOnForm = doc.getElementById("logOnForm");
        Elements inputElements = logOnForm.getElementsByTag("input");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        for (Element inputElement : inputElements) {
            String ekey = inputElement.attr("name");
            String value = inputElement.attr("value");

            for (String dataKey : loginFormData.keySet()) {
                if (ekey.equals(dataKey))
                    value = loginFormData.get(dataKey);
            }
            map.put(ekey, Collections.singletonList(value));
        }
        return map;
    }
}
