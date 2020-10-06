package com.figaf.plugin.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.HttpClient;
import org.springframework.web.client.RestTemplate;

/**
 * @author Arsenii Istlentev
 */
@AllArgsConstructor
@Getter
@Setter
public class RestTemplateWrapper {

    private RestTemplate restTemplate;
    private HttpClient httpClient;
}
