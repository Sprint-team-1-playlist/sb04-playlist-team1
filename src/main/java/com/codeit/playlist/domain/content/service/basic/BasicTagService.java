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

//    @Override
//    public void saveMovieTag() {
//        log.info("[콘텐츠 데이터 관리] The Movie Tag DB적재 시작");
//        List<TheMovieTagResponse> tagResponseList = tmdbTagApiService.getApiMovieTag().block();
//
//        for(int i = 0; i < tagResponseList.size(); i++) {
//            TheMovieTagResponse tagResponse = tagResponseList.get(i);
//            Optional<Tag> genreTag = tagRepository.findByGenreId(tagResponse.genreId());
//            if(genreTag.isPresent()) {
//                Tag existsTag = genreTag.get();
//                existsTag.setName(tagResponse.name());
//                log.info("[콘텐츠 데이터 관리] 태그가 이미 존재합니다. GenreId에 맞는 name으로 바꾸기만 합니다.");
//            } else {
//                Tag tag = new Tag();
//                tag.setGenreId(tagResponse.genreId());
//                tag.setName(tagResponse.name());
//                tagRepository.save(tag);
//                log.info("[콘텐츠 데이터 관리] 태그 생성 완료, tag : {}", tagRepository.findByGenreId(tagResponse.genreId()).orElse(null));
//            }
//        }
//        log.info("[콘텐츠 데이터 관리] The Movie Tag DB적재 완료, size : {}", tagResponseList.size());
//    }

    @Override
    public void saveMovieTagToContent(Content content, List<Integer> genreIds) {
        Map<Integer, String> movieGenreList = tmdbTagApiService.getApiMovieTag().block();

        if (movieGenreList == null || movieGenreList.isEmpty()) {
            log.debug("[콘텐츠 데이터 관리] TMDB 장르 맵이 비어있습니다.");
            return;
        }

        for(Integer genreId : genreIds) {
            String name = movieGenreList.get(genreId);
            if(name == null) {
                log.debug("[콘텐츠 데이터 관리] genreId {}에 해당하는 장르명을 찾을 수 없습니다", genreId);
            }
            Tag tag = new Tag(content, name);
            tag.setGenreId(genreId);
            tagRepository.save(tag);
        }
    }
}
