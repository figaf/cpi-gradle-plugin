package com.figaf.plugin.client.wrapper;

import com.figaf.plugin.entities.RestTemplateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

/**
 * @author Arsenii Istlentev
 */
@Slf4j
public class RestTemplateWrapperHolder {

    private static RestTemplateWrapper INSTANCE;

    public static HttpClientBuilder getHttpClientBuilder() {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        RequestConfig requestConfig = RequestConfig.custom().build();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);
        return httpClientBuilder;
    }

    public static HttpClient createHttpClient() {
        return getHttpClientBuilder().build();
    }

    public static HttpComponentsClientHttpRequestFactory getHttpComponentsClientHttpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory(createHttpClient());
    }

    public static RestTemplate createRestTemplate(BasicAuthenticationInterceptor basicAuthenticationInterceptor) {
        RestTemplate restTemplate = new RestTemplate(getHttpComponentsClientHttpRequestFactory());
        restTemplate.getInterceptors().add(basicAuthenticationInterceptor);
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }

    public static RestTemplateWrapper createRestTemplateWrapper() {
        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory = getHttpComponentsClientHttpRequestFactory();
        HttpClient httpClient = httpComponentsClientHttpRequestFactory.getHttpClient();
        RestTemplate restTemplate = new RestTemplate(httpComponentsClientHttpRequestFactory);
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return new RestTemplateWrapper(restTemplate, httpClient);
    }

    public static RestTemplateWrapper createRestTemplateWrapper(BasicAuthenticationInterceptor basicAuthenticationInterceptor) {
        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory = getHttpComponentsClientHttpRequestFactory();
        HttpClient httpClient = httpComponentsClientHttpRequestFactory.getHttpClient();
        RestTemplate restTemplate = new RestTemplate(httpComponentsClientHttpRequestFactory);
        restTemplate.getInterceptors().add(basicAuthenticationInterceptor);
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return new RestTemplateWrapper(restTemplate, httpClient);
    }

    public static RestTemplateWrapper getOrCreateRestTemplateWrapperSingleton() {
        if (INSTANCE == null) {
            INSTANCE = createRestTemplateWrapper();
        }
        return INSTANCE;
    }

    public static RestTemplateWrapper createNewRestTemplateWrapper() {
        INSTANCE = createRestTemplateWrapper();
        return INSTANCE;
    }
}
