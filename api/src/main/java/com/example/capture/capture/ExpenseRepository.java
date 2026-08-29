package com.example.capture.capture;

import com.example.capture.capture.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
}
