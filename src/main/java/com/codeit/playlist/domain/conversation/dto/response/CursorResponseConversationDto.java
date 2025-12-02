package com.codeit.playlist.domain.conversation.dto.response;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.conversation.dto.data.ConversationDto;
import com.codeit.playlist.domain.conversation.dto.data.ConversationSortBy;
import java.util.List;
import java.util.UUID;

public record CursorResponseConversationDto(
    List<ConversationDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    ConversationSortBy sortBy,
    SortDirection sortDirection
) {

}
