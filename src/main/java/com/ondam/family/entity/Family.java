package com.ondam.family.entity;

import com.ondam.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "families")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Family extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(
            name = "invite_code",
            nullable = false,
            unique = true,
            length = 10
    )
    private String inviteCode;

    @Column(
            name = "created_by",
            nullable = false
    )
    private Long createdBy;

    public Family(
            String name,
            String inviteCode,
            Long createdBy
    ) {
        this.name = name;
        this.inviteCode = inviteCode;
        this.createdBy = createdBy;
    }
}