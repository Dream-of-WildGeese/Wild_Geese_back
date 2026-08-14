package com.ondam.meal.entity;

import com.ondam.global.common.BaseTimeEntity;
import com.ondam.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "meal_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_meal_log_user_date_type",
                        columnNames = {"user_id", "record_date", "meal_type"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @Column(nullable = false)
    private boolean eaten;

    public MealLog(
            User user,
            LocalDate recordDate,
            MealType mealType,
            boolean eaten
    ) {
        this.user = user;
        this.recordDate = recordDate;
        this.mealType = mealType;
        this.eaten = eaten;
    }

    public void update(boolean eaten) {
        this.eaten = eaten;
    }
}