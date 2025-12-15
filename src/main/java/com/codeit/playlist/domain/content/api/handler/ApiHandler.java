package com.codeit.playlist.domain.content.api.handler;

import org.springframework.stereotype.Component;

@Component
public class ApiHandler {
    public boolean isKorean(String title) {
        return title.matches(".*[가-힣].*"); // true이면 한국어
    }
}
