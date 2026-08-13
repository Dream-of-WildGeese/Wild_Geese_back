package com.ondam.family.service;

import com.ondam.family.dto.request.FamilyCreateRequest;
import com.ondam.family.dto.request.FamilyJoinRequest;
import com.ondam.family.dto.response.FamilyCreateResponse;
import com.ondam.family.dto.response.FamilyInfoResponse;
import com.ondam.family.dto.response.FamilyMemberResponse;
import com.ondam.family.entity.Family;
import com.ondam.family.repository.FamilyRepository;
import com.ondam.global.exception.BusinessException;
import com.ondam.global.exception.ErrorCode;
import com.ondam.user.entity.User;
import com.ondam.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        if (user.getFamily() != null) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }

        Family family = familyRepository
                .findByInviteCode(request.inviteCode())
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.FAMILY_NOT_FOUND)
                );

        user.joinFamily(family);
    }

    @Transactional(readOnly = true)
    public FamilyInfoResponse getMyFamily(Long userId) {

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        // 2. 사용자가 가족에 속해 있는지 확인
        Family family = user.getFamily();

        if (family == null) {
            throw new BusinessException(ErrorCode.FAMILY_NOT_FOUND);
        }

        // 3. 같은 가족 구성원 조회
        List<FamilyMemberResponse> members =
                userRepository.findAllByFamilyId(family.getId())
                        .stream()
                        .map(member -> new FamilyMemberResponse(
                                member.getId(),
                                member.getEmail()
                        ))
                        .toList();

        // 4. 가족 정보 반환
        return new FamilyInfoResponse(
                family.getId(),
                family.getName(),
                family.getInviteCode(),
                members
        );
    }

    @Transactional
    public void leaveFamily(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        Family family = user.getFamily();

        if (family == null) {
            throw new BusinessException(ErrorCode.FAMILY_NOT_FOUND);
        }

        if (family.getCreatedBy().equals(userId)) {

            long memberCount = userRepository.countByFamilyId(family.getId());

            if (memberCount > 1) {
                throw new BusinessException(
                        ErrorCode.FAMILY_OWNER_CANNOT_LEAVE
                );
            }

            user.leaveFamily();

            familyRepository.delete(family);

            return;
        }

        user.leaveFamily();
    }

    @Transactional
    public void removeMember(
            Long requesterId,
            Long targetUserId
    ) {

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        Family family = requester.getFamily();

        if (family == null) {
            throw new BusinessException(ErrorCode.FAMILY_NOT_FOUND);
        }

        if (!family.getCreatedBy().equals(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }


        if (requesterId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_REMOVE_SELF);
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        if (targetUser.getFamily() == null ||
                !family.getId().equals(targetUser.getFamily().getId())) {

            throw new BusinessException(ErrorCode.FAMILY_NOT_FOUND);
        }

        targetUser.leaveFamily();
    }
}