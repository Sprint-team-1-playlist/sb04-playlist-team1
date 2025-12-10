package com.codeit.playlist.content.service.api.service;

import com.codeit.playlist.domain.content.api.service.TmdbTagApiService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

public class ContentApiServiceTest {

    @Test
    void apiTagResponseSuccessTest() {
        // given
        TmdbTagApiService tmdbTagApiService = Mockito.mock(TmdbTagApiService.class);

        Map<Integer, String> testMap = Map.of(
          28, "액션",
          12,"모험",
          16,"애니메이션",
          35, "코미디",
          80, "범죄"
        );

        Mockito.when(tmdbTagApiService.getApiMovieTag()).thenReturn(Mono.just(testMap));

        // when
        Map<Integer, String> result = tmdbTagApiService.getApiMovieTag().block();

        // then
        assertEquals("액션", result.get(28));
        assertEquals("모험", result.get(12));
        assertEquals("애니메이션", result.get(16));
        assertEquals("코미디", result.get(35));
        assertEquals("범죄", result.get(80));
    }
}
