package com.example.capture.capture;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.capture.capture.domain.CaptureType;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

// 목록 조회가 건수와 무관하게 고정된 쿼리 수를 쓰는지 확인한다.
// 이게 깨지면 데이터가 늘수록 조용히 느려진다 (stack.md §2.5)
@SpringBootTest
@Transactional
class ListQueryCountTest {

    @Autowired
    private CaptureService captureService;

    @Autowired
    private EntityManager entityManager;

    private Statistics statistics() {
        return entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
    }

    private void save(String text) {
        captureService.create(text);
    }

    private long countQueriesForList(CaptureType type) {
        entityManager.flush();
        entityManager.clear();
        Statistics stats = statistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        captureService.list(type, null, 50, null);

        return stats.getPrepareStatementCount();
    }

    @Test
    @DisplayName("전체 목록은 항목이 몇 건이든 쿼리 4번을 넘지 않는다")
    void 전체_목록_쿼리수() {
        for (int i = 0; i < 5; i++) {
            save("항목" + i + " " + (i + 1) + "000원");
            save("할일" + i);
            save("https://a" + i + ".com 메모");
        }

        long queries = countQueriesForList(null);

        // capture 1번 + expense/todo/link 각 1번
        assertThat(queries).isLessThanOrEqualTo(4);
    }

    @Test
    @DisplayName("타입 필터 목록은 fetch join으로 쿼리 1번이다")
    void 타입_필터_쿼리수() {
        for (int i = 0; i < 10; i++) {
            save("항목" + i + " " + (i + 1) + "000원");
        }

        assertThat(countQueriesForList(CaptureType.EXPENSE)).isEqualTo(1);
    }

    @Test
    @DisplayName("건수를 늘려도 쿼리 수는 그대로다")
    void 건수가_늘어도_고정() {
        for (int i = 0; i < 3; i++) {
            save("항목" + i + " 1000원");
            save("할일" + i);
        }
        long few = countQueriesForList(null);

        for (int i = 10; i < 25; i++) {
            save("항목" + i + " 1000원");
            save("할일" + i);
        }
        long many = countQueriesForList(null);

        assertThat(many).isEqualTo(few);
    }
}
