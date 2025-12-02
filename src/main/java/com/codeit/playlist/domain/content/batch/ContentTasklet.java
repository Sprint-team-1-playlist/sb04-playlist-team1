package com.codeit.playlist.domain.content.batch;

import com.codeit.playlist.domain.content.api.mapper.TheMovieMapper;
import com.codeit.playlist.domain.content.api.service.TheMovieApiService;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Type;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentTasklet implements Tasklet {
    private final TheMovieApiService theMovieApiService;
    private final TheMovieMapper theMovieMapper;
    private final ContentRepository contentRepository;
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("TMDB The Movie API 데이터 배치 수집 시작");

        String query = "Japan";

        List<Content> contents = theMovieApiService.getApiMovie(query)
                .map(response -> {
                    Content content = theMovieMapper.toContent(response, Type.MOVIE);
                    String thumbnailUrl = response.thumbnailUrl();
                    if(thumbnailUrl != null && !thumbnailUrl.isBlank()) {
                        content.setThumbnailUrl("https://image.tmdb.org/t/p/w500" + thumbnailUrl);
                    }
                    return content;
                })
                .doOnComplete(() -> log.info("The Movie 배치 Tasklet 스트림 동작 완료, API 데이터 수집"))
                .doOnError(e -> log.error("The Movie 배치 Tasklet 스트림 에러 발생 : {}", e.getMessage(), e))
                .collectList()
                .block();

        if(contents != null && !contents.isEmpty()) {
            contentRepository.saveAll(contents);
        } else {
            log.error("TMDB The Movie API contents가 없어요. query : {}", query);
            return RepeatStatus.FINISHED;
        }

        return RepeatStatus.FINISHED;
    }
}
