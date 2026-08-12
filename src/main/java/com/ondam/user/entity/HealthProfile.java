package com.ondam.user.entity;

import com.ondam.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "health_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "wellness_interests", columnDefinition = "json")
    private List<WellnessInterest> wellnessInterests;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> diseases;

    public HealthProfile(User user) {
        this.user = user;
    }

    public void update(
            String name,
            LocalDate birthDate,
            UserRole role,
            Gender gender,
            List<String> diseases,
            List<WellnessInterest> wellnessInterests
    ) {
        this.name = name;
        this.birthDate = birthDate;
        this.role = role;
        this.gender = gender;
        this.diseases = diseases;
        this.wellnessInterests = wellnessInterests;
    }
}