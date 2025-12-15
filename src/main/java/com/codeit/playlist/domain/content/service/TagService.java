package com.codeit.playlist.domain.content.service;

import com.codeit.playlist.domain.content.entity.Content;

import java.util.List;

public interface TagService {
//    void saveMovieTag();
    void saveTmdbTagToContent(Content content, List<Integer> genreIds);
}
