package com.pulsepass.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsepass.domain.UserCredential;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
}
