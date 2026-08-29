package com.example.capture.capture;

import com.example.capture.capture.domain.Todo;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // 상세 쪽에서 조회하며 capture를 fetch한다. Capture에는 상세로 가는 연관이 없다.
    // 이게 없으면 목록 20건에 쿼리가 21번 나간다 (stack.md §2.5)
    @Query("""
            select d from Todo d
            join fetch d.capture c
            where (:cursor is null or c.id < :cursor)
              and (:done is null or d.done = :done)
            order by c.id desc
            """)
    List<Todo> findPage(@Param("cursor") Long cursor, @Param("done") Boolean done, Pageable pageable);
}
