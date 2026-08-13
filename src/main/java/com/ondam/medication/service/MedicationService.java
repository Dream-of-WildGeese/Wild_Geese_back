package com.ondam.medication.service;

import com.ondam.global.exception.BusinessException;
import com.ondam.global.exception.ErrorCode;
import com.ondam.medication.dto.request.MedicationCreateRequest;
import com.ondam.medication.dto.request.MedicationUpdateRequest;
import com.ondam.medication.dto.response.MedicationCreateResponse;
import com.ondam.medication.dto.response.MedicationResponse;
import com.ondam.medication.entity.Medication;
import com.ondam.medication.entity.MedicationSchedule;
import com.ondam.medication.repository.MedicationRepository;
import com.ondam.medication.repository.MedicationScheduleRepository;
import com.ondam.user.entity.User;
import com.ondam.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final UserRepository userRepository;

    @Transactional
    public MedicationCreateResponse createMedication(
            Long userId,
            MedicationCreateRequest request
    ) {

        // 1. 사용자 찾기
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        // 2. 약 생성
        Medication medication = new Medication(user, request.name());

        Medication savedMedication =
                medicationRepository.save(medication);

        // 3. 선택한 시간마다 복약 일정 생성
        for (var scheduledTime : request.scheduledTimes()) {

            MedicationSchedule schedule =
                    new MedicationSchedule(
                            savedMedication,
                            scheduledTime,
                            request.daysOfWeek()
                    );

            medicationScheduleRepository.save(schedule);
        }

        // 4. 결과 반환
        return new MedicationCreateResponse(
                savedMedication.getId(),
                savedMedication.getName()
        );
    }

    @Transactional(readOnly = true)
    public List<MedicationResponse> getMedications(Long userId) {

        // 1. 사용자 존재 확인
        userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        // 2. 사용자의 활성 약 조회
        List<Medication> medications =
                medicationRepository.findAllByUserIdAndIsActiveTrue(userId);

        // 3. Medication → Response 변환
        return medications.stream()
                .map(medication -> {

                    List<MedicationResponse.ScheduleResponse> schedules =
                            medicationScheduleRepository
                                    .findAllByMedicationId(medication.getId())
                                    .stream()
                                    .map(schedule ->
                                            new MedicationResponse.ScheduleResponse(
                                                    schedule.getId(),
                                                    schedule.getScheduledTime(),
                                                    schedule.getDaysOfWeek(),
                                                    schedule.isEnabled()
                                            )
                                    )
                                    .toList();

                    return new MedicationResponse(
                            medication.getId(),
                            medication.getName(),
                            schedules
                    );
                })
                .toList();
    }
    @Transactional
    public void updateMedication(
            Long userId,
            Long medicationId,
            MedicationUpdateRequest request
    ) {

        Medication medication = medicationRepository
                .findByIdAndUserId(medicationId, userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.MEDICATION_NOT_FOUND)
                );

        // 약 이름 수정
        medication.update(
                request.name(),
                medication.isActive()
        );

        // 기존 일정 제거
        medicationScheduleRepository
                .deleteAllByMedicationId(medicationId);

        // 새로운 일정 생성
        for (LocalTime scheduledTime : request.scheduledTimes()) {

            MedicationSchedule schedule =
                    new MedicationSchedule(
                            medication,
                            scheduledTime,
                            request.daysOfWeek()
                    );

            medicationScheduleRepository.save(schedule);
        }
    }

}