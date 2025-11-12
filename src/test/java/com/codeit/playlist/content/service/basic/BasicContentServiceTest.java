package com.codeit.playlist.content.service.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.dto.request.ContentCreateRequest;
import com.codeit.playlist.domain.content.entity.Contents;
import com.codeit.playlist.domain.content.entity.Type;
import com.codeit.playlist.domain.content.mapper.ContentMapper;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.content.service.basic.BasicContentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class BasicContentServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentMapper contentMapper;

    @InjectMocks
    private BasicContentService contentService;

    Contents contents = new Contents(
            Type.MOVIE,
            "미소된장국으로 건배",
            "재밌음",
            "exampleUrl",
            "러브코미디",
            2,
            3,
            4);

    ContentDto contentDto = new ContentDto(
            UUID.randomUUID(),
            "MOVIE",
            "미소된장국으로 건배",
            "재밌음",
            "exampleUrl",
            List.of("러브코미디"),
            2.0,
            3,
            4
    );

    @Test
    void createContentsSuccess() {
        // given
        ContentCreateRequest request = new ContentCreateRequest(
                "MOVIE",
                "미소된장국으로 건배",
                "재밌음",
                List.of("러브코미디", "순정만화"));
        given(contentMapper.toDto(any(Contents.class))).willReturn(contentDto);

        // when
        ContentDto result = contentService.create(request);

        // then
        assertThat(result).isEqualTo(contentDto);
        verify(contentRepository).save(any(Contents.class));
    }
}