package com.ondam.user.service;

import com.ondam.global.exception.BusinessException;
import com.ondam.global.exception.ErrorCode;
import com.ondam.user.dto.request.HealthProfileUpdateRequest;
import com.ondam.user.dto.request.UserCreateRequest;
import com.ondam.user.dto.response.HealthProfileResponse;
import com.ondam.user.dto.response.UserCreateResponse;
import com.ondam.user.entity.HealthProfile;
import com.ondam.user.entity.User;
import com.ondam.user.repository.HealthProfileRepository;
import com.ondam.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;

    @Transactional
    public UserCreateResponse createUser(UserCreateRequest request) {

        User user = new User(
                request.email(),
                request.password()
        );

        User savedUser = userRepository.save(user);

        return new UserCreateResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.isOnboardingCompleted()
        );
    }

    @Transactional
    public HealthProfileResponse updateHealthProfile(
            Long userId,
            HealthProfileUpdateRequest request
    ) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        HealthProfile profile = healthProfileRepository.findByUserId(userId).orElseGet(() -> new HealthProfile(user));

        profile.update(
                request.name(),
                request.birthDate(),
                request.role(),
                request.gender(),
                request.diseases(),
                request.wellnessInterests()
        );

        HealthProfile savedProfile = healthProfileRepository.save(profile);

        return new HealthProfileResponse(
                savedProfile.getId(),
                savedProfile.getName(),
                savedProfile.getBirthDate(),
                savedProfile.getRole(),
                savedProfile.getGender(),
                savedProfile.getDiseases(),
                savedProfile.getWellnessInterests()
        );
    }
}