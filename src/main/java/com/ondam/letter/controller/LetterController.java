package com.ondam.letter.controller;

import com.ondam.global.common.ApiResponse;
import com.ondam.letter.dto.request.LetterSendRequest;
import com.ondam.letter.dto.response.LetterResponse;
import com.ondam.letter.dto.response.LetterVoiceResponse;
import com.ondam.letter.service.LetterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "편지", description = "가족 간 편지 주고받기 API")
@RestController
@RequestMapping("/api/v1/letters")
@RequiredArgsConstructor
public class LetterController {

    private final LetterService letterService;

    @Operation(summary = "편지 보내기")
    @PostMapping
    public ApiResponse<Void> sendLetter(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody LetterSendRequest request) {
        letterService.sendLetter(userId, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "받은 편지함 조회")
    @GetMapping("/received")
    public ApiResponse<Page<LetterResponse>> getReceivedLetters(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(letterService.getReceivedLetters(userId, pageable));
    }

    @Operation(summary = "보낸 편지함 조회")
    @GetMapping("/sent")
    public ApiResponse<Page<LetterResponse>> getSentLetters(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(letterService.getSentLetters(userId, pageable));
    }

    @Operation(summary = "편지 읽음 처리")
    @PatchMapping("/{letterId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long letterId) {
        letterService.markAsRead(letterId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "음성 편지 변환 (전송X)", description = "음성을 STT 변환하고 오디오 URL을 반환합니다. 이 결과를 받아 최종 /letters API로 편지를 보냅니다.")
    @PostMapping(value = "/voice", consumes = "multipart/form-data")
    public ApiResponse<LetterVoiceResponse> uploadVoiceLetter(
            @RequestParam MultipartFile audioFile) {
        return ApiResponse.success(letterService.uploadAndTranscribeVoice(audioFile));
    }
}