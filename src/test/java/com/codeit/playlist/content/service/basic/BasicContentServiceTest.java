package com.codeit.playlist.content.service.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.BDDMockito.given;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.dto.request.ContentCreateRequest;
import com.codeit.playlist.domain.content.dto.request.ContentUpdateRequest;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import com.codeit.playlist.domain.content.entity.Type;
import com.codeit.playlist.domain.content.mapper.ContentMapper;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.content.repository.TagRepository;
import com.codeit.playlist.domain.content.service.basic.BasicContentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class BasicContentServiceTest {

    @Mock
    ContentRepository contentRepository;

    @Mock
    TagRepository tagRepository;

    @Mock
    ContentMapper contentMapper;

    @InjectMocks
    BasicContentService contentService;

    @Test
    void createContentsSuccess() {
        // given
        ContentCreateRequest request = new ContentCreateRequest(
                "MOVIE",
                "미소된장국으로 건배",
                "재밌는만화",
                List.of("순정만화", "러브코미디")
        );

        ContentDto content = new ContentDto(
                UUID.randomUUID(),
                Type.MOVIE.toString(),
                "미소된장국으로 건배",
                "재밌는만화",
                "exampleUrl",
                List.of("순정만화","러브코미디"),
                2.0,
                3,
                4);

        String thumbnail = "testThumbnail.jpg";

        when(contentRepository.save(any(Content.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tagRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(contentMapper.toDto(any(Content.class), anyList()))
                .thenReturn(content);

        // when
        ContentDto result = contentService.create(request, thumbnail);

        // then
        assertThat(result).isEqualTo(content);
    }

    @Test
    void updateContentsSuccess() {
        // given
        UUID contentId = UUID.randomUUID();

        Content content = new Content(
                Type.MOVIE,
                "오가미 츠미키와 기일상",
                "매우 재밌는 만화",
                "exampleUrl",
                3.0,
                2,
                3
        );
        List<Tag> oldTag = List.of(
                new Tag(content,"러브코미디"),
                new Tag(content,"BoyMeetGirl")
        );

        ContentDto contentDto = new ContentDto(
                contentId,
                content.getType().toString(),
                content.getTitle(),
                content.getDescription(),
                content.getThumbnailUrl(),
                List.of("러브코미디"),
                content.getAverageRating(),
                content.getReviewCount(),
                content.getWatcherCount()
        );

        ContentUpdateRequest request = new ContentUpdateRequest(
                "미소된장국으로 건배",
                "재미있는 만화",
                List.of("순정만화", "러브코미디")
        );

        String thumbnail = "testThumbnail.jpg";

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(tagRepository.findByContentId(contentId)).thenReturn(oldTag);
        when(contentMapper.toDto(any(Content.class), anyList())).thenReturn(contentDto);

        // when
        ContentDto result = contentService.update(contentId, request, thumbnail);

        // then
        assertThat(content.getTitle()).isEqualTo("미소된장국으로 건배");
        assertThat(content.getDescription()).isEqualTo("재미있는 만화");
        assertThat(content.getThumbnailUrl()).isEqualTo(thumbnail);
    }

    @Test void deleteContentSuccess() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = new Content(
                Type.MOVIE,
                "오가미 츠미키와 기일상",
                "매우 재밌는 만화",
                "exampleUrl",
                3.0,
                2,
                3
        );
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));

        // when
        contentService.delete(contentId);

        // then
        verify(contentRepository).deleteById(contentId);
    }
}

//    @Mock
//    private ContentRepository contentRepository;
//
//    @Mock
//    private ContentMapper contentMapper;
//
//    @Mock
//    private TagRepository tagRepository;
//
//    @InjectMocks
//    private BasicContentService contentService;
//
//
//    ContentDto contentDto = new ContentDto(
//            UUID.randomUUID(),
//            "MOVIE",
//            "미소된장국으로 건배",
//            "재밌음",
//            "exampleUrl",
//            List.of("러브코미디"),
//            2.0,
//            3,
//            4
//    );
//
//    Tag tag1 = new Tag(content, "러브코미디");
//    Tag tag2 = new Tag(content, "순정만화");
//    List<Tag> tags = List.of(tag1, tag2);
//
//    @Test
//    void createContentsSuccess() {
//        // given
//        ContentCreateRequest request = new ContentCreateRequest(
//                "MOVIE",
//                "미소된장국으로 건배",
//                "재밌음",
//                List.of("러브코미디", "순정만화"));
//        String thumbnail = "thumbnail";
//        given(contentMapper.toDto(content, tags)).willReturn(contentDto);
//
//        // when
//        ContentDto result = contentService.create(request, thumbnail);
//
//        // then
//        assertThat(result).isEqualTo(contentDto);
//        verify(contentRepository).save(any(Content.class));
//    }
