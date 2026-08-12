package com.ondam.family.service;

import com.ondam.family.dto.request.FamilyCreateRequest;
import com.ondam.family.dto.request.FamilyJoinRequest;
import com.ondam.family.dto.response.FamilyCreateResponse;
import com.ondam.family.entity.Family;
import com.ondam.family.repository.FamilyRepository;
import com.ondam.global.exception.BusinessException;
import com.ondam.global.exception.ErrorCode;
import com.ondam.user.entity.User;
import com.ondam.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;

    @Transactional
    public FamilyCreateResponse createFamily(
            Long userId,
            FamilyCreateRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        if (user.getFamily() != null) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }

        String inviteCode = createInviteCode();

        Family family = new Family(
                request.name(),
                inviteCode,
                userId
        );

        Family savedFamily = familyRepository.save(family);

        user.joinFamily(savedFamily);

        return new FamilyCreateResponse(
                savedFamily.getId(),
                savedFamily.getName(),
                savedFamily.getInviteCode()
        );
    }

    @Transactional
    public void joinFamily(
            Long userId,
            FamilyJoinRequest request
    ) {

        // 1. 참여하려는 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        // 2. 이미 가족에 속해 있는지 확인
        if (user.getFamily() != null) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }

        // 3. 초대코드로 가족 조회
        Family family = familyRepository
                .findByInviteCode(request.inviteCode())
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.FAMILY_NOT_FOUND)
                );

        // 4. 사용자와 가족 연결
        user.joinFamily(family);
    }

    // 중복되지 않는 초대코드 생성
    private String createInviteCode() {

        String code;

        do {
            code = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase();

        } while (familyRepository.existsByInviteCode(code));

        return code;
    }
}