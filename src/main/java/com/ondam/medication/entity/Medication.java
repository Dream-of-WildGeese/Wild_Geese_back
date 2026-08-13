package com.ondam.medication.entity;

import com.ondam.global.common.BaseTimeEntity;
import com.ondam.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "medications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Medication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public Medication(
            User user,
            String name
    ) {
        this.user = user;
        this.name = name;
        this.isActive = true;
    }

    public void update(
            String name,
            boolean isActive
    ) {
        this.name = name;
        this.isActive = isActive;
    }

    public void deactivate() {
        this.isActive = false;
    }
}