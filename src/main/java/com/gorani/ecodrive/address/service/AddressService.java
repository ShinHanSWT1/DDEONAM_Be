package com.gorani.ecodrive.address.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gorani.ecodrive.address.dto.AddressSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class AddressService {

    private static final Logger log = LoggerFactory.getLogger(AddressService.class);

    @Value("${juso.confirm-key}")
    private String confirmKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<AddressSearchResponse> search(String keyword) throws Exception {
        // 1. URI 조합 (UriComponentsBuilder가 인코딩 자동 처리)ㅁ
        URI uri = UriComponentsBuilder
                .fromUriString("https://business.juso.go.kr/addrlink/addrLinkApi.do")
                .queryParam("currentPage", 1)
                .queryParam("countPerPage", 10)
                .queryParam("keyword", keyword)
                .queryParam("confmKey", confirmKey)
                .queryParam("resultType", "json")
                .build()
                .encode()
                .toUri();

        // 2. juso API 호출
        String response = restTemplate.getForObject(uri, String.class);

        // 3. JSON 파싱 → DTO 변환
        JsonNode root = objectMapper.readTree(response);
        JsonNode jusoList = root.path("results").path("juso");

        List<AddressSearchResponse> results = new ArrayList<>();
        if (jusoList.isArray()) {
            for (JsonNode juso : jusoList) {
                results.add(new AddressSearchResponse(
                        juso.path("roadAddr").asText(),
                        juso.path("jibunAddr").asText(),
                        juso.path("zipNo").asText(),
                        juso.path("bdNm").asText()
                ));
            }
        }

        return results;
    }
}