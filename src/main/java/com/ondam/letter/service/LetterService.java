package com.ondam.letter.service;

import com.ondam.notification.entity.NotificationType;
import com.ondam.notification.service.NotificationService;
import com.ondam.notification.service.WebPushService;
import com.ondam.user.entity.NotificationSetting;
import com.ondam.user.repository.NotificationSettingRepository;
import com.ondam.letter.entity.Letter;
import com.ondam.letter.repository.LetterRepository;
import com.ondam.letter.dto.request.LetterSendRequest;
import com.ondam.letter.dto.response.LetterResponse;
import com.ondam.question.entity.InputType;
import com.ondam.user.entity.User;
import com.ondam.user.repository.UserRepository;
import com.ondam.global.util.S3Uploader;


import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LetterService {

    private final LetterRepository letterRepository;
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;

    private final NotificationService notificationService;
    private final NotificationSettingRepository notificationSettingRepository;
    private final WebPushService webPushService;

    private final WebClient openAiWebClient;

    @Transactional
    public void sendLetter(Long fromUserId, LetterSendRequest request) {

        Letter letter = Letter.builder()
                .fromUserId(fromUserId)
                .toUserId(request.toUserId())
                .content(request.content())
                .inputType(request.inputType())
                .build();

        letterRepository.save(letter);

        sendFamilyReactionPush(fromUserId, request.toUserId(), "새로운 편지가 도착했어요!", "가족이 보낸 따뜻한 편지를 확인해보세요.");
    }

    public Page<LetterResponse> getReceivedLetters(Long userId, Pageable pageable) {

        Page<Letter> letters = letterRepository.findByToUserId(userId, pageable);

        return letters.map(letter -> {

            // 1. letter.getFromUserId()로 User 조회
            Optional<User> fromUserOpt = userRepository.findById(letter.getFromUserId());

            // 2. name, role 꺼내기 (없으면 기본값)
            String name = "Unknown";
            String role = null;

            if(fromUserOpt.isPresent()){
                name = fromUserOpt.get().getName();
                role = fromUserOpt.get().getRole().toString();
            }

            // 3. LetterResponse.builder()로 조립해서 return
            LetterResponse.FromUserItem fromUserItem = LetterResponse.FromUserItem.builder()
                    .userId(letter.getFromUserId())
                    .name(name)
                    .role(role)
                    .build();

            return LetterResponse.builder()
                    .letterId(letter.getId())
                    .fromUser(fromUserItem)
                    .content(letter.getContent())
                    .inputType(letter.getInputType().toString())
                    .audioUrl(letter.getAudioUrl())
                    .readAt(letter.getReadAt())
                    .createdAt(letter.getCreatedAt())
                    .build();

        });
    }

    public Page<LetterResponse> getSentLetters(Long userId, Pageable pageable) {

        Page<Letter> letters = letterRepository.findByFromUserId(userId, pageable);

        return letters.map(letter -> {

            // fromUser는 "나 자신"이니, 굳이 다시 조회 안 해도 되지만
            Optional<User> fromUserOpt = userRepository.findById(letter.getFromUserId());
            Optional<User> toUserOpt = userRepository.findById(letter.getToUserId());

            String fromName = "Unknown";
            String fromRole = null;
            String toName = "Unknown";
            String toRole = null;

            if(fromUserOpt.isPresent()){
                fromName = fromUserOpt.get().getName();
                fromRole = fromUserOpt.get().getRole().toString();
            }

            if(toUserOpt.isPresent()){
                toName = toUserOpt.get().getName();
                toRole = toUserOpt.get().getRole().toString();
            }

            LetterResponse.FromUserItem fromUserItem = LetterResponse.FromUserItem.builder()
                    .userId(letter.getFromUserId())
                    .name(fromName)
                    .role(fromRole)
                    .build();

            LetterResponse.ToUserItem toUserItem = LetterResponse.ToUserItem.builder()
                    .userId(letter.getToUserId())
                    .name(toName)
                    .role(toRole)
                    .build();

            return LetterResponse.builder()
                    .letterId(letter.getId())
                    .fromUser(fromUserItem)
                    .toUser(toUserItem)
                    .content(letter.getContent())
                    .inputType(letter.getInputType().toString())
                    .audioUrl(letter.getAudioUrl())
                    .readAt(letter.getReadAt())
                    .createdAt(letter.getCreatedAt())
                    .build();
        });
    }

    public void markAsRead(Long letterId) {

        // 1. letterId로 Letter 조회 (없으면 예외)
        Letter letter = letterRepository.findById(letterId)
                .orElseThrow(() -> new RuntimeException("편지를 찾을 수 없습니다."));
        // 2. readAt에 지금 시각 채우기
        letter.markAsRead();
        // 3. 저장

        letterRepository.save(letter);
    }

    @Transactional
    public void sendVoiceLetter(Long fromUserId, Long toUserId, MultipartFile audioFile) {

        String audioUrl = s3Uploader.upload(audioFile, "letters");

        String transcribedText = callWhisperApi(audioFile);

        // TODO: STT로 content도 채울 수 있으면 좋지만, 지금은 생략(비워둠)
        Letter letter = Letter.builder()
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .content(transcribedText)
                .inputType(InputType.VOICE)
                .audioUrl(audioUrl)
                .build();

        letterRepository.save(letter);

        sendFamilyReactionPush(fromUserId, toUserId, "새로운 음성 편지가 도착했어요!", "가족이 보낸 따뜻한 목소리를 들어보세요.");
    }

    // STT API 호출 메서드
    private String callWhisperApi(MultipartFile audioFile) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", audioFile.getResource());
            builder.part("model", "whisper-1");

            Map<String, Object> response = openAiWebClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return (String) response.get("text");

        } catch (Exception e) {
            // STT 변환 실패 시 예외 던지지 않고 null 반환하여 오디오 전송은 성공하게 만듦
            return null;
        }
    }

    private void sendFamilyReactionPush(Long fromUserId, Long toUserId, String title, String content) {
        NotificationSetting setting = notificationSettingRepository.findByUserId(toUserId).orElse(null);

        // 상대방이 '가족 반응 알림'을 켜두었을 때 발송
        if (setting != null && setting.isFamilyReactionEnabled()) {

            // 누가 보냈는지 이름 찾기
            String fromName = userRepository.findById(fromUserId)
                    .map(User::getName).orElse("가족");
            String finalContent = fromName + "님이 보낸 " + title;

            // DB 알림 내역 저장 및 웹 푸시 전송
            notificationService.createNotification(toUserId, NotificationType.LETTER, title, finalContent, java.time.LocalDateTime.now());
            webPushService.sendPush(toUserId, title, finalContent);
        }
    }
}