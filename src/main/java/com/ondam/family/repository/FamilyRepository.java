package com.ondam.family.repository;

import com.ondam.family.entity.Family;
import com.ondam.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FamilyRepository extends JpaRepository<Family, Long> {

}