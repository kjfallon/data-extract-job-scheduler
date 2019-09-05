package org.example.extractpublisher.components;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
public class HttpUtils {

    @Autowired
    private RestTemplateBuilder restTemplate;

    public byte[] getHttpGetBinaryData(String url) {

        byte[] binaryResponseBody = null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.build().exchange(url, HttpMethod.GET, entity, byte[].class);
            binaryResponseBody = response.getBody();
        } catch (Exception e){
            e.printStackTrace();
        }

        return binaryResponseBody;
    }
}
