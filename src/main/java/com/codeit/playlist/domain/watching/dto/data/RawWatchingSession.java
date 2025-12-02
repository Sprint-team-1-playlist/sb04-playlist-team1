package com.codeit.playlist.domain.watching.dto.data;

import java.util.UUID;

/*
 * Redis에 저장된 WatchingSession 데이터를 DB Entity 없이 읽어오기 위한 내부 데이터 모델
 */

public record RawWatchingSession(
        UUID watchingId,
        UUID contentId,
        UUID userId,
        long createdAtEpoch
) {
}
