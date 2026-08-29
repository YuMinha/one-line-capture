package com.example.capture.capture;

import com.example.capture.capture.domain.Link;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkRepository extends JpaRepository<Link, Long> {
}
