package com.codeit.playlist.watching.fixture;

import static org.mockito.Mockito.mock;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.watching.dto.data.ChangeType;
import com.codeit.playlist.domain.watching.dto.data.WatchingSessionDto;
import com.codeit.playlist.domain.watching.dto.response.WatchingSessionChange;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;

public class WatchingSessionFixtures {
    public static final UUID FIXED_ID = UUID.randomUUID();
    public static final LocalDateTime FIXED_TIME = LocalDateTime.now();

    public static Content content() {
        return mock(Content.class);
    }

    public static ContentDto contentDto() {
        return new ContentDto(
                FIXED_ID,
                "type",
                "title",
                "description",
                "thumbnailUrl",
                tags(),
                3.5,
                3,
                3
        );
    }

    public static User user() {
        return mock(User.class);
    }

    public static UserDto userDto() {
        return new UserDto(
                FIXED_ID,
                FIXED_TIME,
                "email@test.com",
                "name",
                "profileImageUrl",
                Role.ADMIN,
                false
        );
    }

    public static List<String> tags() {
        return List.of("tag1", "tag2");
    }

    public static List<Tag> tagList() {
        return List.of(mock(Tag.class));
    }

    public static WatchingSessionDto watchingSessionDto() {
        return new WatchingSessionDto(
                FIXED_ID,
                FIXED_TIME,
                userDto(),
                contentDto()
        );
    }

    public static WatchingSessionChange watchingSessionChange() {
        return new WatchingSessionChange(
                ChangeType.JOIN,
                watchingSessionDto(),
                3
        );
    }
}
