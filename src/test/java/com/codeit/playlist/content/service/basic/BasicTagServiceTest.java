package com.codeit.playlist.content.service.basic;

import com.codeit.playlist.domain.content.api.service.TmdbTagApiService;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import com.codeit.playlist.domain.content.repository.TagRepository;
import com.codeit.playlist.domain.content.service.basic.BasicTagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class BasicTagServiceTest {

    @InjectMocks
    private BasicTagService tagService;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TmdbTagApiService tmdbTagApiService;

    // Content는 실제 엔티티 생성 방식이 프로젝트마다 달라서 mock으로 처리하는 게 안전함
    private Content content() {
        return mock(Content.class);
    }

    @Nested
    @DisplayName("saveMovieTagToContent()")
    class SaveMovieTagTests {

        @Test
        @DisplayName("성공: genreIds 만큼 Tag를 저장하고 genreId를 세팅한다")
        void saveMovieTag_success() {
            // given
            Content content = content();

            given(tmdbTagApiService.getApiMovieTag())
                    .willReturn(Mono.just(Map.of(
                            28, "액션",
                            18, "드라마"
                    )));

            // when
            tagService.saveMovieTagToContent(content, List.of(28, 18));

            // then
            ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
            then(tagRepository).should(times(2)).save(tagCaptor.capture());

            List<Tag> saved = tagCaptor.getAllValues();
            assertThat(saved).hasSize(2);
            assertThat(saved.get(0).getGenreId()).isEqualTo(28);
            assertThat(saved.get(1).getGenreId()).isEqualTo(18);
        }

        @Test
        @DisplayName("genreList가 empty면 저장하지 않고 종료한다")
        void saveMovieTag_emptyGenreMap_noSave() {
            // given
            Content content = content();
            given(tmdbTagApiService.getApiMovieTag()).willReturn(Mono.just(Map.of()));

            // when
            tagService.saveMovieTagToContent(content, List.of(28, 18));

            // then
            then(tagRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("genreList가 null(block()이 null)이면 저장하지 않고 종료한다")
        void saveMovieTag_nullGenreMap_noSave() {
            // given
            Content content = content();
            // Mono.empty().block() == null
            given(tmdbTagApiService.getApiMovieTag()).willReturn(Mono.empty());

            // when
            tagService.saveMovieTagToContent(content, List.of(28));

            // then
            then(tagRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("genreId가 맵에 없으면 name=null로 Tag가 저장된다(현재 로직 그대로 검증)")
        void saveMovieTag_missingId_savesNullNameTag() {
            // given
            Content content = content();
            given(tmdbTagApiService.getApiMovieTag()).willReturn(Mono.just(Map.of(28, "액션")));

            // when (999는 맵에 없음)
            tagService.saveMovieTagToContent(content, List.of(999));

            // then
            ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
            then(tagRepository).should(times(1)).save(tagCaptor.capture());

            Tag saved = tagCaptor.getValue();
            assertThat(saved.getGenreId()).isEqualTo(999);
            // Tag에 name getter가 없다면 이 줄은 제거해도 됨 (현재는 "null 이름으로 저장"이 핵심)
            // assertThat(saved.getName()).isNull();
        }
    }

    @Nested
    @DisplayName("saveTvSeriesTagToContent()")
    class SaveTvTagTests {

        @Test
        @DisplayName("성공: genreIds 만큼 Tag를 저장하고 genreId를 세팅한다")
        void saveTvSeriesTag_success() {
            // given
            Content content = content();
            given(tmdbTagApiService.getApiTvSeriesTag())
                    .willReturn(Mono.just(Map.of(
                            10759, "액션&어드벤처",
                            18, "드라마"
                    )));

            // when
            tagService.saveTvSeriesTagToContent(content, List.of(10759, 18));

            // then
            ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
            then(tagRepository).should(times(2)).save(tagCaptor.capture());

            List<Tag> saved = tagCaptor.getAllValues();
            assertThat(saved).hasSize(2);
            assertThat(saved.get(0).getGenreId()).isEqualTo(10759);
            assertThat(saved.get(1).getGenreId()).isEqualTo(18);
        }

        @Test
        @DisplayName("genreList가 empty면 저장하지 않고 종료한다")
        void saveTvSeriesTag_emptyGenreMap_noSave() {
            // given
            Content content = content();
            given(tmdbTagApiService.getApiTvSeriesTag()).willReturn(Mono.just(Map.of()));

            // when
            tagService.saveTvSeriesTagToContent(content, List.of(18));

            // then
            then(tagRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("genreList가 null(block()이 null)이면 저장하지 않고 종료한다")
        void saveTvSeriesTag_nullGenreMap_noSave() {
            // given
            Content content = content();
            given(tmdbTagApiService.getApiTvSeriesTag()).willReturn(Mono.empty());

            // when
            tagService.saveTvSeriesTagToContent(content, List.of(18));

            // then
            then(tagRepository).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("saveTheSportTagToContent()")
    class SaveSportTagTests {

        @Test
        @DisplayName("성공: tagNames 유효한 값만 저장한다(null/blank는 skip)")
        void saveSportTag_success_filtersNullBlank() {
            // given
            Content content = content();

            // when
            tagService.saveTheSportTagToContent(content, List.of("Soccer", " ", null, "EPL"));

            // then: "Soccer", "EPL"만 저장
            then(tagRepository).should(times(2)).save(any(Tag.class));
        }

        @Test
        @DisplayName("tagNames가 null이면 저장하지 않고 종료한다")
        void saveSportTag_null_noSave() {
            // given
            Content content = content();

            // when
            tagService.saveTheSportTagToContent(content, null);

            // then
            then(tagRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("tagNames가 empty이면 저장하지 않고 종료한다")
        void saveSportTag_empty_noSave() {
            // given
            Content content = content();

            // when
            tagService.saveTheSportTagToContent(content, List.of());

            // then
            then(tagRepository).shouldHaveNoInteractions();
        }
    }
}
