package com.ondam.user.entity;

import com.ondam.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "notification_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name="morning_time")
    private LocalTime morningTime;

    @Column(name="morning_enabled", nullable = false)
    private boolean morningEnabled=true;

    @Column(name="evening_time")
    private LocalTime eveningTime;

    @Column(name="evening_enabled",nullable = false)
    private boolean eveningEnabled=true;

    @Enumerated(EnumType.STRING)
    @Column(name="report_day_of_week")
    private DayOfWeek reportDayOfWeek;

    @Column(name="report_enabled")
    private boolean reportEnabled=true;

    @Column(name = "medication_enabled", nullable = false)
    private boolean medicationEnabled = true;

    @Column(name = "family_reaction_enabled", nullable = false)
    private boolean familyReactionEnabled = true;

    public NotificationSetting(User user)
    {
        this.user=user;
        this.morningEnabled =true;
        this.eveningEnabled =true;
        this.reportEnabled=true;
        this.medicationEnabled =true;

    }

    public void update(
            LocalTime morningTime,
            boolean morningEnabled,
            LocalTime eveningTime,
            boolean eveningEnabled,
            boolean reportEnabled,
            DayOfWeek reportDayOfWeek,
            boolean medicationEnabled,
            boolean familyReactionEnabled
    ){
        this.morningTime = morningTime;
        this.morningEnabled = morningEnabled;
        this.eveningTime = eveningTime;
        this.eveningEnabled = eveningEnabled;
        this.reportEnabled = reportEnabled;
        this.reportDayOfWeek = reportDayOfWeek;
        this.medicationEnabled = medicationEnabled;
        this.familyReactionEnabled = familyReactionEnabled;
    }


}