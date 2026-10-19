package com.pulsepass.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.UserRoleId;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
}
