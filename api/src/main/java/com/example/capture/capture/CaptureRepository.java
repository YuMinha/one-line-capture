package com.example.capture.capture;

import com.example.capture.capture.domain.Capture;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaptureRepository extends JpaRepository<Capture, Long> {
}
