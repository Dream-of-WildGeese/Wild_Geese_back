package com.ondam.medication.service;

import com.ondam.global.exception.BusinessException;
import com.ondam.global.exception.ErrorCode;
import com.ondam.medication.dto.request.MedicationCreateRequest;
import com.ondam.medication.dto.request.MedicationLogCreateRequest;
import com.ondam.medication.dto.request.MedicationUpdateRequest;
import com.ondam.medication.dto.response.MedicationCreateResponse;
import com.ondam.medication.dto.response.MedicationDueResponse;
import com.ondam.medication.dto.response.MedicationLogResponse;
import com.ondam.medication.dto.response.MedicationResponse;
import com.ondam.medication.entity.*;
import com.ondam.medication.repository.MedicationLogRepository;
import com.ondam.medication.repository.MedicationRepository;
import com.ondam.medication.repository.MedicationScheduleRepository;
import com.ondam.user.entity.User;
import com.ondam.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final UserRepository userRepository;
    private final MedicationLogRepository medicationLogRepository;

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

    @Transactional
    public void deleteMedication(
            Long userId,
            Long medicationId
    ) {

        Medication medication = medicationRepository
                .findByIdAndUserId(medicationId, userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.MEDICATION_NOT_FOUND)
                );

        medication.deactivate();
    }

    @Transactional(readOnly = true)
    public MedicationLogResponse getMedicationLogs(
            Long userId,
            LocalDate date
    ) {

        // 사용자 존재 확인
        userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        // 해당 사용자의 활성 복약 일정 전체 조회
        List<MedicationSchedule> schedules =
                medicationScheduleRepository
                        .findAllByMedicationUserIdAndMedicationIsActiveTrueAndIsEnabledTrue(
                                userId
                        );

        List<MedicationLogResponse.MedicationLogItem> items =
                schedules.stream()

                        // 요청 날짜의 요일에 먹는 약만
                        .filter(schedule ->
                                schedule.getDaysOfWeek()
                                        .contains(
                                                MedicationDay.valueOf(
                                                        date.getDayOfWeek().name()
                                                )
                                        )
                        )

                        .map(schedule -> {

                            MedicationLogStatus status =
                                    medicationLogRepository
                                            .findByScheduleIdAndRecordDate(
                                                    schedule.getId(),
                                                    date
                                            )
                                            .isPresent()
                                            ? MedicationLogStatus.TAKEN
                                            : MedicationLogStatus.NOT_RECORDED;
                            return new MedicationLogResponse.MedicationLogItem(
                                    schedule.getMedication().getId(),
                                    schedule.getId(),
                                    schedule.getMedication().getName(),
                                    schedule.getScheduledTime(),
                                    status
                            );
                        })
                        .toList();

        int takenCount = (int) items.stream()
                .filter(item ->
                        item.status() == MedicationLogStatus.TAKEN
                )
                .count();

        return new MedicationLogResponse(
                date.toString(),
                items.size(),
                takenCount,
                items
        );
    }

    @Transactional(readOnly = true)
    public List<MedicationDueResponse> getDueMedications(Long userId) {

        // 1. 사용자 확인
        userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        // 2. 현재 날짜와 시간
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        MedicationDay todayDay =
                MedicationDay.valueOf(today.getDayOfWeek().name());

        // 3. 사용자의 활성화된 복약 일정 조회
        List<MedicationSchedule> schedules =
                medicationScheduleRepository
                        .findAllByMedicationUserIdAndMedicationIsActiveTrueAndIsEnabledTrue(
                                userId
                        );

        return schedules.stream()

                // 오늘 먹는 약인지
                .filter(schedule ->
                        schedule.getDaysOfWeek().contains(todayDay)
                )

                // 현재 시간에 먹는 약인지
                .filter(schedule ->
                        schedule.getScheduledTime().getHour()
                                == now.getHour()
                )

                .map(schedule ->
                        new MedicationDueResponse(
                                schedule.getMedication().getId(),
                                schedule.getId(),
                                schedule.getMedication().getName(),
                                schedule.getScheduledTime()
                        )
                )

                .toList();
    }

    @Transactional
    public void createMedicationLog(
            Long userId,
            MedicationLogCreateRequest request
    ) {

        // 1. 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        // 2. 복약 일정 조회
        MedicationSchedule schedule =
                medicationScheduleRepository.findById(request.scheduleId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND
                                )
                        );

        // 3. 이 일정이 현재 사용자의 약인지 확인
        if (!schedule.getMedication()
                .getUser()
                .getId()
                .equals(user.getId())) {

            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 4. 같은 날짜에 이미 기록이 있는지 확인
        boolean alreadyExists =
                medicationLogRepository
                        .findByScheduleIdAndRecordDate(
                                schedule.getId(),
                                request.recordDate()
                        )
                        .isPresent();

        if (alreadyExists) {
            throw new BusinessException(
                    ErrorCode.MEDICATION_LOG_ALREADY_EXISTS
            );
        }

        // 5. 소급 기록 여부
        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Seoul")
        );

        boolean isRetroactive =
                request.recordDate().isBefore(today);

        // 6. 로그 생성
        MedicationLog log = new MedicationLog(
                schedule,
                request.recordDate(),
                request.status(),
                LocalDateTime.now(ZoneId.of("Asia/Seoul")),
                isRetroactive
        );

        medicationLogRepository.save(log);
    }

}