package com.codeit.playlist.domain.content.service.basic;

import com.codeit.playlist.domain.content.api.service.TmdbTagApiService;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import com.codeit.playlist.domain.content.repository.TagRepository;
import com.codeit.playlist.domain.content.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasicTagService implements TagService {
    private final TagRepository tagRepository;
    private final TmdbTagApiService tmdbTagApiService;

    @Override
    public void saveMovieTagToContent(Content content, List<Integer> genreIds) {
        Map<Integer, String> genreList = tmdbTagApiService.getApiMovieTag().block();

        if (genreList == null || genreList.isEmpty()) {
            log.debug("[콘텐츠 데이터 관리] TMDB 장르 맵이 비어있습니다.");
            return;
        }

        for(Integer genreId : genreIds) {
            String name = genreList.get(genreId);
            if(name == null) {
                log.debug("[콘텐츠 데이터 관리] genreId {}에 해당하는 장르명을 찾을 수 없습니다", genreId);
            }
            Tag tag = new Tag(content, name);
            tag.setGenreId(genreId);
            tagRepository.save(tag);
        }
    }

    @Override
    public void saveTvSeriesTagToContent(Content content, List<Integer> genreIds) {
        Map<Integer, String> genreList = tmdbTagApiService.getApiTvSeriesTag().block();

        if(genreList == null || genreList.isEmpty()) {
            log.debug("[콘텐츠 데이터 관리] TMDB 장르 맵이 비어있습니다.");
            return;
        }

        for(int i = 0; i < genreIds.size(); i++) {
            Integer genreId = genreIds.get(i);
            String name = genreList.get(genreId);
            if(name == null) {
                log.debug("[콘텐츠 데이터 관리] genreId {}에 해당하는 장르명을 찾을 수 없습니다.", genreId);
            }
            Tag tag = new Tag(content, name);
            tag.setGenreId(genreId);
            tagRepository.save(tag);
        }
    }
}
