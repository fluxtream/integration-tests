package org.fluxtream.test.integration.fluxtream_capture;

import org.apache.tomcat.util.codec.binary.Base64;
import org.fluxtream.test.integration.config.CommonConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;

/**
 * Created by candide on 08/05/15.
 */
@Component
public class TestRestHelper {

    @Autowired
    CommonConfiguration env;

    public HttpHeaders getBasicAuthHeader(String username, String password) {
        String plainCreds = String.format("%s:%s", username, password);
        byte[] plainCredsBytes = plainCreds.getBytes();
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64Creds);
        return headers;
    }

    public TestRestTemplate getRestTemplate() {
        TestRestTemplate restTemplate = new TestRestTemplate();
        HttpMessageConverter<MultiValueMap<String,?>> formHttpMessageConverter = new FormHttpMessageConverter();
        HttpMessageConverter<String> stringHttpMessageConverter = new StringHttpMessageConverter();
        restTemplate.setMessageConverters(Arrays.asList(formHttpMessageConverter, stringHttpMessageConverter));
        return restTemplate;
    }
}
