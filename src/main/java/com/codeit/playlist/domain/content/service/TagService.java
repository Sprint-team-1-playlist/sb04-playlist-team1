package com.codeit.playlist.domain.content.service;

import com.codeit.playlist.domain.content.entity.Content;

import java.util.List;

public interface TagService {
//    void saveMovieTag();
    void saveMovieTagToContent(Content content, List<Integer> genreIds);
    void saveTvSeriesTagToContent(Content content, List<Integer> genreIds);
}
