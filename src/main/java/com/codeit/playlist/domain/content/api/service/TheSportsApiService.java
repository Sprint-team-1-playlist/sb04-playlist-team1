package com.codeit.playlist.domain.content.api.service;

import com.codeit.playlist.domain.content.api.handler.TheSportsDateHandler;
import com.codeit.playlist.domain.content.api.response.TheSportsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TheSportsApiService {

    private final TheSportsDateHandler dateHandler;

    public List<TheSportsResponse> searchSports(int year, int month) {

        YearMonth sportsMonth = YearMonth.of(year, month);
        LocalDate startDate = sportsMonth.atDay(1);
        LocalDate lastDate = sportsMonth.atEndOfMonth();

        List<TheSportsResponse> sportsList = new ArrayList<>();

        for(LocalDate date = startDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
            sportsList.addAll(dateHandler.getSportsEvent(date));
        }
        return sportsList;
    }
}
