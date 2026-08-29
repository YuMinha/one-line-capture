package com.example.capture.summary;

import java.math.BigDecimal;
import java.time.LocalDate;

// 네이티브 쿼리 결과를 받는 인터페이스 프로젝션. 컬럼 별칭과 getter 이름이 맞아야 한다
public interface DailyTotal {

    LocalDate getSpentDate();

    BigDecimal getTotalAmount();

    long getEntryCount();
}
