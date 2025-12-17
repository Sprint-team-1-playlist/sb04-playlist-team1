package com.codeit.playlist.content.service.api.service;

import com.codeit.playlist.domain.content.api.response.*;
import com.codeit.playlist.domain.content.api.service.TmdbTagApiService;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TmdbTagApiServiceTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    @DisplayName("getApiMovieTag: genres가 있으면 id->name Map으로 변환한다 + URI(path/language) 확인")
    void getApiMovieTag_success_mapsToMap_andChecksUri() throws Exception {
        // given
        TheMovieTagResponse g1 = new TheMovieTagResponse(28, "액션");
        TheMovieTagResponse g2 = new TheMovieTagResponse(18, "드라마");
        TheMovieTagListResponse body = new TheMovieTagListResponse(List.of(g1, g2));

        AtomicReference<String> calledUrl = new AtomicReference<>();

        WebClient webClient = webClientStub(req -> {
            calledUrl.set(req.url().toString());
            try {
                return okJson(body);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TmdbTagApiService service = new TmdbTagApiService(webClient);
        ReflectionTestUtils.setField(service, "apiKey", "dummy-key");

        // when & then
        StepVerifier.create(service.getApiMovieTag())
                .assertNext(map -> {
                    assertThat(map).containsEntry(28, "액션");
                    assertThat(map).containsEntry(18, "드라마");
                })
                .verifyComplete();

        // URI 검증
        assertThat(calledUrl.get()).contains("api.themoviedb.org");
        assertThat(calledUrl.get()).contains("/3/genre/movie/list");
        assertThat(calledUrl.get()).contains("language=ko-KR");
        assertThat(calledUrl.get()).contains("api_key=dummy-key");
    }

    @Test
    @DisplayName("getApiMovieTag: genres가 null이면 빈 Map을 반환한다")
    void getApiMovieTag_genresNull_returnsEmptyMap() throws Exception {
        // given
        TheMovieTagListResponse body = new TheMovieTagListResponse(null);

        WebClient webClient = webClientStub(req -> {
            try {
                return okJson(body);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TmdbTagApiService service = new TmdbTagApiService(webClient);
        ReflectionTestUtils.setField(service, "apiKey", "dummy-key");

        // when & then
        StepVerifier.create(service.getApiMovieTag())
                .assertNext(map -> assertThat(map).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("getApiMovieTag: 중복 genreId가 오면 첫 값(existing)을 유지한다")
    void getApiMovieTag_duplicateId_keepsExisting() throws Exception {
        // given
        TheMovieTagResponse first = new TheMovieTagResponse(28, "액션(첫값)");
        TheMovieTagResponse duplicate = new TheMovieTagResponse(28, "액션(나중값)");
        TheMovieTagListResponse body = new TheMovieTagListResponse(List.of(first, duplicate));

        WebClient webClient = webClientStub(req -> {
            try {
                return okJson(body);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TmdbTagApiService service = new TmdbTagApiService(webClient);
        ReflectionTestUtils.setField(service, "apiKey", "dummy-key");

        // when & then
        StepVerifier.create(service.getApiMovieTag())
                .assertNext(map -> assertThat(map.get(28)).isEqualTo("액션(첫값)"))
                .verifyComplete();
    }

    @Test
    @DisplayName("getApiTvSeriesTag: genres가 있으면 id->name Map으로 변환한다 + URI(path/language) 확인")
    void getApiTvSeriesTag_success_mapsToMap_andChecksUri() throws Exception {
        // given
        TvSeriesTagResponse g1 = new TvSeriesTagResponse(10759, "액션&어드벤처");
        TvSeriesTagResponse g2 = new TvSeriesTagResponse(18, "드라마");
        TvSeriesTagListResponse body = new TvSeriesTagListResponse(List.of(g1, g2));

        AtomicReference<String> calledUrl = new AtomicReference<>();

        WebClient webClient = webClientStub(req -> {
            calledUrl.set(req.url().toString());
            try {
                return okJson(body);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TmdbTagApiService service = new TmdbTagApiService(webClient);
        ReflectionTestUtils.setField(service, "apiKey", "dummy-key");

        // when & then
        StepVerifier.create(service.getApiTvSeriesTag())
                .assertNext(map -> {
                    assertThat(map).containsEntry(10759, "액션&어드벤처");
                    assertThat(map).containsEntry(18, "드라마");
                })
                .verifyComplete();

        // URI 검증
        assertThat(calledUrl.get()).contains("api.themoviedb.org");
        assertThat(calledUrl.get()).contains("/3/genre/tv/list");
        assertThat(calledUrl.get()).contains("language=ko-KR");
        assertThat(calledUrl.get()).contains("api_key=dummy-key");
    }

    @Test
    @DisplayName("getApiTvSeriesTag: genres가 null이면 빈 Map을 반환한다")
    void getApiTvSeriesTag_genresNull_returnsEmptyMap() throws Exception {
        // given
        TvSeriesTagListResponse body = new TvSeriesTagListResponse(null);

        WebClient webClient = webClientStub(req -> {
            try {
                return okJson(body);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TmdbTagApiService service = new TmdbTagApiService(webClient);
        ReflectionTestUtils.setField(service, "apiKey", "dummy-key");

        // when & then
        StepVerifier.create(service.getApiTvSeriesTag())
                .assertNext(map -> assertThat(map).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("getApiTvSeriesTag: 중복 id가 오면 첫 값(existing)을 유지한다")
    void getApiTvSeriesTag_duplicateId_keepsExisting() throws Exception {
        // given
        TvSeriesTagResponse first = new TvSeriesTagResponse(18, "드라마(첫값)");
        TvSeriesTagResponse duplicate = new TvSeriesTagResponse(18, "드라마(나중값)");
        TvSeriesTagListResponse body = new TvSeriesTagListResponse(List.of(first, duplicate));

        WebClient webClient = webClientStub(req -> {
            try {
                return okJson(body);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        TmdbTagApiService service = new TmdbTagApiService(webClient);
        ReflectionTestUtils.setField(service, "apiKey", "dummy-key");

        // when & then
        StepVerifier.create(service.getApiTvSeriesTag())
                .assertNext(map -> assertThat(map.get(18)).isEqualTo("드라마(첫값)"))
                .verifyComplete();
    }

    // -------------------------
    // WebClient Stub Helpers
    // -------------------------

    private WebClient webClientStub(ExchangeFunction fn) {
        return WebClient.builder().exchangeFunction(fn).build();
    }

    private Mono<ClientResponse> okJson(Object bodyObj) throws Exception {
        String json = om.writeValueAsString(bodyObj);
        return Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json) // ✅ String으로 넣어야 디코딩 OK
                        .build()
        );
    }
}