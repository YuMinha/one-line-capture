package com.example.capture.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkCodeRepository extends JpaRepository<LinkCode, String> {

    void deleteByUserId(Long userId);
}
