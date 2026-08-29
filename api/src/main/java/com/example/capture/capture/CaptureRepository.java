package com.example.capture.capture;

import com.example.capture.capture.domain.Capture;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CaptureRepository extends JpaRepository<Capture, Long> {

    // 타입이 섞인 전체 조회는 한 번에 fetch join이 안 된다. v1은 상세 없이 요약만 내려준다 (stack.md §2.5)
    @Query("""
            select c from Capture c
            where (:cursor is null or c.id < :cursor)
            order by c.id desc
            """)
    List<Capture> findPage(@Param("cursor") Long cursor, Pageable pageable);
}
