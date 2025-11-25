package com.codeit.playlist.domain.content.api.handler;

import com.codeit.playlist.domain.content.api.response.TheSportsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TheSportsDateHandler {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

//    @Value("${api.sportsdb.key}")
//    private String apiKey;

    public TheSportsDateHandler(@Qualifier("theSportsClient") WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public List<TheSportsResponse> getSportsEvent(LocalDate localDate) {
        log.info("Sports API 수집 시작 : localDate = {}", localDate);

        String sportsJson = webClient.get()
                .uri("/123/eventsday.php?d=" + localDate)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if(sportsJson == null || sportsJson.isEmpty()) { // 비어있다면 리스트 반환
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(sportsJson); // 예외필요
            JsonNode jsonEvent = root.get("events");

            if(jsonEvent == null || !jsonEvent.isArray()) { // 비어있다면 리스트 반환
                return List.of();
            }

            List<TheSportsResponse> sportsList = new ArrayList<>(); // 반환할것 생성

            for(int i = 0; i < jsonEvent.size(); i++) {
                JsonNode eventSize = jsonEvent.get(i);

                String strEvent = eventSize.path("strEvent").asText(null);
                String strFileName = eventSize.path("strFilename").asText(null);
                String dateEventLocal = eventSize.path("dateEventLocal").asText(null);
                String strThumb = eventSize.path("strThumb").asText(null);

                TheSportsResponse response = new TheSportsResponse(
                        strEvent, strFileName, dateEventLocal, strThumb
                );
                log.info("썸네일 : {}", response.strThumb());
                sportsList.add(response);
            }
            log.info("Sports API 수집 완료, localDate = {}", localDate);
            return sportsList;

        } catch (JsonProcessingException e) {
            log.error("Sports API 단일 날짜 수집 실패, localDate = {}", localDate, e);
            return List.of();
        }
    }
}
