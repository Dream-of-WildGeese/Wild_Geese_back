package com.ondam.family.service;

import com.ondam.family.dto.request.FamilyCreateRequest;
import com.ondam.family.dto.request.FamilyJoinRequest;
import com.ondam.family.dto.response.FamilyCreateResponse;
import com.ondam.family.entity.Family;
import com.ondam.family.repository.FamilyRepository;
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
                        new IllegalArgumentException("사용자를 찾을 수 없습니다.")
                );

        if (user.getFamily() != null) {
            throw new IllegalStateException("이미 가족에 속해 있습니다.");
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

    @Transactional
    public void joinFamily(
            Long userId,
            FamilyJoinRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("사용자를 찾을 수 없습니다.")
                );

        if (user.getFamily() != null) {
            throw new IllegalStateException("이미 가족에 속해 있습니다.");
        }

        Family family = familyRepository
                .findByInviteCode(request.inviteCode())
                .orElseThrow(() ->
                        new IllegalArgumentException("유효하지 않은 초대코드입니다.")
                );

        user.joinFamily(family);
    }
}