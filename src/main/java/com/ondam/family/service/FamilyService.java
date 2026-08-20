package com.ondam.family.service;

import com.ondam.family.dto.request.FamilyJoinRequest;
import com.ondam.family.dto.response.FamilyInfoResponse;
import com.ondam.family.dto.response.FamilyJoinResponse;
import com.ondam.family.dto.response.FamilyMemberResponse;
import com.ondam.family.entity.Family;
import com.ondam.family.repository.FamilyRepository;
import com.ondam.global.exception.BusinessException;
import com.ondam.global.exception.ErrorCode;
import com.ondam.user.entity.HealthProfile;
import com.ondam.user.entity.User;
import com.ondam.user.repository.HealthProfileRepository;
import com.ondam.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;

    @Transactional
    public FamilyJoinResponse joinFamily(
            Long userId,
            FamilyJoinRequest request
    ) {

        // 1. 코드 입력한 사용자
        User joiningUser = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        // 2. 입력한 초대코드 주인
        User inviter = userRepository
                .findByInviteCode(request.inviteCode())
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        // 3. 자기 자신의 코드 방지
        if (joiningUser.getId().equals(inviter.getId())) {
            throw new BusinessException(ErrorCode.CANNOT_JOIN_SELF);
        }

        // 4. 이미 가족인 경우
        if (joiningUser.getFamily() != null) {

            Family currentFamily = joiningUser.getFamily();

            // 입력한 초대코드의 주인이 현재 같은 가족이면
            // 그 사람을 연결된 사용자로 반환
            if (inviter.getFamily() != null
                    && currentFamily.getId().equals(inviter.getFamily().getId())) {

                return new FamilyJoinResponse(
                        currentFamily.getId(),
                        inviter.getId(),
                        inviter.getName()
                );
            }

            // 다른 가족의 코드를 입력한 경우
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }

        // 5. 초대한 사람이 속한 가족
        Family family = inviter.getFamily();

        // 6. 초대한 사람도 가족이 없다면 생성
        if (family == null) {

            Family newFamily = new Family(
                    inviter.getId()
            );

            family = familyRepository.save(newFamily);

            inviter.joinFamily(family);
        }

        // 7. 코드 입력한 사용자 가족 연결
        joiningUser.joinFamily(family);

        // 8. 반드시 "입력한 초대코드의 주인"을 반환
        return new FamilyJoinResponse(
                family.getId(),
                inviter.getId(),
                inviter.getName()
        );
    }

    public FamilyInfoResponse getMyFamily(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        Family family = user.getFamily();

        if (family == null) {
            throw new BusinessException(ErrorCode.FAMILY_NOT_FOUND);
        }

        List<FamilyMemberResponse> members =
                userRepository.findAllByFamilyId(family.getId())
                        .stream()
                        .map(member -> {

                            HealthProfile healthProfile =
                                    healthProfileRepository
                                            .findByUserId(member.getId())
                                            .orElse(null);

                            return new FamilyMemberResponse(
                                    member.getId(),
                                    member.getEmail(),
                                    member.getRole(),
                                    healthProfile != null
                                            ? healthProfile.getGender()
                                            : null
                            );
                        })
                        .toList();

        return new FamilyInfoResponse(
                family.getId(),
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

            long memberCount =
                    userRepository.countByFamilyId(family.getId());

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