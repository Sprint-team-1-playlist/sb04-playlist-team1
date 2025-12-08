package com.codeit.playlist.domain.content.service.basic;

import com.codeit.playlist.domain.content.api.service.TmdbTagApiService;
import com.codeit.playlist.domain.content.repository.TagRepository;
import com.codeit.playlist.domain.content.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasicTagService implements TagService {
    private final TagRepository tagRepository;
    private final TmdbTagApiService tmdbTagApiService;
    @Override
    public void saveMovieTag() {

    }
}
