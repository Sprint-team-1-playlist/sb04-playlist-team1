package com.codeit.playlist.content.service.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.inOrder;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.dto.request.ContentCreateRequest;
import com.codeit.playlist.domain.content.dto.request.ContentCursorRequest;
import com.codeit.playlist.domain.content.dto.request.ContentUpdateRequest;
import com.codeit.playlist.domain.content.dto.response.CursorResponseContentDto;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import com.codeit.playlist.domain.content.exception.ContentBadRequestException;
import com.codeit.playlist.domain.content.exception.ContentNotFoundException;
import com.codeit.playlist.domain.content.mapper.ContentMapper;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.content.repository.TagRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.codeit.playlist.domain.content.service.basic.BasicContentService;
import com.codeit.playlist.domain.file.S3Uploader;
import com.codeit.playlist.domain.playlist.repository.PlaylistContentRepository;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.review.repository.ReviewRepository;
import com.codeit.playlist.domain.watching.repository.RedisWatchingSessionRepository;
import com.codeit.playlist.global.constant.S3Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class BasicContentServiceTest {

    @InjectMocks
    private BasicContentService contentService;

    @Mock
    private ContentRepository contentRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private ContentMapper contentMapper;
    @Mock
    private S3Uploader s3Uploader;
    @Mock
    private S3Properties s3Properties;
    @Mock
    private RedisWatchingSessionRepository redisWatchingSessionRepository;
    @Mock
    private PlaylistRepository playlistRepository;
    @Mock
    private PlaylistContentRepository playlistContentRepository;
    @Mock
    private ReviewRepository reviewRepository;

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("성공: 썸네일 업로드 + Content 저장 + Tag 저장 + DTO 매핑")
        void create_success() {
            // given
            given(s3Properties.getContentBucket()).willReturn("content-bucket");

            ContentCreateRequest request = new ContentCreateRequest(
                    "movie",
                    "테스트 제목",
                    "테스트 설명",
                    List.of("액션", " 드라마 ", "", "액션") // trim + empty 제거 + distinct 기대
            );

            MultipartFile thumbnail = new MockMultipartFile(
                    "thumbnail",
                    "thumb.jpg",
                    "image/jpeg",
                    "fake-image-bytes".getBytes(StandardCharsets.UTF_8)
            );

            // save()는 리턴 안 써도 되지만 호출 검증 위해 그대로 둠
            willAnswer(inv -> inv.getArgument(0)).given(contentRepository).save(any(Content.class));

            ContentDto mappedDto = mock(ContentDto.class);
            given(contentMapper.toDtoUsingS3(any(Content.class), anyList(), eq(s3Properties)))
                    .willReturn(mappedDto);

            // when
            ContentDto result = contentService.create(request, thumbnail);

            // then
            assertThat(result).isSameAs(mappedDto);

            // S3 업로드 호출 검증 (key는 내부 생성이라 "값이 존재" 정도만 검증)
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            then(s3Uploader).should(times(1))
                    .upload(eq("content-bucket"), keyCaptor.capture(), eq(thumbnail));
            assertThat(keyCaptor.getValue()).isNotBlank();

            // Content 저장 검증
            ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
            then(contentRepository).should(times(1)).save(contentCaptor.capture());
            Content saved = contentCaptor.getValue();
            assertThat(saved.getType()).isEqualTo("movie");
            assertThat(saved.getTitle()).isEqualTo("테스트 제목");
            assertThat(saved.getDescription()).isEqualTo("테스트 설명");
            assertThat(saved.getThumbnailUrl()).isNotBlank();

            // Tag 저장: ["액션","드라마"] 2개만 저장되는지
            ArgumentCaptor<List<Tag>> tagsCaptor = ArgumentCaptor.forClass(List.class);
            then(tagRepository).should(times(1)).saveAll(tagsCaptor.capture());
            List<Tag> savedTags = tagsCaptor.getValue();
            assertThat(savedTags).extracting(Tag::getName).containsExactlyInAnyOrder("액션", "드라마");

            // DTO 매핑 호출
            then(contentMapper).should(times(1))
                    .toDtoUsingS3(any(Content.class), anyList(), eq(s3Properties));
        }

        @Test
        @DisplayName("실패: type이 null이면 ContentBadRequestException")
        void create_fail_typeNull() {
            // given
            ContentCreateRequest request = new ContentCreateRequest(
                    null, "t", "d", List.of("tag")
            );
            MultipartFile thumbnail = new MockMultipartFile(
                    "thumbnail", "a.jpg", "image/jpeg", "x".getBytes(StandardCharsets.UTF_8)
            );

            // when & then
            assertThatThrownBy(() -> contentService.create(request, thumbnail))
                    .isInstanceOf(ContentBadRequestException.class)
                    .hasMessageContaining("컨텐츠에 대한 잘못된 요청입니다."); // ✅ 실제 메시지 기준

            then(contentRepository).shouldHaveNoInteractions();
            then(tagRepository).shouldHaveNoInteractions();
            then(s3Uploader).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("실패: thumbnail이 null이면 ContentBadRequestException")
        void create_fail_thumbnailNull() {
            // given
            ContentCreateRequest request = new ContentCreateRequest(
                    "movie",
                    "t",
                    "d",
                    List.of("tag")
            );

            // when & then
            assertThatThrownBy(() -> contentService.create(request, null))
                    .isInstanceOf(ContentBadRequestException.class)
                    .hasMessageContaining("컨텐츠에 대한 잘못된 요청입니다.");

            then(contentRepository).shouldHaveNoInteractions();
            then(tagRepository).shouldHaveNoInteractions();
            then(s3Uploader).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("실패: thumbnail이 empty면 ContentBadRequestException")
        void create_fail_thumbnailEmpty() {
            // given
            ContentCreateRequest request = new ContentCreateRequest(
                    "movie",
                    "t",
                    "d",
                    List.of("tag")
            );
            MultipartFile empty = new MockMultipartFile(
                    "thumbnail", "a.jpg", "image/jpeg", new byte[0]
            );

            // when & then
            assertThatThrownBy(() -> contentService.create(request, empty))
                    .isInstanceOf(ContentBadRequestException.class)
                    .hasMessageContaining("컨텐츠에 대한 잘못된 요청입니다.");

            then(contentRepository).shouldHaveNoInteractions();
            then(tagRepository).shouldHaveNoInteractions();
            then(s3Uploader).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("성공: 새 썸네일 있으면 upload 호출 + 태그 싹 지우고 새로 저장 + DTO 반환")
        void update_success_withNewThumbnail() {
            // given
            UUID contentId = UUID.randomUUID();
            Content content = new Content(
                    1L,
                    "movie",
                    "old title",
                    "old desc",
                    "old-key.jpg",
                    0,
                    0,
                    0
            );

            given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
            given(s3Properties.getContentBucket()).willReturn("content-bucket");

            // 기존 태그 존재
            List<Tag> oldTags = List.of(new Tag(content, "old"));
            given(tagRepository.findByContentId(contentId)).willReturn(oldTags);

            ContentUpdateRequest request = new ContentUpdateRequest(
                    "new title",
                    "new desc",
                    List.of(" 액션 ", "드라마", "액션")
            );

            MultipartFile newThumbnail = new MockMultipartFile(
                    "thumbnail", "new.jpg", "image/jpeg", "new-image".getBytes(StandardCharsets.UTF_8)
            );

            ContentDto mappedDto = mock(ContentDto.class);
            given(contentMapper.toDtoUsingS3(any(Content.class), anyList(), eq(s3Properties)))
                    .willReturn(mappedDto);

            // when
            ContentDto result = contentService.update(contentId, request, newThumbnail);

            // then
            assertThat(result).isSameAs(mappedDto);

            // 썸네일 업로드 1회
            then(s3Uploader).should(times(1))
                    .upload(eq("content-bucket"), anyString(), eq(newThumbnail));

            // 기존 태그 삭제 후 새 태그 저장
            then(tagRepository).should(times(1)).deleteAll(oldTags);

            ArgumentCaptor<List<Tag>> newTagsCaptor = ArgumentCaptor.forClass(List.class);
            then(tagRepository).should(times(1)).saveAll(newTagsCaptor.capture());
            assertThat(newTagsCaptor.getValue())
                    .extracting(Tag::getName)
                    .containsExactlyInAnyOrder("액션", "드라마");

            // 엔티티 값이 갱신되었는지(메서드 구현에 따라 필드 확인)
            assertThat(content.getTitle()).isEqualTo("new title");
            assertThat(content.getDescription()).isEqualTo("new desc");
            assertThat(content.getThumbnailUrl()).isNotBlank();

            then(contentMapper).should(times(1))
                    .toDtoUsingS3(eq(content), anyList(), eq(s3Properties));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 contentId면 ContentNotFoundException")
        void update_fail_notFound() {
            // given
            UUID contentId = UUID.randomUUID();
            given(contentRepository.findById(contentId)).willReturn(Optional.empty());

            ContentUpdateRequest request = new ContentUpdateRequest(
                    "t",
                    "d",
                    List.of("tag")
            );

            // when & then
            assertThatThrownBy(() -> contentService.update(contentId, request, null))
                    .isInstanceOf(ContentNotFoundException.class);

            then(tagRepository).shouldHaveNoInteractions();
            then(s3Uploader).shouldHaveNoInteractions();
            then(contentMapper).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("성공: 존재하면 리뷰->플리컨텐츠->태그 삭제 후 컨텐츠 삭제")
        void delete_success() {
            // given
            UUID contentId = UUID.randomUUID();
            given(contentRepository.existsById(contentId)).willReturn(true);

            // when
            contentService.delete(contentId);

            // then (호출 순서까지 검증)
            InOrder inOrder = inOrder(contentRepository, reviewRepository, playlistContentRepository, tagRepository);

            inOrder.verify(contentRepository).existsById(contentId);
            inOrder.verify(reviewRepository).deleteAllByContent_Id(contentId);
            inOrder.verify(playlistContentRepository).deleteAllByContent_Id(contentId);
            inOrder.verify(tagRepository).deleteAllByContentId(contentId);
            inOrder.verify(contentRepository).deleteById(contentId);

            // 불필요한 추가 호출 방지 (선택)
            then(reviewRepository).shouldHaveNoMoreInteractions();
            then(playlistContentRepository).shouldHaveNoMoreInteractions();
            then(tagRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("실패: 존재하지 않으면 ContentNotFoundException + 아무것도 삭제 안 함")
        void delete_fail_notFound() {
            // given
            UUID contentId = UUID.randomUUID();
            given(contentRepository.existsById(contentId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> contentService.delete(contentId))
                    .isInstanceOf(ContentNotFoundException.class);

            then(reviewRepository).shouldHaveNoInteractions();
            then(playlistContentRepository).shouldHaveNoInteractions();
            then(tagRepository).shouldHaveNoInteractions();
            then(contentRepository).should(never()).deleteById(contentId);
        }
    }

    @Nested
    @DisplayName("get() - cursor pagination")
    class GetTests {

        @Test
        @DisplayName("성공: limit+1 조회 -> hasNext true, nextCursor/nextIdAfter 생성 (sortBy=watcherCount)")
        void get_success_hasNext_watcherCount() {
            // given
            ContentCursorRequest request = mock(ContentCursorRequest.class);
            given(request.limit()).willReturn(2);
            given(request.sortDirection()).willReturn(SortDirection.DESCENDING);
            given(request.sortBy()).willReturn("watcherCount");

            // ✅ UUID를 직접 주입 (List.of(null, ..) 방지)
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID id3 = UUID.randomUUID();

            Content c1 = new Content(1L, "movie", "t1", "d1", "k1", 0, 0, 0);
            Content c2 = new Content(2L, "movie", "t2", "d2", "k2", 0, 0, 0);
            Content c3 = new Content(3L, "movie", "t3", "d3", "k3", 0, 0, 0);

            // id 필드명이 보통 "id"라서 이렇게 박으면 됨
            ReflectionTestUtils.setField(c1, "id", id1);
            ReflectionTestUtils.setField(c2, "id", id2);
            ReflectionTestUtils.setField(c3, "id", id3);

            // searchContents는 limit+1(=3)개를 가져온다고 가정
            List<Content> found = List.of(c1, c2, c3);
            given(contentRepository.searchContents(eq(request), eq(false), eq(2), eq("watcherCount")))
                    .willReturn(found);

            // 태그: 실제 로직은 (firstTwoIds) 기준으로 findByContentIdIn 호출
            List<UUID> firstTwoIds = List.of(id1, id2);
            Tag t11 = new Tag(c1, "액션");
            Tag t21 = new Tag(c2, "드라마");

            given(tagRepository.findByContentIdIn(eq(firstTwoIds)))
                    .willReturn(List.of(t11, t21));

            // watcherCount: c1=1, c2=5
            given(redisWatchingSessionRepository.countWatchingSessionByContentId(id1)).willReturn(1L);
            given(redisWatchingSessionRepository.countWatchingSessionByContentId(id2)).willReturn(5L);

            ContentDto dto1 = mock(ContentDto.class);
            ContentDto dto2 = mock(ContentDto.class);
            given(contentMapper.toDtoUsingS3(eq(c1), anyList(), eq(s3Properties))).willReturn(dto1);
            given(contentMapper.toDtoUsingS3(eq(c2), anyList(), eq(s3Properties))).willReturn(dto2);

            // when
            CursorResponseContentDto res = contentService.get(request);

            // then
            assertThat(res.data()).containsExactly(dto1, dto2);
            assertThat(res.hasNext()).isTrue();
            assertThat(res.pageSize()).isEqualTo(2);
            assertThat(res.sortBy()).isEqualTo("watcherCount");
            assertThat(res.sortDirection()).isEqualTo("DESCENDING");

            // nextCursor는 "이번 페이지 마지막 요소(c2)의 watcherCount"
            assertThat(res.nextCursor()).isEqualTo("5");
            assertThat(res.nextIdAfter()).isEqualTo(id2.toString());
        }

        @Nested
        @DisplayName("search() - 단건 조회")
        class SearchTests {

            @Test
            @DisplayName("성공: content + tags 조회 후 DTO 매핑")
            void search_success() {
                // given
                UUID id = UUID.randomUUID();
                Content content = new Content(1L, "movie", "t", "d", "k", 0, 0, 0);
                // Content 엔티티 id가 생성 규칙상 생성자에서 바로 세팅되지 않는다면,
                // 이 테스트는 프로젝트 엔티티 구현에 맞게 조정 필요 (예: reflection으로 id 세팅)

                given(contentRepository.findById(id)).willReturn(Optional.of(content));
                given(tagRepository.findByContentId(content.getId())).willReturn(List.of(new Tag(content, "액션")));

                ContentDto dto = mock(ContentDto.class);
                given(contentMapper.toDtoUsingS3(eq(content), anyList(), eq(s3Properties))).willReturn(dto);

                // when
                ContentDto result = contentService.search(id);

                // then
                assertThat(result).isSameAs(dto);
                then(tagRepository).should(times(1)).findByContentId(content.getId());
                then(contentMapper).should(times(1)).toDtoUsingS3(eq(content), anyList(), eq(s3Properties));
            }
        }
    }
}