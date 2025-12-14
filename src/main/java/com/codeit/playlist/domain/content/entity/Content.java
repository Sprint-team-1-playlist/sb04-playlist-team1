package com.codeit.playlist.domain.content.entity;

import com.codeit.playlist.domain.base.BaseUpdatableEntity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contents")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseUpdatableEntity {

    /**
     * TMDB ID
     */
    @Column(name = "api_id", nullable = false)
    private Long apiId;

    /**
     * 컨텐츠 타입
     */
    @Column(length = 20, nullable = false)
    private String type;

    /**
     * 컨텐츠 제목
     */
    @Column(length = 100, nullable = false)
    private String title;

    /**
     * 컨텐츠 설명@Slf4j
     * @Service
     * @RequiredArgsConstructor
     * public class TheSportApiService {
     *     private final WebClient webClient;
     *
     *     private Mono<TheSportListResponse> callTheSportApi(int leagueId, String season) {
     *         log.info("[콘텐츠 데이터 관리] TheSport API Mono 빌드 시작");
     *         return webClient.get()
     *                 .uri(uriBuilder -> uriBuilder
     *                         .scheme("https")
     *                         .host("www.thesportsdb.com")
     *                         .path("/api/v1/json/123/eventsseason.php")
     *                         .queryParam("id", leagueId)
     *                         .queryParam("s", season)
     *                         .build())
     *                 .retrieve()
     *                 .onStatus(s -> s.value() == 429, clientResponse -> clientResponse.createException().flatMap(Mono::error))
     *                 .bodyToMono(TheSportListResponse.class)
     *                 .retryWhen(
     *                         Retry.backoff(5, Duration.ofSeconds(5))
     *                                 .maxBackoff(Duration.ofMinutes(1))
     *                                 .filter(exception -> exception instanceof WebClientResponseException webException && webException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS)
     *                                 .transientErrors(true)
     *                 )
     *                 .doOnNext(response -> log.info("season : {}, totalEvents : {}", season, response.events() == null ? 0 : response.events().size()))
     *                 .doOnError(WebClientResponseException.class,
     *                         e -> log.error("[콘텐츠 데이터 관리] TheSports API Mono 빌드 에러 발생", e));
     *     }
     *
     *     private Flux<TheSportResponse> fluxingTheSportApi(YearMonth yearMonth) {
     *         log.info("[콘텐츠 데이터 관리] TheSport API Flux 빌드 시작");
     *         int leagueId = 4328;
     *         String season = circulateSoccerSeason(yearMonth);
     *         return callTheSportApi(leagueId, season)
     *                 .flatMapMany(theSportResponse -> {
     *                     if(theSportResponse.events() == null || theSportResponse.events().isEmpty()) {
     *                         return Flux.empty();
     *                     }
     *                     return Flux.fromIterable(theSportResponse.events());
     *                         })
     *                 .filter(response -> response.dateEvent() != null && response.dateEvent().isBlank())
     *                 .filter(response -> YearMonth.from(LocalDate.parse(response.dateEvent())).equals(yearMonth))
     *                 .onErrorResume(WebClientResponseException.TooManyRequests.class, e -> {
     *                     log.warn("[콘텐츠 데이터 관리] 429 Too many Requests");
     *                     return Flux.empty();
     *                 });
     *     }
     *
     *     private String circulateSoccerSeason(YearMonth yearMonth) {
     *         int year = yearMonth.getYear();
     *         return yearMonth.getMonthValue() >= 8 ? (year + "-" + (year + 1)) : ((year - 1) + "-" + year);
     *     }
     *
     *     public Flux<TheSportResponse> getApiSport(int year, int month) {
     *         return fluxingTheSportApi(YearMonth.of(year, month));
     *     }
     * }
     */
    @Column(nullable = false, length = 2000)
    private String description;

    /**
     * 썸네일 이미지 URL
     */
    @Column(nullable = false)
    private String thumbnailUrl;

    /**
     * 평균 평점
     */
    @Column(nullable = false)
    private double averageRating;

    /**
     * 리뷰 개수
     */
    @Column(nullable = false)
    private int reviewCount;

    /**
     * 시청자 수
     */
    @Column(nullable = false)
    private int watcherCount;

    public void updateContent(String title, String description, String thumbnailUrl) {
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
    }

    public static Content createSportsContent(String type, String description, String thumbnailUrl) {
        Content content = new Content();
        content.type = type;
        content.title = "sport";
        content.description = description;
        content.thumbnailUrl = thumbnailUrl;
        content.averageRating = 0.0;
        content.reviewCount = 0;
        content.watcherCount = 0;
        return content;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setApiId(Long apiId) {
        this.apiId = apiId;
    }

    /**
     * 컨텐츠 편의 메서드(리뷰용)
     */

    // 리뷰 생성 시 호출
    public void applyReviewCreated(int newRating) {
        int beforeCount = this.reviewCount;
        double beforeAvg = this.averageRating;

        int afterCount = beforeCount + 1;
        double afterAvg = ((beforeAvg * beforeCount) + newRating) / afterCount;

        this.reviewCount = afterCount;
        this.averageRating = afterAvg;
    }

    // 리뷰 수정 시 호출 (기존 평점 -> 새로운 평점)
    public void applyReviewUpdated(int oldRating, int newRating) {
        if (this.reviewCount <= 0) {
            this.reviewCount = 1;
            this.averageRating = newRating;
            return;
        }

        double sum = this.averageRating * this.reviewCount;
        double newSum = sum - oldRating + newRating;
        double afterAvg = newSum / this.reviewCount;

        this.averageRating = afterAvg;
    }

    public void applyReviewDeleted(int deletedRating) {
        int beforeCount = this.reviewCount;

        if (beforeCount <= 0) {
            this.reviewCount = 0;
            this.averageRating = 0;
            return;
        }

        int afterCount = beforeCount - 1;

        if (afterCount == 0) {
            this.reviewCount = 0;
            this.averageRating = 0.0;
        } else {
            double afterAvg =
                    ((this.averageRating * beforeCount) - deletedRating)
                            / afterCount;

            this.reviewCount = afterCount;
            this.averageRating = afterAvg;
        }
    }
}
