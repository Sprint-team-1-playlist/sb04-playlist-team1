package com.codeit.playlist.domain.content.batch;

import com.codeit.playlist.domain.content.api.mapper.TheMovieMapper;
import com.codeit.playlist.domain.content.api.response.TheMovieResponse;
import com.codeit.playlist.domain.content.api.service.TheMovieApiService;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import com.codeit.playlist.domain.content.entity.Type;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.content.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentTasklet implements Tasklet {
    private final TheMovieApiService theMovieApiService;
    private final TheMovieMapper theMovieMapper;
    private final ContentRepository contentRepository;
    private final TagRepository tagRepository;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("[콘텐츠 데이터 관리] TMDB The Movie API 데이터 배치 수집 시작");

        String query = "&language=ko-KR&year=2025";

        List<TheMovieResponse> movieResponseList = theMovieApiService.getApiMovie(query)
                .doOnError(e -> log.error("[콘텐츠 데이터 관리] The Movie 배치 Tasklet 스트림 에러 발생 : {}", e.getMessage(), e))
                .doOnComplete(() -> log.info("[콘텐츠 데이터 관리] The Movie 배치 Tasklet 스트림 동작 완료, API 데이터 수집"))
                .collectList()
                .block();

        if(movieResponseList == null || movieResponseList.isEmpty()) {
            log.error("[콘텐츠 데이터 관리] TMDB The Movie API contents가 없어요. query : {}", query);
            return RepeatStatus.FINISHED;
        }

        for(int i = 0; i< movieResponseList.size(); i++) {
            TheMovieResponse movieResponse = movieResponseList.get(i);
            Content content = theMovieMapper.toContent(movieResponse, Type.MOVIE);
            String thumbnailUrl = movieResponse.thumbnailUrl();
            if(thumbnailUrl != null && !thumbnailUrl.isBlank()) {
                content.setThumbnailUrl("https://image.tmdb.org/t/p/w500" + thumbnailUrl);
            }
            Content resultContent = contentRepository.save(content); // 썸네일까지 set된 content

            // 조건문 추가해줘야됨
            if(movieResponse.genreIds() != null && !movieResponse.genreIds().isEmpty()) {
                for(int j = 0; j < movieResponse.genreIds().size(); j++) {
                    Integer genreId = movieResponse.genreIds().get(j);
                    String tagName = changeString(genreId);
                    Tag tag = new Tag(resultContent, tagName);
                    tagRepository.save(tag);
                }
            }
        }
        log.info("[콘텐츠 데이터 관리] The Movie API 콘텐츠와 태그 수집 완료");
        return RepeatStatus.FINISHED;
    }

    public String changeString(Integer genreId) {
        return genreId.toString();
    }
}
