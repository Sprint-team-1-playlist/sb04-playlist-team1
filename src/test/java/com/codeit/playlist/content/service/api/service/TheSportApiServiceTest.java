package com.codeit.playlist.content.service.api.service;

import com.codeit.playlist.domain.content.api.response.TheSportListResponse;
import com.codeit.playlist.domain.content.api.response.TheSportResponse;
import com.codeit.playlist.domain.content.api.service.TheSportApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TheSportApiServiceTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    @DisplayName("성공: events가 있으면 Flux로 방출하고 dateEvent null/blank는 필터링된다")
    void getApiSport_success_filtersInvalidDateEvent() throws Exception {
        // given
        TheSportResponse e1 = new TheSportResponse(
                "1", "A vs B", "file1", "Soccer",
                "A", "B", "2025-12-20", "p1"
        );
        TheSportResponse e2 = new TheSportResponse(
                "2", "C vs D", "file2", "Soccer",
                "C", "D", "", "p2"       // blank → 제거
        );
        TheSportResponse e3 = new TheSportResponse(
                "3", "E vs F", "file3", "Soccer",
                "E", "F", null, "p3"     // null → 제거
        );
        TheSportResponse e4 = new TheSportResponse(
                "4", "G vs H", "file4", "Soccer",
                "G", "H", "2025-12-21", "p4"
        );

        TheSportListResponse body =
                new TheSportListResponse(List.of(e1, e2, e3, e4));

        WebClient webClient = webClientStub(req -> {
            // season 검증: 2025-12 → 2025-2026
            assertThat(req.url().toString()).contains("s=2025-2026");
            assertThat(req.url().toString()).contains("id=4328");
            try {
                return okJson(body);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TheSportApiService service = new TheSportApiService(webClient);
        ReflectionTestUtils.setField(service, "apiKey", "dummy-key");

        // when & then
        StepVerifier.create(service.getApiSport(2025, 12))
                .expectNext(e1)
                .expectNext(e4)
                .verifyComplete();
    }

    @Test
    @DisplayName("events가 null이면 Flux.empty()")
    void getApiSport_eventsNull_empty() throws Exception {
        // given
        TheSportListResponse body = new TheSportListResponse(null);

        AtomicInteger calls = new AtomicInteger(0);
        WebClient webClient = webClientStub(req -> {
            calls.incrementAndGet();
            try {
                return okJson(body);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TheSportApiService service = new TheSportApiService(webClient);
        ReflectionTestUtils.setField(service, "apiKey", "dummy-key");

        // when & then
        StepVerifier.create(service.getApiSport(2025, 12))
                .verifyComplete();

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("events가 empty면 Flux.empty()")
    void getApiSport_eventsEmpty_empty() throws Exception {
        // given
        TheSportListResponse body = new TheSportListResponse(List.of());

        WebClient webClient = webClientStub(req -> {
            try {
                return okJson(body);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TheSportApiService service = new TheSportApiService(webClient);
        ReflectionTestUtils.setField(service, "apiKey", "dummy-key");

        // when & then
        StepVerifier.create(service.getApiSport(2025, 12))
                .verifyComplete();
    }

    @Test
    @DisplayName("season 계산: 8월 이상이면 YYYY-(YYYY+1)")
    void season_augustOrLater() throws Exception {
        // given: 2025-08 → 2025-2026
        TheSportListResponse body = new TheSportListResponse(List.of(
                new TheSportResponse(
                        "1", "A vs B", "f", "Soccer",
                        "A", "B", "2025-08-10", "p"
                )
        ));

        WebClient webClient = webClientStub(req -> {
            assertThat(req.url().toString()).contains("s=2025-2026");
            try {
                return okJson(body);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TheSportApiService service = new TheSportApiService(webClient);
        ReflectionTestUtils.setField(service, "apiKey", "dummy-key");

        StepVerifier.create(service.getApiSport(2025, 8))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("season 계산: 7월 이하면 (YYYY-1)-YYYY")
    void season_julyOrEarlier() throws Exception {
        // given: 2025-07 → 2024-2025
        TheSportListResponse body = new TheSportListResponse(List.of(
                new TheSportResponse(
                        "1", "A vs B", "f", "Soccer",
                        "A", "B", "2025-07-10", "p"
                )
        ));

        WebClient webClient = webClientStub(req -> {
            assertThat(req.url().toString()).contains("s=2024-2025");
            try {
                return okJson(body);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TheSportApiService service = new TheSportApiService(webClient);
        ReflectionTestUtils.setField(service, "apiKey", "dummy-key");

        StepVerifier.create(service.getApiSport(2025, 7))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("429 TooManyRequests면 onErrorResume로 빈 Flux 반환")
    void getApiSport_429_returnsEmpty() {
        // given
        WebClient webClient = webClientStub(req ->
                Mono.just(ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS).build())
        );

        TheSportApiService service = new TheSportApiService(webClient);
        ReflectionTestUtils.setField(service, "apiKey", "dummy-key");

        // when & then
        StepVerifier.create(service.getApiSport(2025, 12))
                .verifyComplete();
    }

    // ----------------------------------------------------------------
    // WebClient Stub Helpers
    // ----------------------------------------------------------------

    private WebClient webClientStub(ExchangeFunction fn) {
        return WebClient.builder().exchangeFunction(fn).build();
    }

    private Mono<ClientResponse> okJson(Object bodyObj) throws Exception {
        String json = om.writeValueAsString(bodyObj);
        return Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json) // 🔥 반드시 String
                        .build()
        );
    }
}
