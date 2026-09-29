package com.example.capture.capture;

import com.example.capture.capture.domain.Capture;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CaptureRepository extends JpaRepository<Capture, Long> {

    // 타입이 섞인 전체 조회는 한 번에 fetch join이 안 된다. 상세는 서비스가 타입별로 모아서 붙인다 (stack.md §2.5)
    @Query("""
            select c from Capture c
            where c.userId = :userId
              and (:cursor is null or c.id < :cursor)
            order by c.id desc
            """)
    List<Capture> findPage(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    // 찾고 나서 소유자를 비교하지 않는다. 조건에 넣으면 남의 것과 없는 것이 똑같이 404가 된다 (stack.md §2.2)
    Optional<Capture> findByIdAndUserId(Long id, Long userId);
}
