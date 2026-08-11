package com.ondam.global.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateUtils {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 지금 시각 기준 "오늘" (모든 record_date는 이걸로 통일) */
    public static LocalDate today() {
        return LocalDate.now(KST);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(KST);
    }

    /** 주의 시작일(월요일) */
    public static LocalDate weekStart(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }
}