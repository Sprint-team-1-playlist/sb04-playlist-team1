package com.codeit.playlist.content.service.api.service;

import com.codeit.playlist.domain.content.api.response.TvSeriesListResponse;
import com.codeit.playlist.domain.content.api.response.TvSeriesResponse;
import com.codeit.playlist.domain.content.api.service.TvSeriesApiService;
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
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class TvSeriesApiServiceTest {
    private final ObjectMapper om = new ObjectMapper();

    @Test
    @DisplayName("totalPages<=1이면 1페이지만 호출하고 results만 방출한다")
    void getApiTv_totalPages1_onlyFirstPage() throws Exception {
        // given
        TvSeriesResponse r1 = new TvSeriesResponse(1L, "tv1", "d1", "/p1.jpg", List.of(1, 2));
        TvSeriesResponse r2 = new TvSeriesResponse(2L, "tv2", "d2", "/p2.jpg", List.of(3));

        TvSeriesListResponse page1 = new TvSeriesListResponse(
                List.of(r1, r2),
                1,
                1, // totalPages
                2
        );

        AtomicInteger callCount = new AtomicInteger(0);

        WebClient webClient = webClientStub(req -> {
            callCount.incrementAndGet();
            // page=1만 와야 함
            assertThat(req.url().toString()).contains("page=1");
            try {
                return okJson(page1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TvSeriesApiService service = new TvSeriesApiService(webClient);
        ReflectionTestUtils.setField(service, "apikey", "dummy-key");

        // when & then
        StepVerifier.create(service.getApiTv("language=ko-KR"))
                .expectNext(r1)
                .expectNext(r2)
                .verifyComplete();

        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("totalPages=3이면 page=1~3 호출하고 concat 순서로 방출한다")
    void getApiTv_totalPages3_pages1to3() throws Exception {
        // given
        TvSeriesResponse p1r = new TvSeriesResponse(1L, "p1", "d", "/1.jpg", List.of());
        TvSeriesResponse p2r = new TvSeriesResponse(2L, "p2", "d", "/2.jpg", List.of());
        TvSeriesResponse p3r = new TvSeriesResponse(3L, "p3", "d", "/3.jpg", List.of());

        TvSeriesListResponse page1 = new TvSeriesListResponse(List.of(p1r), 1, 3, 3);
        TvSeriesListResponse page2 = new TvSeriesListResponse(List.of(p2r), 2, 3, 3);
        TvSeriesListResponse page3 = new TvSeriesListResponse(List.of(p3r), 3, 3, 3);

        Map<Integer, TvSeriesListResponse> pages = Map.of(
                1, page1,
                2, page2,
                3, page3
        );

        AtomicInteger callCount = new AtomicInteger(0);

        WebClient webClient = webClientStub(req -> {
            callCount.incrementAndGet();
            int page = extractPage(req.url());
            try {
                return okJson(pages.get(page));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TvSeriesApiService service = new TvSeriesApiService(webClient);
        ReflectionTestUtils.setField(service, "apikey", "dummy-key");

        // when & then
        StepVerifier.create(service.getApiTv("language=ko-KR"))
                .expectNext(p1r)
                .expectNext(p2r)
                .expectNext(p3r)
                .verifyComplete();

        assertThat(callCount.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("totalPages가 maxPage(5)보다 크면 1~5 페이지만 호출한다")
    void getApiTv_totalPages10_limitTo5Pages() throws Exception {
        // given
        Map<Integer, TvSeriesResponse> resultByPage = new HashMap<>();
        Map<Integer, TvSeriesListResponse> bodyByPage = new HashMap<>();

        for (int p = 1; p <= 5; p++) {
            TvSeriesResponse r = new TvSeriesResponse((long) p, "t" + p, "d", "/" + p + ".jpg", List.of());
            resultByPage.put(p, r);
            bodyByPage.put(p, new TvSeriesListResponse(List.of(r), p, 10, 10));
        }

        AtomicInteger callCount = new AtomicInteger(0);
        Set<Integer> calledPages = ConcurrentHashMap.newKeySet();

        WebClient webClient = webClientStub(req -> {
            callCount.incrementAndGet();
            int page = extractPage(req.url());
            calledPages.add(page);
            try {
                return okJson(bodyByPage.get(page));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TvSeriesApiService service = new TvSeriesApiService(webClient);
        ReflectionTestUtils.setField(service, "apikey", "dummy-key");

        // when & then
        StepVerifier.create(service.getApiTv("language=ko-KR"))
                .expectNext(resultByPage.get(1))
                .expectNext(resultByPage.get(2))
                .expectNext(resultByPage.get(3))
                .expectNext(resultByPage.get(4))
                .expectNext(resultByPage.get(5))
                .verifyComplete();

        assertThat(callCount.get()).isEqualTo(5);
        assertThat(calledPages).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
        assertThat(calledPages).doesNotContain(6, 7, 8, 9, 10);
    }

    @Test
    @DisplayName("첫 페이지 results가 empty면 Flux.empty()로 종료한다 (추가 페이지 호출 없음)")
    void getApiTv_firstPageEmpty_returnsEmpty() throws Exception {
        // given
        TvSeriesListResponse page1 = new TvSeriesListResponse(List.of(), 1, 10, 0);

        AtomicInteger callCount = new AtomicInteger(0);
        WebClient webClient = webClientStub(req -> {
            callCount.incrementAndGet();
            try {
                return okJson(page1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TvSeriesApiService service = new TvSeriesApiService(webClient);
        ReflectionTestUtils.setField(service, "apikey", "dummy-key");

        // when & then
        StepVerifier.create(service.getApiTv("language=ko-KR"))
                .verifyComplete();

        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("429 발생 시 Retry.backoff로 재시도 후 성공한다 (가상시간)")
    void getApiTv_retryOn429_thenSuccess() throws Exception {
        // given
        TvSeriesResponse r1 = new TvSeriesResponse(1L, "tv1", "d1", "/p1.jpg", List.of());
        TvSeriesListResponse okBody = new TvSeriesListResponse(List.of(r1), 1, 1, 1);

        AtomicInteger attempts = new AtomicInteger(0);

        WebClient webClient = webClientStub(req -> {
            int n = attempts.incrementAndGet();
            if (n <= 2) {
                return statusJson(HttpStatus.TOO_MANY_REQUESTS, "{\"status_code\":429}");
            }
            try {
                return okJson(okBody);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TvSeriesApiService service = new TvSeriesApiService(webClient);
        ReflectionTestUtils.setField(service, "apikey", "dummy-key");

        // when & then
        StepVerifier.withVirtualTime(() -> service.getApiTv("language=ko-KR"))
                .thenAwait(Duration.ofSeconds(10)) // backoff 2s,4s 포함 넉넉히
                .expectNext(r1)
                .verifyComplete();

        assertThat(attempts.get()).isEqualTo(3);
    }

    // -------------------------
    // WebClient Stub Helpers
    // -------------------------

    private WebClient webClientStub(ExchangeFunction fn) {
        return WebClient.builder().exchangeFunction(fn).build();
    }

    private Mono<ClientResponse> okJson(Object bodyObj) throws Exception {
        String json = om.writeValueAsString(bodyObj);
        return statusJson(HttpStatus.OK, json);
    }

    private Mono<ClientResponse> statusJson(HttpStatus status, String json) {
        return Mono.just(
                ClientResponse.create(status)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json) // ✅ String으로!
                        .build()
        );
    }

    private int extractPage(URI uri) {
        String q = uri.getQuery(); // "api_key=...&language=ko-KR&page=3" 같은 형태
        if (q == null) return 1;

        for (String part : q.split("&")) {
            if (part.startsWith("page=")) {
                return Integer.parseInt(part.substring("page=".length()));
            }
        }

        int idx = q.indexOf("page=");
        if (idx >= 0) {
            String tail = q.substring(idx + 5);
            int amp = tail.indexOf('&');
            String pageStr = (amp >= 0) ? tail.substring(0, amp) : tail;
            return Integer.parseInt(pageStr);
        }
        return 1;
    }
}
