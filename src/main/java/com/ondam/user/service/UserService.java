package com.ondam.user.service;

import com.ondam.global.exception.BusinessException;
import com.ondam.global.exception.ErrorCode;
import com.ondam.user.dto.request.HealthProfileUpdateRequest;
import com.ondam.user.dto.request.NotificationSettingUpdateRequest;
import com.ondam.user.dto.request.PushSubscriptionCreateRequest;
import com.ondam.user.dto.request.UserCreateRequest;
import com.ondam.user.dto.response.HealthProfileResponse;
import com.ondam.user.dto.response.InviteCodeResponse;
import com.ondam.user.dto.response.NotificationSettingResponse;
import com.ondam.user.dto.response.UserCreateResponse;
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

}