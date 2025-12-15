package com.codeit.playlist.domain.content.batch;

import com.codeit.playlist.domain.content.api.handler.ApiHandler;
import com.codeit.playlist.domain.content.api.mapper.TmdbMapper;
import com.codeit.playlist.domain.content.api.response.TvSeriesResponse;
import com.codeit.playlist.domain.content.api.service.TvSeriesApiService;
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
public class TvSeriesTasklet implements Tasklet {
    private final TvSeriesApiService tvSeriesApiService;
    private final TmdbMapper tvSeriesMapper;
    private final ContentRepository contentRepository;
    private final TagService tagService;
    private final ApiHandler apiHandler;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("[콘텐츠 데이터 관리] TMDB TvSeries API 데이터 배치 수집 시작");
        String query = "language=ko-KR&year=2025";
        int invalidCount = 0;
        int existCount = 0;
        int languageCount = 0;

        List<TvSeriesResponse> tvSeriesResponseList = tvSeriesApiService.getApiTv(query)
                .doOnError(e -> log.error("[콘텐츠 데이터 관리 TvSeries 배치 Tasklet 스트림 에러 발생 : {}]", e.getMessage(), e))
                .doOnComplete(() -> log.info("[콘텐츠 데이터 관리 TvSeries 배치 Tasklet 스트림 동작 완료]"))
                .collectList()
                .block();

        if(tvSeriesResponseList == null || tvSeriesResponseList.isEmpty()) {
            return RepeatStatus.FINISHED;
        }

        for(int i = 0; i < tvSeriesResponseList.size(); i++) {
            TvSeriesResponse tvSeriesResponse = tvSeriesResponseList.get(i);
            Content content = tvSeriesMapper.toContent(tvSeriesResponse, "tvSeries");
            String thumbnailUrl = tvSeriesResponse.thumbnailUrl();
            if(thumbnailUrl != null || !thumbnailUrl.isBlank()) {
                content.setThumbnailUrl("https://image.tmdb.org/t/p/w500" + thumbnailUrl);
            }

            if(tvSeriesResponse.description() == null || tvSeriesResponse.description().isBlank()) {
                invalidCount++;
                continue;
            }

            if(tvSeriesResponse.apiId() == null) {
                continue;
            }

            Long tmdbId = tvSeriesResponse.apiId();
            if(contentRepository.existsByApiId(tmdbId)) {
                existCount++;
                continue;
            }

            if(tvSeriesResponse.title() == null || !apiHandler.isKorean(tvSeriesResponse.title())) {
                languageCount++;
                continue;
            }

            Content resultContent = contentRepository.save(content);

            if(tvSeriesResponse.genreIds() != null && !tvSeriesResponse.genreIds().isEmpty()) {
                tagService.saveTvSeriesTagToContent(resultContent, tvSeriesResponse.genreIds());
            }
        }
        log.debug("[콘텐츠 데이터 관리] TMDB TvSport API에서 한글이 한글자도 없는 콘텐츠 횟수 : {}", languageCount);
        log.debug("[콘텐츠 데이터 관리] TMDB TvSport API가 이만큼 비어있어요. count : {}", invalidCount);
        log.debug("[콘텐츠 데이터 관리] TMDB TvSport API content가 이만큼 없어요. count : {}", existCount);
        log.info("[콘텐츠 데이터 관리] TvSeries API 콘텐츠와 태그 수집 완료");
        return RepeatStatus.FINISHED;
    }
}
