package com.codeit.playlist.domain.playlist.service.basic;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.content.repository.TagRepository;
import com.codeit.playlist.domain.follow.repository.FollowRepository;
import com.codeit.playlist.domain.notification.dto.data.NotificationDto;
import com.codeit.playlist.domain.notification.entity.Level;
import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.playlist.domain.playlist.dto.response.CursorResponsePlaylistDto;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.exception.PlaylistAccessDeniedException;
import com.codeit.playlist.domain.playlist.exception.PlaylistNotFoundException;
import com.codeit.playlist.domain.playlist.mapper.PlaylistMapper;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.repository.SubscribeRepository;
import com.codeit.playlist.domain.playlist.service.PlaylistService;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.global.constant.S3Properties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BasicPlaylistService implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final SubscribeRepository subscribeRepository;
    private final UserRepository userRepository;
    private final PlaylistMapper playlistMapper;
    private final S3Properties s3Properties;
    private final FollowRepository followRepository;
    private final ObjectMapper objectMapper;                       // Kafka payload 직렬화
    private final KafkaTemplate<String, String> kafkaTemplate;    //  Kafka 발행
    private final TagRepository tagRepository;

    //플레이리스트 생성
    @Override
    public PlaylistDto createPlaylist(PlaylistCreateRequest request, UUID ownerId) {

        log.debug("[플레이리스트] 생성 요청: ownerId={}, title={}", ownerId, request.title());

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> UserNotFoundException.withId(ownerId));

        Playlist playlist = new Playlist(owner, request.title(), request.description());

        Playlist saved = playlistRepository.save(playlist);

        PlaylistDto dto = playlistMapper.toDto(saved, s3Properties);

        log.info("[플레이리스트] 생성 완료: id={}", dto.id());

        List<UUID> followerIds = followRepository.findFollowerIdsByFolloweeId(ownerId);

        if (!followerIds.isEmpty()) {
            String title = String.format("%s 님이 새 플레이리스트를 만들었어요.", owner.getName());
            String contentMsg = String.format("[ %s ] %s", saved.getTitle(), saved.getDescription());

            for (UUID followerId : followerIds) {
                NotificationDto notificationDto = new NotificationDto(null, null, followerId,
                                                                        title, contentMsg, Level.INFO);

                try {
                    String payload = objectMapper.writeValueAsString(notificationDto);
                    kafkaTemplate.send("playlist.NotificationDto", payload);
                } catch (JsonProcessingException e) {
                    log.error("[플레이리스트] 팔로워 알림 직렬화 실패: followerId= {}, playlistId= {}",
                            followerId, saved.getId(), e);
                }
            }

            log.info("[플레이리스트] 팔로우한 사용자들에게 새 플레이리스트 생성 알림 발행: ownerId= {}, followerCount= {}",
                    ownerId, followerIds.size());
        }

        return dto;
    }

    //플레이리스트 수정
    @Override
    public PlaylistDto updatePlaylist(UUID playlistId, PlaylistUpdateRequest request, UUID currentUserId) {
        log.debug("[플레이리스트] 수정 시작: playlistId= {}, currentUserId= {}", playlistId, currentUserId);

        //플레이리스트 조회
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> PlaylistNotFoundException.withId(playlistId));

        //3. 소유자 검증
        UUID ownerId = playlist.getOwner().getId();

        log.debug("[플레이리스트] 소유자 검증: ownerId= {}, currentUserId= {}", ownerId, currentUserId);

        if (!ownerId.equals(currentUserId)) {
            throw PlaylistAccessDeniedException.withIds(playlist.getId(), ownerId, currentUserId);
        }

        playlist.updateInfo(request.title(), request.description());

        log.info("[플레이리스트] 수정 성공: playlistId= {}", playlistId);

        return playlistMapper.toDto(playlist, s3Properties);
    }

    //플레이리스트 논리 삭제
    @Override
    public void softDeletePlaylist(UUID playlistId, UUID requesterUserId) {
        log.debug("[플레이리스트] 삭제 시작 : playlistId= {}, requesterUserId= {}", playlistId, requesterUserId);

        //삭제되지 않은 플레이리스트 조회
        Playlist playlist = playlistRepository.findByIdAndDeletedAtIsNull(playlistId)
                .orElseThrow(() -> {
                    log.error("[플레이리스트] 삭제 실패: 존재하지 않거나 이미 삭제됨 playlistId= {}", playlistId);
                    return PlaylistNotFoundException.withId(playlistId);
                });

        //소유자 검증
        UUID ownerId = playlist.getOwner().getId();
        if (!ownerId.equals(requesterUserId)) {
            log.error("[플레이리스트] 논리 삭제 실패: 권한 없음 playlistId= {}, ownerId= {}, requester= {}",
                    playlistId, ownerId, requesterUserId);
            throw PlaylistAccessDeniedException.withPlaylistId(playlistId);
        }

        int deleted = playlistRepository.softDeleteById(playlistId);
        if (deleted == 0) {
            throw PlaylistNotFoundException.withId(playlistId);
        }

        log.info("[플레이리스트] 논리 삭제 성공 playlistId= {}, requesterUserId= {}",
                playlistId, requesterUserId);
    }

    //플레이리스트 일반 삭제(논리 삭제 호출)
    @Override
    public void deletePlaylist(UUID playlistId, UUID requesterUserId) {
        log.debug("[플레이리스트] 삭제 시작 : playlistId = {}, requesterUserId = {}", playlistId, requesterUserId);
        softDeletePlaylist(playlistId, requesterUserId);
    }

    //플레이리스트 목록 조회
    @Transactional(readOnly = true)
    @Override
    public CursorResponsePlaylistDto findPlaylists(String keywordLike, UUID ownerIdEqual,
                                                   UUID subscriberIdEqual, String cursor,
                                                   UUID idAfter, int limit, String sortBy,
                                                   SortDirection sortDirection) {
        log.debug("[플레이리스트] 목록 조회 서비스 호출: keywordLike= {}, ownerIdEqual= {}, subscriberIdEqual= {}, " +
                        "cursor= {}, idAfter= {}, limit= {}, sortBy= {}, sortDirection= {}",
                keywordLike, ownerIdEqual, subscriberIdEqual, cursor, idAfter, limit, sortBy, sortDirection);

        boolean asc = (sortDirection == SortDirection.ASCENDING);

        String sortByValue;
        if (sortBy == null || sortBy.isBlank()) {
            sortByValue = "updatedAt";
        } else if ("updatedAt".equals(sortBy) || "subscribeCount".equals(sortBy)) {
            sortByValue = sortBy;
        } else {
            log.debug("[플레이리스트] 지원하지 않는 sortBy 값, 기본값 사용: {}", sortBy);
            sortByValue = "updatedAt";
        }

        //커서 해석 (cursor가 메인)
        UUID effectiveIdAfter = null;
        if (cursor != null && !cursor.isBlank()) {
            try {
                effectiveIdAfter = UUID.fromString(cursor);
            } catch (IllegalArgumentException e) {
                log.error("[플레이리스트] 잘못된 cursor 형식: cursor= {}", cursor);
                effectiveIdAfter = null;
            }
        }

        //idAfter가 보조
        if (effectiveIdAfter == null && idAfter != null) {
            effectiveIdAfter = idAfter;
        }

        boolean hasCursor = (effectiveIdAfter != null);


        Pageable pageable = PageRequest.of(0, limit);

        Slice<Playlist> playlists = playlistRepository.searchPlaylists(
                keywordLike,
                ownerIdEqual,
                subscriberIdEqual,
                hasCursor,
                effectiveIdAfter,
                asc,
                sortByValue,
                pageable
        );

        List<Playlist> content = playlists.getContent();
        boolean hasNext = playlists.hasNext();

        List<PlaylistDto> data = content.stream()
                .map(p -> playlistMapper.toDto(p, s3Properties))
                .toList();

        // 6. nextCursor, nextIdAfter 계산
        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !content.isEmpty()) {
            Playlist last = content.get(content.size() - 1);
            nextCursor = last.getId().toString();   // 커서 = 마지막 id 문자열
            nextIdAfter = last.getId();
        }

        // 7. 필터 기준 전체 개수 조회
        long totalCount = playlistRepository.countPlaylists(
                keywordLike,
                ownerIdEqual,
                subscriberIdEqual
        );

        CursorResponsePlaylistDto response = new CursorResponsePlaylistDto(
                data,
                nextCursor,
                nextIdAfter,
                hasNext,
                totalCount,
                sortByValue,
                sortDirection
        );

        log.debug("[플레이리스트] 목록 조회 서비스 완료: dataSize= {}, hasNext= {}, totalCount= {}, " +
                        "nextCursor= {}, nextIdAfter= {}",
                data.size(), hasNext, totalCount, nextCursor, nextIdAfter);

        return response;
    }

    //플레이리스트 단건 조회
    @Transactional(readOnly = true)
    @Override
    public PlaylistDto getPlaylist(UUID playlistId, UUID currentUserId) {
        log.debug("[플레이리스트] 단건 조회 시작: playlistId= {}", playlistId);

        //플레이리스트와 연관 객체 로딩
        Playlist playlist = playlistRepository.findWithDetailsById(playlistId)
                .orElseThrow(() -> PlaylistNotFoundException.withId(playlistId));

        List<UUID> contentIds = playlist.getPlaylistContents().stream()
                .map(pc -> pc.getContent().getId())
                .toList();

        Map<UUID, List<String>> tagMap = tagRepository.findTagsByContentIds(contentIds);

        //로그인한 유저가 해당 플레이리스트를 구독하고 있는지
        boolean subscribedByMe = currentUserId != null &&
                subscribeRepository.existsBySubscriber_IdAndPlaylist_Id(currentUserId, playlistId);



        //Entity -> DTO
        PlaylistDto dto = playlistMapper.toDto(playlist, tagMap, s3Properties);

        PlaylistDto result = new PlaylistDto(
                dto.id(),
                dto.owner(),
                dto.title(),
                dto.description(),
                dto.updatedAt(),
                dto.subscriberCount(),
                subscribedByMe,
                dto.contents()
        );

        log.info("[플레이리스트] 단건 조회 완료: playlistId= {}", playlistId);
        return result;
    }

}
