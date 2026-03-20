package com.bootcamp.paymentdemo.common.config;

import lombok.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean("portOneRestClient")
    public RestClient portOneRestClient(PortOneProperties properties){

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000); // 5초 안에 연결 안 되면 포기
            factory.setReadTimeout(30000);   // 연결 후 30초 안에 응답 안 오면 포기

            return RestClient.builder()
                    .requestFactory(factory)
                    .baseUrl(properties.getApi().getBaseUrl())  // https://api.portone.io
                    .defaultHeader("Content-Type", "application/json")
                    .defaultHeader("Authorization","PortOne "+ properties.getApi().getSecret())
                    .build();
    }


}
