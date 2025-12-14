package com.codeit.playlist.domain.content.batch;

import com.codeit.playlist.domain.content.api.mapper.TheMovieMapper;
import com.codeit.playlist.domain.content.api.response.TheMovieResponse;
import com.codeit.playlist.domain.content.api.service.TheMovieApiService;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import com.codeit.playlist.domain.content.service.TagService;
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
    private final TagService tagService;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("[콘텐츠 데이터 관리] TMDB The Movie API 데이터 배치 수집 시작");

        String query = "language=ko-KR&year=2025";
        int invalidCount = 0;
        int existCount = 0;
        int languageCount = 0;

        List<TheMovieResponse> movieResponseList = theMovieApiService.getApiMovie(query)
                .doOnError(e -> log.error("[콘텐츠 데이터 관리] The Movie 배치 Tasklet 스트림 에러 발생 : {}", e.getMessage(), e))
                .doOnComplete(() -> log.info("[콘텐츠 데이터 관리] The Movie 배치 Tasklet 스트림 동작 완료, API 데이터 수집"))
                .collectList()
                .block();

        if(movieResponseList == null || movieResponseList.isEmpty()) {
            return RepeatStatus.FINISHED;
        }

        for(int i = 0; i< movieResponseList.size(); i++) {
            TheMovieResponse movieResponse = movieResponseList.get(i);
            Content content = theMovieMapper.toContent(movieResponse, "movie");
            String thumbnailUrl = movieResponse.thumbnailUrl();
            if(thumbnailUrl != null && !thumbnailUrl.isBlank()) {
                content.setThumbnailUrl("https://image.tmdb.org/t/p/w500" + thumbnailUrl);
            }

            if(movieResponse.description() == null || movieResponse.description().isBlank()) { // 설명이 없음
                invalidCount++;
                continue;
            }

            if(movieResponse.tmdbId() == null) { // tmdb id가 없음
                continue;
            }

            Long tmdbId = movieResponse.tmdbId();
            if(contentRepository.existsByApiId(tmdbId)) { // tmdbId가 이미 있음
                existCount++;
                continue;
            }

            if(movieResponse.title() == null || !isKorean(movieResponse.title())) { // 한글이 한글자도 없음, false
                languageCount++;
                continue;
            }
            Content resultContent = contentRepository.save(content); // 썸네일까지 set된 content

            if(movieResponse.genreIds() != null && !movieResponse.genreIds().isEmpty()) {
                tagService.saveMovieTagToContent(resultContent, movieResponse.genreIds());
            }
        }
        log.info("[콘텐츠 데이터 관리] TMDB The Movie API에서 한글이 한글자도 없는 콘텐츠 횟수 : {}", languageCount);
        log.info("[콘텐츠 데이터 관리] TMDB The Movie API가 이만큼 비어있었어요. count : {}", invalidCount);
        log.info("[콘텐츠 데이터 관리] TMDB The Movie API contents가 이만큼 없어요. count : {}", existCount);
        log.info("[콘텐츠 데이터 관리] The Movie API 콘텐츠와 태그 수집 완료");
        return RepeatStatus.FINISHED;
    }

    private boolean isKorean(String title) {
        boolean result = title.matches(".*[가-힣].*"); // true
        return result;
    }
}
