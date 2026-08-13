package com.ondam.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    FAMILY_NOT_FOUND(HttpStatus.NOT_FOUND, "가족을 찾을 수 없습니다."),
    ALREADY_JOINED(HttpStatus.CONFLICT, "이미 가족에 속해 있습니다."),
    ALREADY_ANSWERED(HttpStatus.CONFLICT, "이미 답변한 질문입니다."),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "오늘의 질문이 아직 생성되지 않았습니다."),
    REPORT_NOT_READY(HttpStatus.BAD_REQUEST, "리포트 생성 조건이 충족되지 않았습니다."),
    FAMILY_OWNER_CANNOT_LEAVE(HttpStatus.CONFLICT, "다른 가족 구성원이 남아 있어 가족 생성자는 나갈 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN,"가족 구성원을 내보낼 권한이 없습니다."),
    CANNOT_REMOVE_SELF(HttpStatus.BAD_REQUEST, "본인은 가족 나가기 API를 이용해주세요."),
    CANNOT_JOIN_SELF(HttpStatus.BAD_REQUEST, "자신의 초대코드는 입력할 수 없습니다."),
    MEDICATION_NOT_FOUND(HttpStatus.BAD_REQUEST,"복용약을 찾을 수 없습니다."),
    MEDICATION_LOG_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 복약 기록이 존재합니다."),
    MEDICATION_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "복약 일정을 찾을 수 없습니다.");
    private final HttpStatus status;
    private final String message;
}