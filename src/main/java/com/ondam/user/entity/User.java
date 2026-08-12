package com.ondam.user.entity;

import com.ondam.family.entity.Family;
import com.ondam.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    private Family family;

    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.onboardingCompleted = false;
    }

    public void joinFamily(Family family) {
        this.family = family;
    }

    public void leaveFamily(){
        this.family=null;
    }

}