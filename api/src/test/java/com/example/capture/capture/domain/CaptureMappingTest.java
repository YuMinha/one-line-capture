package com.example.capture.capture.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

// 인메모리 DB로 바꾸지 않는다. MySQL 방언과 Flyway 스키마를 그대로 검증하는 게 목적이다
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CaptureMappingTest {

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("Capture와 Expense를 저장한 뒤 조회하면 같은 id를 공유한다")
    void capture와_expense_저장_후_조회() {
        Capture capture = new Capture("점심 9000원", CaptureType.EXPENSE, CaptureSource.AUTO);
        em.persist(capture);
        em.persist(new Expense(capture, new BigDecimal("9000.00"), "점심", LocalDate.of(2026, 8, 28)));
        em.flush();
        em.clear();

        Expense found = em.find(Expense.class, capture.getId());

        assertThat(found).isNotNull();
        // @MapsId의 핵심: 상세 행의 PK가 capture의 id와 같다
        assertThat(found.getCaptureId()).isEqualTo(capture.getId());
        assertThat(found.getAmount()).isEqualByComparingTo("9000");
        assertThat(found.getMerchant()).isEqualTo("점심");
        assertThat(found.getSpentAt()).isEqualTo(LocalDate.of(2026, 8, 28));
        assertThat(found.getCapture().getRawText()).isEqualTo("점심 9000원");
        assertThat(found.getCapture().getType()).isEqualTo(CaptureType.EXPENSE);
    }

    @Test
    @DisplayName("created_at은 DB가 채운다")
    void created_at은_DB가_채운다() {
        Capture capture = new Capture("우산 챙기기", CaptureType.TODO, CaptureSource.AUTO);
        em.persist(capture);
        em.flush();
        em.clear();

        Capture found = em.find(Capture.class, capture.getId());

        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("마감 없는 Todo와 안 읽은 Link도 저장된다")
    void todo와_link_저장() {
        Capture todoCapture = new Capture("우산 챙기기", CaptureType.TODO, CaptureSource.AUTO);
        em.persist(todoCapture);
        em.persist(new Todo(todoCapture, "우산 챙기기", null));

        Capture linkCapture = new Capture("https://example.com 스프링 정리글", CaptureType.LINK, CaptureSource.AUTO);
        em.persist(linkCapture);
        em.persist(new Link(linkCapture, "https://example.com", "스프링 정리글"));
        em.flush();
        em.clear();

        Todo todo = em.find(Todo.class, todoCapture.getId());
        Link link = em.find(Link.class, linkCapture.getId());

        assertThat(todo.getDueAt()).isNull();
        assertThat(todo.isDone()).isFalse();
        assertThat(link.getReadAt()).isNull();
        assertThat(link.getNote()).isEqualTo("스프링 정리글");
    }
}
