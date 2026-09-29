package com.example.capture.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByTokenHash(String tokenHash);

    Optional<DeviceToken> findByUserIdAndFromEnvTrue(Long userId);
}
