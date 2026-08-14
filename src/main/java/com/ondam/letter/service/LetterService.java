package com.ondam.letter.service;

import com.ondam.letter.entity.Letter;
import com.ondam.letter.repository.LetterRepository;
import com.ondam.letter.dto.request.LetterSendRequest;
import com.ondam.letter.dto.response.LetterResponse;
import com.ondam.question.entity.InputType;
import com.ondam.user.entity.User;
import com.ondam.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LetterService {

    private final LetterRepository letterRepository;
    private final UserRepository userRepository;

    public void sendLetter(Long fromUserId, LetterSendRequest request) {

        Letter letter = Letter.builder()
                .fromUserId(fromUserId)
                .toUserId(request.toUserId())
                .content(request.content())
                .inputType(request.inputType())
                .build();

        letterRepository.save(letter);
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

    public void sendVoiceLetter(Long fromUserId, Long toUserId, MultipartFile audioFile) {

        // TODO: 실제 파일 저장소(S3 등) 연동 필요. 지금은 고정 URL로 스텁 처리.
        String audioUrl = "https://example.com/letters/stub.webm";

        // TODO: STT로 content도 채울 수 있으면 좋지만, 지금은 생략(비워둠)
        Letter letter = Letter.builder()
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .content(null)
                .inputType(InputType.VOICE)
                .audioUrl(audioUrl)
                .build();

        letterRepository.save(letter);
    }
}