package com.ember.payroll.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ConversionService {
    private final RestTemplate restTemplate;

    private final ObjectMapper mapper;

    @Value("${currency-api}")
    private String CURRENCY_API_URL;

    public ConversionService(RestTemplateBuilder restTemplateBuilder, ObjectMapper mapper) {
        this.restTemplate = restTemplateBuilder.build();
        this.mapper = mapper;
    }

    public double USDtoTRY(double usd) {
        ResponseEntity<String> response = restTemplate.getForEntity(CURRENCY_API_URL, String.class);
        try {
            double rate = mapper.readTree(response.getBody()).path("try").asDouble();
            return usd * rate;
        } catch(JsonProcessingException e) {
            e.printStackTrace();
        }

        return -1;
    }
}
