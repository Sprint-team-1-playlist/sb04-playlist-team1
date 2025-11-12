package com.codeit.playlist.domain.playlist.service.basic;

import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.mapper.PlaylistMapper;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.service.PlaylistService;
import com.codeit.playlist.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasicPlaylistService implements PlaylistService {

    private final PlaylistRepository playlistRepository;
//    private final UserRepository userRepository;
    private final PlaylistMapper playlistMapper;
    private final EntityManager em; //임시 EntityManager(삭제 예정)

    @Transactional
    @Override
    public PlaylistDto createPlaylist(PlaylistCreateRequest request, UUID ownerId) {
        validate(request);//누락 방지

        log.debug("[플레이리스트] 생성 요청: ownerId={}, title={}", ownerId, request.title());

        Playlist playlist;
        if(ownerId != null) {
            User ownerRef = em.getReference(User.class, ownerId);
            playlist = playlistMapper.toEntity(request, ownerRef);
        } else {
            playlist = playlistMapper.toEntity(request);
        }

        Playlist saved = playlistRepository.save(playlist);
        PlaylistDto dto = playlistMapper.toDto(saved);

        log.info("[플레이리스트] 생성 완료: id={}", dto.id());
        return dto;
    }

    private void validate(PlaylistCreateRequest req) {
        if (req == null) throw new IllegalArgumentException("request must not be null");
        if (req.title() == null || req.title().trim().isEmpty())
            throw new IllegalArgumentException("title must not be blank");
        if (req.description() == null || req.description().trim().isEmpty())
            throw new IllegalArgumentException("description must not be blank");
    }
}
