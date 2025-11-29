package com.codeit.playlist.domain.watching.dto.data;

import java.util.List;

public record RawWatchingSessionPage(
        List<RawWatchingSession> raws,
        boolean hasNext
) {
}
