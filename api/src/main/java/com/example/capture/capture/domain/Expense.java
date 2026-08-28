package com.example.capture.capture.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;

@Entity
@Table(name = "expense")
@Getter
public class Expense {

    @Id
    private Long captureId;

    // @MapsId가 없으면 JPA가 별도 PK를 만들려 해서 스키마와 어긋난다 (stack.md §2.3)
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "capture_id")
    private Capture capture;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 100)
    private String merchant;

    @Column(name = "spent_at", nullable = false)
    private LocalDate spentAt;

    protected Expense() {}

    public Expense(Capture capture, BigDecimal amount, String merchant, LocalDate spentAt) {
        this.capture = capture;
        this.amount = amount;
        this.merchant = merchant;
        this.spentAt = spentAt;
    }
}
