package com.ondam.user.service;

import com.ondam.global.exception.BusinessException;
import com.ondam.global.exception.ErrorCode;
import com.ondam.user.dto.request.*;
import com.ondam.user.dto.response.*;
import com.ondam.user.entity.HealthProfile;
import com.ondam.user.entity.NotificationSetting;
import com.ondam.user.entity.PushSubscription;
import com.ondam.user.entity.User;
import com.ondam.user.repository.HealthProfileRepository;
import com.ondam.user.repository.NotificationSettingRepository;
import com.ondam.user.repository.PushSubscriptionRepository;
import com.ondam.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.management.Notification;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;

    @Transactional
    public UserCreateResponse createUser(UserCreateRequest request) {

        String inviteCode = createInviteCode();

        User user = new User(
                request.email(),
                request.password(),
                request.name(),
                request.role(),
                inviteCode
        );

        User savedUser = userRepository.save(user);

        return new UserCreateResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getRole(),
                savedUser.getInviteCode(),
                savedUser.isOnboardingCompleted()
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

        } while (userRepository.existsByInviteCode(code));

        return code;
    }


    @Transactional
    public HealthProfileResponse updateHealthProfile(
            Long userId,
            HealthProfileUpdateRequest request
    ) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        HealthProfile profile = healthProfileRepository.findByUserId(userId).orElseGet(() -> new HealthProfile(user));

        profile.update(
                request.birthDate(),
                request.gender(),
                request.diseases(),
                request.wellnessInterests()
        );

        HealthProfile savedProfile = healthProfileRepository.save(profile);

        return new HealthProfileResponse(
                savedProfile.getId(),
                savedProfile.getBirthDate(),
                savedProfile.getGender(),
                savedProfile.getDiseases(),
                savedProfile.getWellnessInterests()
        );
    }
    @Transactional
    public NotificationSettingResponse updateNotificationSettings(
            Long userId,
            NotificationSettingUpdateRequest request
    ){
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(request.morningEnabled() && request.morningTime() == null){
            throw new IllegalArgumentException("아침 알림을 켜려면 시간을 설정해야 합니다.");
        }
        if(request.eveningEnabled() && request.eveningTime() == null){
            throw new IllegalArgumentException("저녁 알림을 켜려면 시간을 설정해야 합니다");
        }
        if (request.reportEnabled() && request.reportDayOfWeek() == null) {
            throw new IllegalArgumentException(
                    "리포트 알림을 켜려면 요일을 설정해야 합니다."
            );}
        NotificationSetting setting = notificationSettingRepository.findByUserId(userId).orElseGet(()->new NotificationSetting(user));

        setting.update(
                request.morningTime(),
                request.morningEnabled(),
                request.eveningTime(),
                request.eveningEnabled(),
                request.reportEnabled(),
                request.reportDayOfWeek(),
                request.medicationEnabled(),
                request.familyReactionEnabled()
        );

        NotificationSetting saved = notificationSettingRepository.save(setting);

        return new NotificationSettingResponse(
                saved.getId(),
                saved.getMorningTime(),
                saved.isMorningEnabled(),
                saved.getEveningTime(),
                saved.isEveningEnabled(),
                saved.isReportEnabled(),
                saved.getReportDayOfWeek(),
                saved.isMedicationEnabled(),
                saved.isFamilyReactionEnabled()
        );
    }

    @Transactional(readOnly = true)
    public NotificationSettingResponse getNotificationSetting(Long userId) {

        NotificationSetting setting =
                notificationSettingRepository.findByUserId(userId).orElseThrow(() ->
                                new IllegalArgumentException("알림 설정을 찾을 수 없습니다."));

        return new NotificationSettingResponse(
                setting.getId(),
                setting.getMorningTime(),
                setting.isMorningEnabled(),
                setting.getEveningTime(),
                setting.isEveningEnabled(),
                setting.isReportEnabled(),
                setting.getReportDayOfWeek(),
                setting.isMedicationEnabled(),
                setting.isFamilyReactionEnabled()
        );
    }
    @Transactional
    public void completeOnboarding(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        user.completeOnboarding();
    }

    @Transactional
    public void createPushSubscription(
            Long userId,
            PushSubscriptionCreateRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        // 이미 같은 endpoint가 존재하면
        PushSubscription existingSubscription =
                pushSubscriptionRepository
                        .findByEndpoint(request.endpoint())
                        .orElse(null);

        if (existingSubscription != null) {

            // 현재 사용자와 최신 key 정보로 갱신
            existingSubscription.update(
                    user,
                    request.p256dh(),
                    request.auth()
            );

            return;
        }

        // 처음 보는 endpoint라면 새로 저장
        PushSubscription subscription =
                new PushSubscription(
                        user,
                        request.endpoint(),
                        request.p256dh(),
                        request.auth()
                );

        pushSubscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public InviteCodeResponse getInviteCode(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        return new InviteCodeResponse(
                user.getInviteCode()
        );
    }

    @Transactional(readOnly = true)
    public UserNameResponse getMyInfo(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        return new UserNameResponse(
                user.getId(),
                user.getName()
        );
    }

    @Transactional(readOnly = true)
    public HealthProfileResponse getHealthProfile(Long userId) {

        // 사용자 존재 여부 확인
        userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        // 해당 사용자의 건강프로필 조회
        HealthProfile healthProfile =
                healthProfileRepository.findByUserId(userId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.HEALTH_PROFILE_NOT_FOUND
                                )
                        );

        return new HealthProfileResponse(
                healthProfile.getId(),
                healthProfile.getBirthDate(),
                healthProfile.getGender(),
                healthProfile.getDiseases(),
                healthProfile.getWellnessInterests()
        );
    }

    @Transactional
    public void deletePushSubscription(
            Long userId,
            PushSubscriptionDeleteRequest request
    ) {

        PushSubscription subscription =
                pushSubscriptionRepository
                        .findByEndpoint(request.endpoint())
                        .orElse(null);

        // 이미 없으면 로그아웃은 그냥 성공 처리
        if (subscription == null) {
            return;
        }

        // 다른 사용자의 구독을 삭제하지 못하도록 확인
        if (!subscription.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        pushSubscriptionRepository.delete(subscription);
    }

}